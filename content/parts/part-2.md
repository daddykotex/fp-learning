Day 2
====
In day 1, I tried to explain what were the reasons driving this exploration into functional programming. Basically, apart from pure curiosity, I think functional programming will help me write more maintainable, testable and stable code.

# FP Scala for mortals

I kept reading [fommil's book](https://leanpub.com/fp-scala-mortals/). The introduction gives an interesting example that is not complex to grasp. I'll break it down even further to make sure I understand the premise here: you want to build a data structure that describes the program behavior without actually _doing_ anything.

Before we proceed, here are some imports to make our lives easier:

```scala
scala> import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext

scala> import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContext.Implicits.global

scala> import scala.language.higherKinds
import scala.language.higherKinds
```

Given the data structure, you can customize the behavior of the program by "executing" with different interpreters.

The example brings a simple terminal to the table. Let's build a very similar interface (Note: do not ever use this code, it's terrible, it's just to show where the problem arise):

```scala
scala> trait Terminal {
     |   def read: String
     |   def write(line: String): Unit
     | }
defined trait Terminal
```

This terminal can read from the console, and write to it. Here is a very naive default implementation.

```scala
scala> case object ConsoleTerminal extends Terminal {
     |   override def read: String = ???
     |   override def write(line: String): Unit = ???
     | }
defined object ConsoleTerminal
```

This implementation is rock solid, and can be used as-is. It can also be tested relatively easily since we defined an interface. We could create a mock really quickly with to use in our test.
 
Now skip a few months later, your Big Data startup is going wild and the `Terminal` default implementation does not scale. You need to make asynchronous, what do you do?

```scala
scala> import scala.concurrent.Future
import scala.concurrent.Future

scala> trait AsyncTerminal {
     |   def read: Future[String]
     |   def write(line: String): Future[Unit]
     | }
defined trait AsyncTerminal

scala> case object AsyncConsoleTerminal extends AsyncTerminal {
     |   override def read: Future[String] = ???
     |   override def write(line: String): Future[Unit] = ???
     | }
defined object AsyncConsoleTerminal

scala> class SyncConsoleTerminal(async: AsyncTerminal) extends Terminal {
     |   override def read: String = ???
     |   override def write(line: String): Unit = ???
     | }
defined class SyncConsoleTerminal
```

So now you've got two different interfaces, two different problems. Could we have a single way to define a `Terminal` program, be it async or not?

In the synchronous version, we work directly with the output type where as in the asynchronous version, it's wrapped into a `Future`. The `Future` type gives us contextual information. It tells us the code is executed asynchronously. Is there a way to express the fact that the code is synchronous? This is what @fommil shows in his introduction:

```scala
scala> trait Terminal[C[_]] {
     |   def read: C[String]
     |   def write(line: String): C[Unit]
     | }
defined trait Terminal
```

So what's `C` here? `C` is a type constructor. In `Scala`, `Java` and many other languages, you can use generics to write pieces of logic that works for different types. This is really powerful and allows you, among other things, to avoid copy/pasting (or code generation for that matter).

But in `Scala` (and other languages as well, but not Java for example), you can use Higher Kinded Types. This allows you write logic that works for type that themselves contain other types. In the example above, `C[_]` is a type constructor. So we can define an implementation of `Terminal` for any type takes another type: `List`, `Set`, `Option`, etc.

Now we can redefine our async interface replacing `C` with `Future`.

```scala
scala> case object AsyncConsoleTerminal extends Terminal[Future] {
     |   override def read: Future[String] = ???
     |   override def write(line: String): Future[Unit] = ???
     | }
defined object AsyncConsoleTerminal
```

Now, how do we implement the synchronous version? Well, we can use a `Scala` trick and type aliases to solve our issue:

```scala
scala> type Now[T] = T
defined type alias Now

scala> case object ConsoleTerminal extends Terminal[Now] {
     |   override def read: String = ???
     |   override def write(line: String): Unit = ???
     | }
defined object ConsoleTerminal
```

Now we have a common interface for both our synchronous and asynchronous terminal. If we use any of the implementation of `Terminal[C[_]]`, it's relatively easy to define a hello world program:

```scala
scala> def helloSync(term: Terminal[Now]): String = {
     |   val input: String = term.read
     |   term.write(input)
     |   input
     | }
helloSync: (term: Terminal[Now])String

scala> def helloAsync(term: Terminal[Future]): Future[String] = {
     |   val input: Future[String] = term.read
     |   input flatMap { in => term.write(in) map {_ => in} }
     | }
helloAsync: (term: Terminal[scala.concurrent.Future])scala.concurrent.Future[String]
```

Easy enough, easy to understand and easy to use, but that code is not so DRY. Is there a way we could build _one_ program and then decide if we want it to run asynchronously or synchronously? There is way but, before we can do it, we need a way to work with our `C[_]` here. Let's introduce a `trait` that allows us to work with `C[_]`:

```scala
scala> trait Execution[C[_]] {
     |   def doAndThen[A, B](a: C[A])(f: A => C[B]): C[B]
     |   def create[A](a: A): C[A]
     | }
defined trait Execution
```

From this point on, if we have an instance of `Execution`, we can chain our terminal operations to build a program:

```scala
scala> def hello[C[_]](term: Terminal[C], ex: Execution[C]): C[String] = {
     |   ex.doAndThen(term.read) { in =>
     |     ex.doAndThen(term.write(in)) { _ =>
     |       ex.create(in)
     |     }
     |   }
     | }
hello: [C[_]](term: Terminal[C], ex: Execution[C])C[String]
```
In order to use our function, we need an implementation of `Execution` for both `Now` and `Future`:

```scala
scala> val nowExec: Execution[Now] = new Execution[Now] {
     |   def doAndThen[A, B](a: Now[A])(f: A => Now[B]): Now[B] = f(a)
     |   def create[A](a: A): Now[A] = a
     | }
nowExec: Execution[Now] = $anon$1@7c82068

scala> val futureExec: Execution[Future] = new Execution[Future] {
     |   def doAndThen[A, B](a: Future[A])(f: A => Future[B]): Future[B] = a.flatMap(f)
     |   def create[A](a: A): Future[A] = Future { a }
     | }
futureExec: Execution[scala.concurrent.Future] = $anon$1@8c06a3b
```

We can now build an asynchronous and a synchronous version of our hello program:

```scala
scala> def syncHello: String = hello(ConsoleTerminal, nowExec)
syncHello: String

scala> def asyncHello: Future[String] = hello(AsyncConsoleTerminal, futureExec)
asyncHello: scala.concurrent.Future[String]
```

So what is this execution thing that we have to implement in order to use to build our hello program using the higher kinded type `C[_]`? From what I understand, it's an instance of the `Monad`. If you've used the `Option` or the `Future`, you've used a `Monad` before. With these two, you can have syntax like this:

```scala
scala> val opt1 = Option(1)
opt1: Option[Int] = Some(1)

scala> val opt2 = Option(2)
opt2: Option[Int] = Some(2)

scala> val opt3 = for {
     |   v1 <- opt1
     |   v2 <- opt2
     | } yield v1 + v2
opt3: Option[Int] = Some(3)
```

The `for` comprehension in `Scala` is a syntax sugar for when you work with a `Monad`. Can we use this syntax with our execution monad? Not just yet, we have to update it a little. The for comprehension use `flatMap` and `map`, so we want our execution thingy, to have these.

```scala
scala> trait Execution[C[_]] {
     |   def doAndThen[A, B](a: C[A])(f: A => C[B]): C[B]
     |   def create[A](a: A): C[A]
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
defined object Execution
warning: previously defined trait Execution is not a companion to object Execution.
Companions must be defined together; you may wish to use :paste mode for this.
```

With that in hand, we can go ahead and clean up our `hello` function:

```scala
scala> def hello[C[_]](term: Terminal[C])(implicit e: Execution[C]): C[String] = {
     |   import Execution._
     | 
     |   for {
     |     in <- term.read
     |     _ <- term.write(in)
     |   } yield in
     | }
hello: [C[_]](term: Terminal[C])(implicit e: Execution[C])C[String]
```

With the `implicit class`, we turned our `Execution` trait into a Monad that we were able to leverage to build our hello program. Now functionnal libraries like `scalaz` and `cats` allow you to do that very cleanly.
