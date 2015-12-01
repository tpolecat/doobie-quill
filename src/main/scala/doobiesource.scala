package doobie.contrib.quill.shallow

import doobie.imports._

import java.sql.PreparedStatement
import java.sql.ResultSet

import io.getquill.naming.NamingStrategy
import io.getquill.source.sql.SqlSource
import io.getquill.source.sql.idiom.SqlIdiom

import scalaz.syntax.apply._

/**
 * A source whose `run` method yields `QuillQuery0` (analogous to `Query0`) for queries and
 * `ConnectionIO[Int]` for updates, transforming Quill quotes into doobie programs. This source is 
 * otherwise identical in behavior to `JdbcSource` in terms of type mapping, compile-time checking,
 * and configuration.
 */
class DoobieSource[D <: SqlIdiom, N <: NamingStrategy]
 extends SqlSource[D, N, ResultSet, PreparedStatement]
    with CopyPasta {

  def execute(sql: String): ConnectionIO[Int] =
    HC.prepareStatement(sql)(HPS.executeUpdate)

  def execute(sql: String, bindList: List[PreparedStatement => PreparedStatement]) =
    HC.prepareStatement(sql)(bindList.foldRight(HPS.executeUpdate) { (f, op) =>
      FPS.raw(f) *> FPS.addBatch *> op
    })

  def query[T](sql: String, bind: PreparedStatement => PreparedStatement, extractor: ResultSet => T): QuillQuery0[T] =
    new QuillQuery0(sql, bind, extractor)

}





// For a number of reasons it's not currently possible to extend or compose with JdbcSource, so the
// trait below contains the bits that must necessarily be copy/pasted.

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

import java.sql.Connection
import java.sql.Timestamp
import java.sql.Types

import io.getquill.naming.NamingStrategy
import io.getquill.source.jdbc.JdbcSource
import io.getquill.source.jdbc.DataSource
import io.getquill.source.sql.SqlSource
import io.getquill.source.sql.idiom.SqlIdiom

import scala.util.DynamicVariable
import scala.util.Try

trait CopyPasta { self: DoobieSource[_, _] =>

  private val dataSource = DataSource(config)

  private val currentConnection = new DynamicVariable[Option[Connection]](None)

  private def withConnection[T](f: Connection => T) =
    currentConnection.value.map(f).getOrElse {
      val conn = dataSource.getConnection
      try f(conn)
      finally conn.close
    }

  def probe(sql: String) =
    withConnection { conn =>
      Try {
        conn.createStatement.execute(sql)
      }
    }

  private val dateTimeZone = TimeZone.getDefault

  private def encoder[T](f: PreparedStatement => (Int, T) => Unit): Encoder[T] =
    new Encoder[T] {
      override def apply(index: Int, value: T, row: PreparedStatement) = {
        f(row)(index + 1, value)
        row
      }
    }

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      override def apply(index: Int, value: Option[T], row: PreparedStatement) =
        value match {
          case Some(value) => d(index, value, row)
          case None =>
            import Types._
            val sqlType =
              d match {
                case `stringEncoder`     => VARCHAR
                case `bigDecimalEncoder` => NUMERIC
                case `booleanEncoder`    => BOOLEAN
                case `byteEncoder`       => TINYINT
                case `shortEncoder`      => SMALLINT
                case `intEncoder`        => INTEGER
                case `longEncoder`       => BIGINT
                case `floatEncoder`      => REAL
                case `doubleEncoder`     => DOUBLE
                case `byteArrayEncoder`  => VARBINARY
                case `dateEncoder`       => TIMESTAMP
              }
            row.setNull(index + 1, sqlType)
            row
        }
    }

  implicit val stringEncoder = encoder(_.setString)
  implicit val bigDecimalEncoder: Encoder[BigDecimal] =
    new Encoder[BigDecimal] {
      override def apply(index: Int, value: BigDecimal, row: PreparedStatement) = {
        row.setBigDecimal(index + 1, value.bigDecimal)
        row
      }
    }
  implicit val booleanEncoder = encoder(_.setBoolean)
  implicit val byteEncoder = encoder(_.setByte)
  implicit val shortEncoder = encoder(_.setShort)
  implicit val intEncoder = encoder(_.setInt)
  implicit val longEncoder = encoder(_.setLong)
  implicit val floatEncoder = encoder(_.setFloat)
  implicit val doubleEncoder = encoder(_.setDouble)
  implicit val byteArrayEncoder = encoder(_.setBytes)
  implicit val dateEncoder: Encoder[Date] =
    new Encoder[Date] {
      override def apply(index: Int, value: Date, row: PreparedStatement) = {
        row.setTimestamp(index + 1, new Timestamp(value.getTime), Calendar.getInstance(dateTimeZone))
        row
      }
    }

  private def decoder[T](f: ResultSet => Int => T): Decoder[T] =
    new Decoder[T] {
      def apply(index: Int, row: ResultSet) =
        f(row)(index + 1)
    }

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    new Decoder[Option[T]] {
      def apply(index: Int, row: ResultSet) = {
        val res = d(index, row)
        row.wasNull match {
          case true  => None
          case false => Some(res)
        }
      }
    }

  implicit val stringDecoder = decoder(_.getString)
  implicit val bigDecimalDecoder: Decoder[BigDecimal] =
    new Decoder[BigDecimal] {
      def apply(index: Int, row: ResultSet) = {
        val v = row.getBigDecimal(index + 1)
        if (v == null)
          BigDecimal(0)
        else
          v
      }
    }
  implicit val booleanDecoder = decoder(_.getBoolean)
  implicit val byteDecoder = decoder(_.getByte)
  implicit val shortDecoder = decoder(_.getShort)
  implicit val intDecoder = decoder(_.getInt)
  implicit val longDecoder = decoder(_.getLong)
  implicit val floatDecoder = decoder(_.getFloat)
  implicit val doubleDecoder = decoder(_.getDouble)
  implicit val byteArrayDecoder = decoder(_.getBytes)
  implicit val dateDecoder: Decoder[Date] =
    new Decoder[Date] {
      def apply(index: Int, row: ResultSet) = {
        val v = row.getTimestamp(index + 1, Calendar.getInstance(dateTimeZone))
        if (v == null)
          new Date(0)
        else
          new Date(v.getTime)
      }
    }

}

