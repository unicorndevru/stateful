package psgr.eventbus.core

import javax.inject.Singleton

import akka.event.{ EventBus, LookupClassification }

import scala.reflect.ClassTag
import scala.util.Try

@Singleton
private[eventbus] class AkkaLocalBus extends EventBus with LookupClassification {
  type Event = LocalEvent[_ <: Product]
  type Classifier = ClassTag[_]
  type Subscriber = PartialFunction[LocalEvent[_], Unit]

  override protected def classify(event: Event): Classifier = event.modelClassTag

  override protected def publish(event: Event, subscriber: Subscriber): Unit =
    Try(subscriber.applyOrElse(event, (_: Product) ⇒ ()))

  override protected def compareSubscribers(a: Subscriber, b: Subscriber): Int =
    (a.getClass.getName + a.hashCode()).compareTo(b.getClass.getName + b.hashCode())

  override protected def mapSize(): Int = 128
}
