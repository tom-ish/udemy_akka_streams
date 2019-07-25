package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

/**
  * Created by Tomohiro on 25 juillet 2019.
  */

object GraphsBasics extends App {

  implicit val system = ActorSystem("GraphsBasics")
  implicit val materializer = ActorMaterializer()

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(_ + 1) // hard computation
  val multiplier = Flow[Int].map(_ * 10) // hard computation
  val output = Sink.foreach[(Int, Int)](println)

  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder : GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._ // brings some nice operators into scope

      // step 2 - add the necessary shapes for this graph
      val broadcast = builder.add(Broadcast[Int](2)) // fan-out operator
      val zip = builder.add(Zip[Int, Int]) // fan-in operator

      // step 3 - tying up the shapes
      input ~> broadcast

      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier  ~> zip.in1

      zip.out ~> output

      // step 4 - return a closed shape
      ClosedShape // FREEZE the builder's shape
      // shape
    } // (static) graph
  ) // runnable graph

  //  graph.run() // run the graph and materialize it

  /**
    * Exercise 1 - feed a source into 2 sinks at the same time (hint: use a broadcast)
    */

  val sink1 = Sink.foreach[Int](println)
  val sink2 = Sink.foreach[Int](println)

  val broadcastGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      input ~> broadcast ~> sink1 // implicit port numbering
               broadcast ~> sink2

//      input ~> broadcast
//
//      broadcast.out(0) ~> sink1
//      broadcast.out(1) ~> sink2

      ClosedShape
    }
  )

  broadcastGraph.run()


  /**
    * Exercise 2 - balance
    */

    import scala.concurrent.duration._
  val fastSource = Source(1 to 1000).throttle(5, 1 second)
  val slowSource = Source(1 to 1000).throttle(2, 1 second)
  val sink_1 = Sink.foreach[Int](x => println("[Sink 1] : " + x))
  val sink_2 = Sink.foreach[Int](x => println("[Sink 2] : " + x))

  val balanceGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder : GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      fastSource ~> merge ~> balance ~> sink_1
      slowSource ~> merge;   balance ~> sink_2


      ClosedShape
    }
  )

  balanceGraph.run()
}
