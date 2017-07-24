//#full-example
package com.lightbend.akka.sample

import akka.actor._

class Publisher extends Actor with ActorLogging {
  override def receive: Receive = {
    case Publish(message) =>
      log.info("Publish: {}", message)
      // Publishする
      context.system.eventStream.publish(message)
    case any @ _ =>
      log.warning("Unknown message: {}", any)
  }

  override def preStart(): Unit = log.info("Actor is starting")
}

class Subscriber extends Actor with ActorLogging {
  override def receive: Receive = {
    case Text(message) =>
      log.info("Received text: {}", message)
    case Image(filename) =>
      log.info("Received image: {}", filename)
    case any @ _ =>
      log.warning("Unknown message: {}", any)
  }

  override def preStart(): Unit = {
    log.info("Actor is starting")
    // 起動時にSubscribeするメッセージを指定する
    context.system.eventStream.subscribe(self, classOf[Message])
  }

  override def postStop(): Unit = {
    log.info("Actor has stopped")
  }
}

case class Publish(content: Message)

trait Message
case class Text(message: String) extends Message
case class Image(filename: String) extends Message
case class AdHocMessage(message: String)

object EventStream extends App {
  val system = ActorSystem("example")

  val publisher = system.actorOf(Props(classOf[Publisher]), "publisher")
  val subscriber = system.actorOf(Props(classOf[Subscriber]), "subscriber")

  publisher ! Publish(Text("Hello"))
  publisher ! Publish(Image("smile.png"))
  Thread.sleep(10L)

  // PublisherのActorにPublish対象じゃないメッセージを送ってみる
  publisher ! AdHocMessage("World!")

  // SubscriberのActorに個別でメッセージを送ってみる
  subscriber ! AdHocMessage("World!!!")



  // Subscriberが死ぬと、PublisherのSubscriberリストから除外される
  subscriber ! PoisonPill
  Thread.sleep(10L)

  // これはSubscriberに届かず、デッドレターにもならない(デッドレターはタイミング次第でなる場合がある)
  publisher ! Publish(Text("Hello again"))
  Thread.sleep(10L)

  system.terminate()
}

//#main-class
//#full-example
