package part5_advanced

import akka.actor.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitches}

import scala.concurrent.duration._
/**
  * Created by Tomohiro on 26 juillet 2019.
  */

object DynamicStreamHandling extends App {
  implicit val system = ActorSystem("DynamicStreamHandling")
  implicit val materializer = ActorMaterializer()
  import system.dispatcher

  // #1 : Kill Switch
  val killSwitchFlow = KillSwitches.single[Int]

  val counter = Source(LazyList.from(1)).throttle(1, 1 second).log("Counter")
  val sink = Sink.ignore

//  val killSwitch = counter
//    .viaMat(killSwitchFlow)(Keep.right)
//    .to(sink)
//    .run()
//
//  system.scheduler.scheduleOnce(3 seconds) {
//    killSwitch.shutdown()
//  }

  // shared Kill Switch
  val anotherCounter = Source(LazyList.from(1)).throttle(2, 1 second)
  val sharedKillSwitch = KillSwitches.shared("oneButtonToRuleThemAll")

//  counter.via(sharedKillSwitch.flow).runWith(Sink.ignore)
//  anotherCounter.via(sharedKillSwitch.flow).runWith(Sink.ignore)
//
//  system.scheduler.scheduleOnce(3 seconds) {
//    sharedKillSwitch.shutdown()
//  }


  // MergeHub

  val dynamicMerge = MergeHub.source[Int]
//  val materializedSink = dynamicMerge.to(Sink.foreach[Int](println)).run()
//
//  // use this sink anytime we like
//  Source(LazyList.from(1 to 10)).runWith(materializedSink)
//  counter.runWith(materializedSink)

  // BroadcastHub
  val dynamicBroadcast = BroadcastHub.sink[Int]
//  val materializedSource = Source(LazyList.from(1 to 100)).runWith(dynamicBroadcast)
//
//  materializedSource.runWith(Sink.ignore)
//  materializedSource.runWith(Sink.foreach[Int](println))
//
  /**
    * Challenge - combine a mergeHub and a broadcastHub
    *
    *  A publisher-subscriber component
    */
  val merge = MergeHub.source[String]
  val broadcast = BroadcastHub.sink[String]
  val (publisherPort, subscriberPort) = merge.toMat(broadcast)(Keep.both).run()

  subscriberPort.runWith(Sink.foreach(e => println(s"I received $e")))
  subscriberPort.map(string => string.length).runWith(Sink.foreach(number => println(s"I got a number $number")))

  Source("Akka is amazing".split(" ").toList).runWith(publisherPort)
  Source("I love Scala".split(" ").toList).runWith(publisherPort)
  Source.single("STREEEEEEEAMS").runWith(publisherPort)
}
