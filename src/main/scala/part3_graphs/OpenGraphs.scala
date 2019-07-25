package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, RunnableGraph, Sink, Source}

/**
  * Created by Tomohiro on 25 juillet 2019.
  */

object OpenGraphs extends App {

  implicit val system = ActorSystem("OpenGraphs")
  implicit val materializer = ActorMaterializer()


  /*
    A composite source that concatenates 2 sources
    - emits ALL the elements from the first source
    - then ALL the elements from the second source
   */
  val source1 = Source(1 to 10)
  val source2 = Source(42 to 1000)


  // step 1
  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder : GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // step 2 - declaring shapes
      val concat = builder.add(Concat[Int](2))

      // step 3 - tying them together
      source1 ~> concat
      source2 ~> concat

      // step 4
      SourceShape(concat.out)
    }
  )

  sourceGraph.to(Sink.foreach(println)).run()


  /*
    Complex sink
  */
  val sink_1 = Sink.foreach[Int](x => println("[Meaningful thing 1] : " + x))
  val sink_2 = Sink.foreach[Int](x => println("[Meaningful thing 2] : " + x))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      broadcast ~> sink_1
      broadcast ~> sink_2

      SinkShape(broadcast.in)
    }
  )

  source1.to(sinkGraph).run()


  /**
    * Challenge - complex flow?
    * Write your own flow that's composed of two other flows
    *   - one that adds 1 to a number
    *   - one that does number * 10
    */

  val incrementer = Flow[Int].map(_ + 1)
  val multiplier = Flow[Int].map(_ * 10)

  val complexFlowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // everything operates on SHAPES, not components

      val incrementerShape = builder.add(incrementer)
      val multiplierShape = builder.add(multiplier)

      incrementerShape ~> multiplierShape

      FlowShape(incrementerShape.in, multiplierShape.out) // SHAPE
    } // static graph
  ) // component

  source1.via(complexFlowGraph).to(Sink.foreach[Int](println)).run()


  /**
    * Exercise : flow from a sink and a source ?
    */
  def fromSinkAndSource[A, B](sink : Sink[A, _], source : Source[B, _]) : Flow[A, B, _] =
    Flow.fromGraph(
      GraphDSL.create() { implicit builder : GraphDSL.Builder[NotUsed] =>
        val sourceShape = builder.add(source)
        val sinkShape = builder.add(sink)

        FlowShape(sinkShape.in, sourceShape.out)
      }
    )

  val f = Flow.fromSinkAndSourceCoupled(Sink.foreach[String](println), Source(1 to 10))
}
