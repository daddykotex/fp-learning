On `0.5.2`, the following fails, but I can't mark it with `:nofail`:

```scala
scala> trait Execution[C[_]] {
     |   def doAndThen[A, B](a: C[A])(f: A => C[B]): C[B]
     |   def create[A](a: => A): C[A]
     | }
defined trait Execution

scala> object Execution {
     |   implicit class Ops[A, C[_]](c: C[A]) {
     |     def flatMap[B](f: A => C[B])(implicit e: Execution[C]): C[B] =
     |       e.doAndThen(c)(f)
     |     def map[B](f: A => B)(implicit e: Execution[C]): C[B] = {
     |       e.doAndThen(c)(f andThen e.create)
     |     }
     |   } 
     | }
<console>:18: error: polymorphic expression cannot be instantiated to expected type;
 found   : [A](=> A) => C[A]
 required: B => C[B]
             e.doAndThen(c)(f andThen e.create)
                                        ^
```

While the following works:

```scala
     | trait Execution[C[_]] {
     |   def doAndThen[A, B](a: C[A])(f: A => C[B]): C[B]
     |   def create[A](a: A): C[A]
     | }
     | object Execution {
     |   implicit class Ops[A, C[_]](c: C[A]) {
     |     def flatMap[B](f: A => C[B])(implicit e: Execution[C]): C[B] =
     |       e.doAndThen(c)(f)
     |     def map[B](f: A => B)(implicit e: Execution[C]): C[B] = {
     |       e.doAndThen(c)(f andThen(e.create[B](_)))
     |     }
     |   } 
     | }
```
