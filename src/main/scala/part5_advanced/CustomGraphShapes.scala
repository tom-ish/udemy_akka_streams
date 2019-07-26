package part5_advanced

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Balance, GraphDSL, Merge, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape, Graph, Inlet, Outlet, Shape}

import scala.concurrent.duration._
/**
  * Created by Tomohiro on 26 juillet 2019.
  */

object CustomGraphShapes extends App{

  implicit val system = ActorSystem("CustomGraphShapes")
  implicit val materializer = ActorMaterializer()

  // balanced 2x3 shape
  case class Balance2x3 (
                    // Inlet[T] (<=> input), Outlet[T] (<=> output)
                     in0 : Inlet[Int],
                     in1 : Inlet[Int],
                     out0 : Outlet[Int],
                     out1 : Outlet[Int],
                     out2 : Outlet[Int]
                   ) extends Shape {
    override val inlets: Seq[Inlet[_]] = List(in0, in1)
    override val outlets: Seq[Outlet[_]] = List(out0, out1, out2)

    override def deepCopy(): Shape = Balance2x3(
      in0.carbonCopy(),
      in1.carbonCopy(),
      out0.carbonCopy(),
      out1.carbonCopy(),
      out2.carbonCopy()
    )
  }

  val balance2x3impl = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val merge = builder.add(Merge[Int](2))
    val balance = builder.add(Balance[Int](3))

    merge ~> balance

    Balance2x3(
      merge.in(0),
      merge.in(1),
      balance.out(0),
      balance.out(1),
      balance.out(2)
    )
  }

  val balance2x3Graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val slowSource = Source(LazyList.from(1)).throttle(1, 1 second)
      val fastSource = Source(LazyList.from(1)).throttle(2, 1 second)

      def createSink(index: Int) = Sink.fold(0)((count: Int, element: Int) => {
        println(s"[SINK $index] Received $element, current count is $count")
        count+1
      })

      val sink1 = builder.add(createSink(1))
      val sink2 = builder.add(createSink(2))
      val sink3 = builder.add(createSink(3))

      val balance2x3 = builder.add(balance2x3impl)

      slowSource ~> balance2x3.in0
      fastSource ~> balance2x3.in1

      balance2x3.out0 ~> sink1
      balance2x3.out1 ~> sink2
      balance2x3.out2 ~> sink3

      ClosedShape
    }
  )

//  balance2x3Graph.run()

  /**
    * Exercise: generalize the balance component, make it N x M
    */

  case class GenericBalance[T](override val inlets: List[Inlet[T]],
                               override val outlets: List[Outlet[T]]) extends Shape {
    override def deepCopy(): Shape = {
      val inletsCopy = inlets.map(_.carbonCopy())
      val outletsCopy = outlets.map(_.carbonCopy())
      GenericBalance(inletsCopy, outletsCopy)
    }
  }

  object GenericBalance {
    def apply[T](inputCounts : Int, outputCounts: Int) : Graph[GenericBalance[T], NotUsed] =
      GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val merge = builder.add(Merge[T](inputCounts))
      val balance = builder.add(Balance[T](outputCounts))

      merge ~> balance

      GenericBalance(merge.inlets.toList, balance.outlets.toList)
    }
  }

  val genericBalanceGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val slowSource = Source(LazyList.from(1)).throttle(1, 1 second)
      val fastSource = Source(LazyList.from(1)).throttle(2, 1 second)

      def createSink(index: Int) = Sink.fold(0)((count: Int, element: Int) => {
        println(s"[SINK $index] Received $element, current count is $count")
        count+1
      })


      val sink1 = builder.add(createSink(1))
      val sink2 = builder.add(createSink(2))
      val sink3 = builder.add(createSink(3))

      val genericBalance = builder.add(GenericBalance[Int](2, 3))

      slowSource ~> genericBalance.inlets(0)
      fastSource ~> genericBalance.inlets(1)

      genericBalance.outlets(0) ~> sink1
      genericBalance.outlets(1) ~> sink2
      genericBalance.outlets(2) ~> sink3

      ClosedShape
    }
  )

  genericBalanceGraph.run()
  


}
