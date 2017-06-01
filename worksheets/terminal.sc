import scala.concurrent.Future

trait Terminal[C[_]] {
    def read: C[String]
    def write(line: String): C[Unit]
}

case object AsyncConsoleTerminal extends Terminal[Future] {
    import scala.io.StdIn
    
    override def read: Future[String] = {
        Future.successful {
            StdIn.readLine
        }
    }
    override def write(line: String): Future[Unit] = {
        Future.successful {
            println(line)
        }
    }
}

type Now[T] = T
case object ConsoleTerminal extends Terminal[Now] {
    import scala.io.StdIn
    override def read: String = {
        StdIn.readLine
    }
    override def write(line: String): Unit = {
        println(line)
    }
}

def echo[C[_]](term: Terminal[C]): Unit = {
    val input: C[String] = term.read
    val output: C[Unit] = term.write(s"Hello $input")
    ()
}

echo(AsyncConsoleTerminal)