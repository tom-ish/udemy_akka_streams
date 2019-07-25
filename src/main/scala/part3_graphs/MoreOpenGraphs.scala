package part3_graphs

import java.util.Date

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

/**
  * Created by Tomohiro on 25 juillet 2019.
  */

object MoreOpenGraphs extends App {

  implicit val system = ActorSystem("MoreOpenGraphs")
  implicit val materializer = ActorMaterializer()

  /*
    Example: Max3 operator
      - 3 inputs of type Int
      - the maximum of the 3
   */

  val max3StaticGraph = GraphDSL.create() { implicit builder : GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val max1 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))
    val max2 = builder.add(ZipWith[Int, Int, Int]((a, b) => Math.max(a, b)))

    max1.out ~> max2.in0

    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source((1 to 10).map(_ => 5))
  val source3 = Source((1 to 10).reverse)

  val maxSink = Sink.foreach[Int](x => println(s"Max is $x"))

  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val max3Shape = builder.add(max3StaticGraph)

      source1 ~> max3Shape.in(0)
      source2 ~> max3Shape.in(1)
      source3 ~> max3Shape.in(2)

      max3Shape.out ~> maxSink

      ClosedShape
    }
  )

  max3RunnableGraph.run()

  // same for UniformFanOutShape

  /*
    Non-uniform fan out shape

    Processing bank transactions
    Transaction is suspicious if amount > 10000

    Streams component for Transaction
    - output1: let the transaction go through
    - output2: suspicious transaction IDs
   */

  case class Transaction(id: String, source: String, recipient: String, amount: Int, date: Date)

  val transactionSource = Source(List(
    Transaction("54234452", "Paul", "Jim", 100, new Date),
    Transaction("23424923", "Daniel", "Jim", 100000, new Date),
    Transaction("52324342", "Jim", "Alice", 7000, new Date)
  ))

  val bankProcessor = Sink.foreach[Transaction](println)
  val suspiciousAnalysisService = Sink.foreach[String](id => println(s"Suspicious transaction ID: $id"))

  val suspiciousTransactionStaticGraph = GraphDSL.create() { implicit builder : GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._

    val broadcast = builder.add(Broadcast[Transaction](2))
    val suspiciousTransactionFilter = builder.add(Flow[Transaction].filter(txn => txn.amount > 10000))
    val transactionIdExtractor = builder.add(Flow[Transaction].map[String](txn => txn.id))

    broadcast.out(0) ~> suspiciousTransactionFilter ~> transactionIdExtractor

    new FanOutShape2(broadcast.in, broadcast.out(1), transactionIdExtractor.out)
  }

  val suspiciousTransactionRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder : GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val suspiciousTransactionShape = builder.add(suspiciousTransactionStaticGraph)

      transactionSource ~> suspiciousTransactionShape.in
      suspiciousTransactionShape.out0 ~> bankProcessor
      suspiciousTransactionShape.out1 ~> suspiciousAnalysisService

      ClosedShape
    }
  )
  suspiciousTransactionRunnableGraph.run()
}
