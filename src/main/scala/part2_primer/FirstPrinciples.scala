package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

/**
  * Created by Tomohiro on 24 juillet 2019.
  */

object FirstPrinciples extends App {

  implicit val system = ActorSystem("FirstPrinciples")
  implicit val materializer = ActorMaterializer()

  // Sources
  val source = Source(1 to 10)
  // Sink
  val sink = Sink.foreach[Int](println)

  val graph = source.to(sink)
  // graph.run()

  // flows transform elements
  val flow = Flow[Int].map(x => x + 1)
  val sourceWithFlow = source.via(flow)
  val flowWithSink = flow.to(sink)

  // sourceWithFlow.to(sink).run()
  // source.to(flowWithSink).run()
  // source.via(flow).to(sink).run()

  // nulls are NOT allowed
  //  val illegalSource = Source.single[String](null)
  //  illegalSource.to(Sink.foreach(println)).run()
  // use Options instead

  // various kinds of sources
  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source(List(1, 2, 3))
  val emptySource = Source.empty[Int]
  val infiniteSource_ = Source(Stream.from(1)) // do not confuse Akka stream with "collection" Stream
  val infiniteSource = Source(LazyList.from(1))
  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.fromFuture(Future(42))

  // sinks
  val theMostBoringSink = Sink.ignore
  val foreachSink = Sink.foreach[String](println)
  val headSink = Sink.head[Int] // retrieves head and then closes the stream
  val foldSink = Sink.fold[Int, Int](0)((a, b) => a + b)

  // flows - usually mapped to collection operators
  val mapFlow = Flow[Int].map(x => 2 * x)
  val takeFlow = Flow[Int].take(5)
  // drop, filter
  // NOT have flatMap

  // source -> flow -> flow -> ... -> sink
  val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink)
  // doubleFlowGraph.run()

  // syntactic sugar
  val mapSource = Source(1 to 10).map(x => x * 2) // Source(1 to 10).via(Flow[Int].map(x => x * 2))
  // run streams directly
  // mapSource.runForeach(println) // mapSource.to(Sink.foreach[Int](println)).run()

  // OPERATORS = components

  /**
    * Exercise: create a stream that takes the names of persons,
    * then you will keep the first 3 names with length > 5 characters
    */

  val names = List("Alice", "Bob", "Charlie", "David", "Martin", "AkkaStreams")
  val namesSource = Source(names)
  val longNameFlow = Flow[String].filter(_.length > 5)
  val limitFlow = Flow[String].take(2)
  val namesSink = Sink.foreach[String](println)

  namesSource.via(longNameFlow).via(limitFlow).to(namesSink).run()

  val namesStream = Source(names).filter(_.length > 5).take(2)
  namesStream.runForeach(println)
}
