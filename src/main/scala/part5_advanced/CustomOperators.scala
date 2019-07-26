package part5_advanced

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Attributes, Inlet, Outlet, Shape, SinkShape, SourceShape}
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}

import scala.collection.mutable
import scala.util.Random

/**
  * Created by Tomohiro on 26 juillet 2019.
  */

object CustomOperators extends App {

  implicit val system = ActorSystem("CustomOperators")
  implicit val materializer = ActorMaterializer()

  // 1 - a custom source that emits random numbers until canceled

  class RandomNumberGenerator(max: Int) extends GraphStage[SourceShape[Int]] {

    // step 1: define the ports and the component-specific members
    val outPort = Outlet[Int]("randomGenerator")
    val random = new Random()

    // step 2: construct a new shape
    override def shape: SourceShape[Int] = SourceShape(outPort)

    // step 3: create the logic
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      // step 4:
      // define mutable state
      // implements my logic here by settings handlers on appropriate ports

      setHandlers(outPort, new OutHandler {
        // when there is demand from downstream
        override def onPull(): Unit = {
          // emit a new element
          val nextNumber = random.nextInt(max)
          // push it out of the outPort
          push(outPort, netNumber)
        }
      })
    }
  }

  val randomGeneratorSource = Source.fromGraph(new RandomNumberGenerator(100))
//  randomGeneratorSource.runWith(Sink.foreach(println))


  // 2 - a custom sink that prints elements in batches of a given size
  class Batcher(batchSize: Int) extends GraphStage[SinkShape[Int]] {
    val inPort = Inlet[Int]("batcher")

    override def shape: SinkShape[Int] = ???

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

      override def preStart(): Unit = {
        pull(inPort) // signaling demand upstream for a new element
      }

      // mutable state
      val batch = new mutable.Queue[Int]

      setHandler(inPort, new InHandler {
        // when the upstream wants to send me an element
        override def onPush(): Unit = {
          val nextElement = grab(inPort)
          batch.enqueue(nextElement)

          if(batch.size >= batchSize) {
            println("New batch : " + batch.dequeueAll(_ => true).mkString("[", ",", "]"))
          }

          pull(inPort) // send demand upstream
        }

        override def onUpstreamFinish(): Unit = {
          if(batch.nonEmpty) {
            println("New batch : " + batch.dequeueAll(_ => true).mkString("[", ",", "]"))
            println("Stream finished.")
          }
        }
      })
    }
  }

  val batcherSink = Sink.fromGraph(new Batcher(10))
  randomGeneratorSource.to(batcherSink).run()
}
