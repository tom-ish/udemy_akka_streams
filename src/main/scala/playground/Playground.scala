package playground

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}

/**
  * Created by Tomohiro on 24 juillet 2019.
  */

object Playground extends App {

  implicit val actorSystem = ActorSystem("Playground")
  implicit val materializer = ActorMaterializer()

  Source.single("hello, Streams!").to(Sink.foreach(println)).run()

}
