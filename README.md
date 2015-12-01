## doobie-quill

Experimental [doobie](https://github.com/tpolecat/doobie) integration with [Quill](http://getquill.io).

The idea here is that you can use Quill to construct SQL queries and then lift them into `ConnectionIO` for composition and execution like any other doobie program. This provides an alternative to the `sql` interpolator.

The provided `DoobieSource` acts just like a Quill `JdbcSource` and provides compile-time query checking, but `.run` delivers doobie programs rather than immediate results. Note that **type mapping is provided by Quill** here; doobie's `Meta`/`Atom`/`Composite` scheme is unused.

This software is **very experimental**, unreleased, and unsupported. But feel free to discuss on the [doobie gitter channel](https://gitter.im/tpolecat/doobie). If there is sufficient interest this may evolve into an official doobie contrib project.

Thanks to @fwbrasil for help getting up and running.

See the source for an example.
