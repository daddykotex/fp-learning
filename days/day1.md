So here we are, day 1. I've decided to document my functional programming learning after seeing some
interesting comments by [fommil](https://twitter.com/fommil) regarding FP in `Scala`.

Fommil decided to write a book to introduce functional programing in `Scala` to beginners. The book, 
[Scala for Mortals](https://leanpub.com/fp-scala-mortals/), is actively being written at the time of this post. 
It is targeted at an audience of people like me, so I decided to share my impressions reading his book as
a way to provide relevant feedback.

-------

Let's kick things off with some details as too why I think it would be relevant for me learn functional
programming. As I said in the introductory post, my work @ mnubo got me into `Scala`. Since then, I enjoyed
most of the language features and its syntax. 

I learnt `Scala` by converting existing `Java` codebases to `Scala` and I got addicted to some things 
that made my day to day much more enjoyable. Here is a non-exhaustive list of those things:

* `case class`es
* immutable (by default) `collections`
* first class support for functions
* rich language API: `Option`, `Future`, `Either|Try`, `List|Stream|Seq|Traversable`, etc.

At the beginning, my code was written in a very imperative way. But as time went by, I began
to use more and more of the above things and my code became more succinct and clean. I noticed that
the patterns I could take out from this were mostly due to the functional nature that one can find
in `Scala`.

After a while, I tried to extract some common things and ended up using libraries like `cats` and `scalaz`.
But when I tried to look at the source code to figure out these things were working, I was breathless.
I could understand the code, but I was having a hard time figuring out how all of these components were
working together. Things like the famous `Monad`, the `Applicative` or any of 
[theses](https://github.com/tpolecat/cats-infographic/blob/master/cats.pdf).

The ultimate goal would be to understand some of them.
