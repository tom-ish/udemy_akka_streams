package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape, FanOutShape2, FlowShape, SinkShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Keep, RunnableGraph, Sink, Source}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * Created by Tomohiro on 25 juillet 2019.
  */

object GraphMaterializedValues extends App {
  implicit val system = ActorSystem("GraphMaterializedValues")
  implicit val materializer = ActorMaterializer()

  val wordSource = Source(List("Akka", "is", "awesome", "rock", "the", "jvm"))
  val printer = Sink.foreach[String](println)
  val counter = Sink.fold[Int, String](0)((counter, _) => counter + 1)

  /*
    A composite component (sink)
    - prints out all the strings which are lowercase
    - COUNTS the strings that are short (< 5 chars)
   */

  val complexWordSink = Sink.fromGraph(
    GraphDSL.create(counter) { implicit builder => counterShape =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[String](2))
      val lowercaseFilterShape = builder.add(Flow[String].filter(word => word.charAt(0).isLower))
      val shortFilterShape = builder.add(Flow[String].filter(word => word.length < 5))

      broadcast ~> lowercaseFilterShape ~> printer
      broadcast ~> shortFilterShape     ~> counterShape

      SinkShape(broadcast.in)
    }
  )

  import system.dispatcher
  val shortStringCountFuture = wordSource.toMat(complexWordSink)(Keep.right).run()
  shortStringCountFuture.onComplete {
    case Success(count) => println(s"the total number of short strings is : $count")
    case Failure(exception) => println(s"the count of short string failed : $exception")
  }


  /**
    * Exercise
    */
  def enhanceFlow[A, B](flow: Flow[A, B, _]): Flow[A, B, Future[Int]] = {
    val counterSink = Sink.fold[Int, B](0)((count, _) => count + 1)

    Flow.fromGraph(
      GraphDSL.create(counterSink) { implicit builder =>
        counterSinkShape =>
          import GraphDSL.Implicits._

          val broadcast = builder.add(Broadcast[B](2))
          val originalFlowShape = builder.add(flow)

          originalFlowShape ~> broadcast ~> counterSinkShape

          FlowShape(originalFlowShape.in, broadcast.out(1))
      }
    )
  }

  val simpleSource = Source(1 to 42)
  val simpleFlow = Flow[Int].map(x => x)
  val simpleSink = Sink.ignore

  val enhanceCountFuture = simpleSource.viaMat(enhanceFlow(simpleFlow))(Keep.right).toMat(simpleSink)(Keep.left).run()
  enhanceCountFuture.onComplete {
    case Success(count) => println(s"$count elements went through the enhanced flow")
    case _ => println("something failed")
  }
}

