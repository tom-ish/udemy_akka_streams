package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, OverflowStrategy, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, MergePreferred, RunnableGraph, Sink, Source, Zip, ZipWith}

/**
  * Created by Tomohiro on 25 juillet 2019.
  */

object GraphCycles extends App {

  implicit val system = ActorSystem("GraphCycles")
  implicit val materializer = ActorMaterializer()

  val accelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
                   mergeShape <~ incrementerShape
    ClosedShape
  }

  //  RunnableGraph.fromGraph(accelerator).run()

  // graph cycle deadlock!

  /*
    Solution 1 : MergePreferred
   */
  val actualAccelerator = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(MergePreferred[Int](1))
    val incrementerShape = builder.add(Flow[Int].map { x =>
      println(s"Accelerating $x")
      x + 1
    })

    sourceShape ~> mergeShape ~> incrementerShape
         mergeShape.preferred <~ incrementerShape
    ClosedShape
  }

  //  RunnableGraph.fromGraph(actualAccelerator).run()

  /*
    Solution 2 : Buffers
   */
  val bufferedRepeater = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val sourceShape = builder.add(Source(1 to 100))
    val mergeShape = builder.add(Merge[Int](2))
    val repeaterShape = builder.add(Flow[Int].buffer(10, OverflowStrategy.dropHead).map { x =>
      println(s"Accelerating $x")
      Thread.sleep(100)
      x
    })

    sourceShape ~> mergeShape ~> repeaterShape
    mergeShape <~ repeaterShape
    ClosedShape
  }

  //  RunnableGraph.fromGraph(bufferedRepeater).run()


  /*
    cycle risk deadlocking
    - add bounds to the number of elements in the cycle

    boundedness vs liveness
   */

  /**
    * Challenge: create a fin-in shape
    * - two inputs which will be fed with EXACTLY ONE number
    * - output will emit an INFINITE FIBONACCI SEQUENCE based off those 2 numbers
    *
    * Hint : use  ZipWith, cycles and MergePreferred
    */

  val source1 = Source.single(1)
  val source2 = Source.single(1)
  val output = Sink.foreach[Int]{x => println(x); Thread.sleep(100)}

  val fibonacciGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val firstSourceShape = builder.add(source1)
      val secondSourceShape = builder.add(source2)
      val firstMergeShape = builder.add(MergePreferred[Int](1))
      val secondMergeShape = builder.add(MergePreferred[Int](1))
      val sumShape = builder.add(ZipWith[Int, Int, Int]((a, b) => a + b))
      val broadcast = builder.add(Broadcast[Int](2))
      val outputBroadcast = builder.add(Broadcast[Int](2))
      val outputShape = builder.add(output)


      firstSourceShape ~> firstMergeShape.preferred
                          firstMergeShape ~> broadcast ~> secondMergeShape ~> outputBroadcast ~> sumShape.in0
                                                          secondMergeShape.preferred <~ secondSourceShape
                                             broadcast ~> sumShape.in1
      sumShape.out ~> firstMergeShape.in(0)

      outputBroadcast.out(1) ~> outputShape

      ClosedShape
    }
  )

//  fibonacciGraph.run()


  val fibonacciGenerator2 = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val zip = builder.add(Zip[BigInt, BigInt])
      val mergePreferred = builder.add(MergePreferred[(BigInt, BigInt)](1))
      val fiboLogic = builder.add(Flow[(BigInt, BigInt)].map { pair =>
        Thread.sleep(100)
        (pair._1 + pair._2, pair._1)
      })
      val broadcast = builder.add(Broadcast[(BigInt, BigInt)](2))
      val extractLast = builder.add(Flow[(BigInt, BigInt)].map(_._1))

      zip.out ~> mergePreferred ~> fiboLogic ~> broadcast ~> extractLast
                 mergePreferred.preferred    <~ broadcast

      UniformFanInShape(extractLast.out, zip.in0, zip.in1)
    }


  val fibonacciGraph2 = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder : GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val source1 = builder.add(Source.single[BigInt](1))
      val source2 = builder.add(Source.single[BigInt](1))
      val sink = builder.add(Sink.foreach[BigInt](println))
      val fibo = builder.add(fibonacciGenerator2)

      source1 ~> fibo.in(0)
      source2 ~> fibo.in(1)
      fibo.out ~> sink

      ClosedShape
    }
  )

  fibonacciGraph2.run()
}
