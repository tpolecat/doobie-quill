package doobie.contrib.quill.shallow

import doobie.imports._

import io.getquill._
import io.getquill.naming.SnakeCase
import io.getquill.source.jdbc.JdbcSource 
import io.getquill.source.sql.idiom.PostgresDialect

import scalaz.concurrent.Task

// A source that acts like a Quill JdbcSource but interprets to doobie programs. In principle this
// object should be definable in nested scope within `example` below but compilation fails non-
// deterministically so we're putting it up here to be safe.
object ds extends DoobieSource[PostgresDialect, SnakeCase]

// Example doobie program that uses Quill to construct queries. Note that this relies on the config
// file at /src/main/resources/application.conf
object example extends App {

  // A data type that happens to line up with the COUNTRY table in the test db. Try changing the
  // name of a field, and you will see that the select below fails to compile.
  case class Country(code: String, name: String, gnp: Double)

  // Quill query with server-side filtering
  val select = quote { 
    query[Country].filter(_.code != "USA")
  }

  // Interpreted into a scalaz stream with client-side filtering
  val vec: ConnectionIO[Vector[Country]] = 
    ds.run(select).process.filter(_.name.startsWith("U")).vector

  // Runnable like any other doobie program
  val xa: Transactor[Task] = DriverManagerTransactor[Task](
    "org.postgresql.Driver", "jdbc:postgresql:world", "postgres", ""
  )
 
  vec.transact(xa).run.foreach(println)

  // Country(ARE,United Arab Emirates,37966.0)
  // Country(GBR,United Kingdom,1378330.0)
  // Country(UGA,Uganda,6313.0)
  // Country(UKR,Ukraine,42168.0)
  // Country(URY,Uruguay,20831.0)
  // Country(UZB,Uzbekistan,14194.0)
  // Country(UMI,United States Minor Outlying Islands,0.0)

}
