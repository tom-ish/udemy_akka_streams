package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

/**
  * Created by Tomohiro on 24 juillet 2019.
  */

object MaterializingStreams extends App {

  implicit val system = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))
  //  val simpleMaterializedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int]((a, b) => a + b)
  val sumFuture = source.runWith(sink)
  //  sumFuture.onComplete {
  //    case Success(value) => println(s"The sum of all elements is : $value")
  //    case Failure(exception) => println(s"The sum of the elements could not be computed: $exception")
  //  }


  // choosing materialized values
  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(x => x + 1)
  val simpleSink = Sink.foreach[Int](println)
  //  simpleSource.viaMat(simpleFlow)(Keep.right) // <=> ((sourceMat, flowMat) => flowMat)
  val graph = simpleSource.viaMat(simpleFlow)(Keep.right).toMat(simpleSink)(Keep.right)
  graph.run().onComplete {
    case Success(_) => println("Stream processing finished")
    case Failure(exception) => println(s"Stream processing failed with : $exception")
  }

  // sugar
  // Source(1 to 10).runWith(Sink.reduce[Int](_ + _)) // source.to(Sink.reduce)(Keep.right)
  // Source(1 to 10).runReduce[Int](_ + _) // same

  // backwards
  // Sink.foreach[Int](println).runWith(Source.single(42)) // source(..).to(sink...).run()

  // both ways
  // Flow[Int].map(x => x * 2).runWith(simpleSource, simpleSink)

  /**
    * Exercise
    * - return the last element out of a source (use Sink.last)
    * - compute the total word count out of a stream of sentences
    *   - map, fold, reduce
    */

  val aSource = Source(1 to 10)

  val lastFuture1 = aSource.toMat(Sink.last[Int])(Keep.right).run()
  val lastFuture2 = aSource.runWith(Sink.last)

  lastFuture1.onComplete {
    case Success(v) => println("last : " + v)
  }

  lastFuture2.onComplete {
    case Success(v) => println("last : " + v)
  }



  val sentencesSource = Source(List(
    "Akka is awesome",
    "I love streams",
    "Materialized values are killing me"
  ))


  val wordCountFlow = Flow[String].fold[Int](0)((count, content) => count + content.split(" ").length)
  val totalCount = sentencesSource.via(wordCountFlow).to(Sink.head).run()

  val wcSink = Sink.fold[Int, String](0)((count, content) => count + content.split(" ").length)



  val wordCountSink =
    Sink.fold[Int, String](0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)
  val g1 = sentencesSource.toMat(wordCountSink)(Keep.right).run()
  val g2 = sentencesSource.runWith(wordCountSink)
  val g3 = sentencesSource.runFold(0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)

  val wordCountFlows = Flow[String].fold[Int](0)((currentWords, newSentence) => currentWords + newSentence.split(" ").length)
  val g4 = sentencesSource.via(wordCountFlows).toMat(Sink.head)(Keep.right).run()
  val g5 = sentencesSource.viaMat(wordCountFlows)(Keep.both).toMat(Sink.head)(Keep.right).run()
  val g6 = sentencesSource.via(wordCountFlows).runWith(Sink.head)
  val g7 = wordCountFlows.runWith(sentencesSource, Sink.head)._2


  //  val wcFlow = Flow[String].fold[Int](0)((rslt, nextSentence) => rslt + nextSentence.split(" ").length)
  //  val totalWordCount = sentencesSource
  //     .runFold(0)((rslt, nextSentence) => rslt + nextSentence.split(" ").length)

}