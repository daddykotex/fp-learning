Day 2
====
In day 1, I tried to explain what were the reasons driving this exploration into functional programming. Basically, apart from pure curiosity, I think functional programming will help me write more maintainable, testable and stable code.

# FP Scala for mortals

I kept reading [fommil's book](https://leanpub.com/fp-scala-mortals/). The introduction gives an interesting example that is not complex to grasp. I'll break it down even further to make sure I understand the premise here: you want to build a data structure that describes the program behavior without actually _doing_ anything.

Before we proceed, here are some imports to make our lives easier:

```tut
import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global
```

Given the data structure, you can customize the behavior of the program by "executing" with different interpreters.

The example brings a simple terminal to the table. Let's build a very similar interface (Note: do not ever use this code, it's terrible, it's just to show where the problem arise):

```tut
trait Terminal {
    def read: String
    def write(line: String): Unit
}
```

This terminal can read from the console, and write to it. Here is a very naive default implementation.

```tut
case object ConsoleTerminal extends Terminal {
    override def read: String = ???
    override def write(line: String): Unit = ???
}
```

This implementation is rock solid, and can be used as-is. It can also be tested relatively easily since we defined an interface. We could create a mock really quickly with to use in our test.
 
Now skip a few months later, your Big Data startup is going wild and the `Terminal` default implementation does not scale. You need to make asynchronous, what do you do?

```tut

import scala.concurrent.Future

trait AsyncTerminal {
    def read: Future[String]
    def write(line: String): Future[Unit]
}

case object AsyncConsoleTerminal extends AsyncTerminal {
    override def read: Future[String] = ???
    override def write(line: String): Future[Unit] = ???
}

class SyncConsoleTerminal(async: AsyncTerminal) extends Terminal {
    override def read: String = ???
    override def write(line: String): Unit = ???
}

```

So now you've got two different interfaces, two different problems. Could we have a single way to define a `Terminal` program, be it async or not?

In the synchronous version, we work directly with the output type where as in the asynchronous version, it's wrapped into a `Future`. The `Future` type gives us contextual information. It tells us the code is executed asynchronously. Is there a way to express the fact that the code is synchronous? This is what @fommil shows in his introduction:

```tut
trait Terminal[C[_]] {
    def read: C[String]
    def write(line: String): C[Unit]
}
```

So what's `C` here? `C` is a type constructor. In `Scala`, `Java` and many other languages, you can use generics to write pieces of logic that works for different types. This is really powerful and allows you, among other things, to avoid copy/pasting (or code generation for that matter).

But in `Scala` (and other languages as well, but not Java for example), you can use Higher Kinded Types. This allows you write logic that works for type that themselves contain other types. In the example above, `C[_]` is a type constructor. So we can define an implementation of `Terminal` for any type takes another type: `List`, `Set`, `Option`, etc.

Now we can redefine our async interface replacing `C` with `Future`.

```tut
case object AsyncConsoleTerminal extends Terminal[Future] {
    override def read: Future[String] = ???
    override def write(line: String): Future[Unit] = ???
}
```

Now, how do we implement the synchronous version? Well, we can use a `Scala` trick and type aliases to solve our issue:

```tut
type Now[T] = T
case object ConsoleTerminal extends Terminal[Now] {
    override def read: String = ???
    override def write(line: String): Unit = ???
}
```

Now we have a common interface for both our synchronous and asynchronous terminal. If we use any of the implementation of `Terminal[C[_]]`, it's relatively easy to define a hello world program:

```tut
def helloSync(term: Terminal[Now]): String = {
    val input: String = term.read
    term.write(input)
    input
}
def helloAsync(term: Terminal[Future]): Future[String] = {
    val input: Future[String] = term.read
    input flatMap { in => term.write(in) map {_ => in} }
}
```

Easy enough, easy to understand and easy to use, but that code is not so DRY. Is there a way we could build _one_ program and then decide if we want it to run asynchronously or synchronously? There is way but, before we can do it, we need a way to work with our `C[_]` here. Let's introduce a `trait` that allows us to work with `C[_]`:

```tut
trait Execution[C[_]] {
    def doAndThen[A, B](a: C[A])(f: A => C[B]): C[B]
    def create[A](a: => A): C[A]
}
```

From this point on, if we have an instance of `Execution`, we can chain our terminal operations to build a program:

```tut
def hello[C[_]](term: Terminal[C], ex: Execution[C]) = {
    ex.doAndThen(term.read) { in =>
        ex.doAndThen(term.write(in)) { _ =>
            ex.create(in)
        }
    }
}
```
In order to use our function, we need an implementation of `Execution` for both `Now` and `Future`:

```tut
val nowExec: Execution[Now] = new Execution[Now] {
    def doAndThen[A, B](a: Now[A])(f: A => Now[B]): Now[B] = f(a)
    def create[A](a: => A): Now[A] = a
}
val futureExec: Execution[Future] = new Execution[Future] {
    def doAndThen[A, B](a: Future[A])(f: A => Future[B]): Future[B] = a.flatMap(f)
    def create[A](a: => A): Future[A] = Future { a }
}
```

We can now build an asynchronous and a synchronous version of our hello program:

```tut
def syncHello: String = hello(ConsoleTerminal, nowExec)
def asyncHello: Future[String] = hello(AsyncConsoleTerminal, futureExec)
```