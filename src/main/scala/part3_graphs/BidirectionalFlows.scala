package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, BidiShape, ClosedShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}

/**
  * Created by Tomohiro on 25 juillet 2019.
  */

object BidirectionalFlows extends App {

  implicit val system = ActorSystem("BidirectionalFlows")
  implicit val materializer = ActorMaterializer()

  /*
    Example : cryptography
   */
  def encrypt(n: Int)(content: String) : String = content.map(c => (c + n).toChar)
  def decrypt(n: Int)(content: String) : String = content.map(c => (c - n).toChar)

  println(encrypt(3)("Akka"))

  // bi directional flow - bidiFlow
  val bidiCryptoStaticGraph = GraphDSL.create() { implicit builder =>
    val encryptionFlowShape = builder.add(Flow[String].map(encrypt(3)))
    val decryptionFlowShape = builder.add(Flow[String].map(decrypt(3)))

//    BidiShape(encryptionFlowShape.in, encryptionFlowShape.out, decryptionFlowShape.in, decryptionFlowShape.out)
    BidiShape.fromFlows(encryptionFlowShape, decryptionFlowShape)
  }

  val unencryptedStrings = "Akka is awesome, testing bidirectional flows".split(" ").toList
  val unencryptedSource = Source(unencryptedStrings)
  val encryptedSource = Source(unencryptedStrings).map(encrypt(3))

  val cryptoBidiGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val unencryptedSourceShape = builder.add(unencryptedSource)
      val encryptedSourceShape = builder.add(encryptedSource)
      val bidiStaticGraph = builder.add(bidiCryptoStaticGraph)
      val encryptedSinkShape = builder.add(Sink.foreach[String](str => println(s"Encrypted : $str")))
      val decryptedSinkShape = builder.add(Sink.foreach[String](str => println(s"Decrypted : $str")))

      unencryptedSourceShape ~> bidiStaticGraph.in1  ; bidiStaticGraph.out1 ~> encryptedSinkShape
      decryptedSinkShape     <~ bidiStaticGraph.out2 ; bidiStaticGraph.in2  <~ encryptedSourceShape
      // encryptedSourceShape   ~> bidiStaticGraph.in2 ; bidiStaticGraph.out2 ~> decryptedSinkShape

      ClosedShape
    }
  )

  cryptoBidiGraph.run()

  /*
    - encrypting / decrypting
    - encoding / decoding
    - serializing / deserializing
   */
}
