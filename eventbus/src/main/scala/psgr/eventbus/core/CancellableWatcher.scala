package psgr.eventbus.core

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.Cancellable
import akka.event.{ EventBus, LookupClassification }

class CancellableWatcher[B <: EventBus with LookupClassification](bus: B, subscriber: B#Subscriber, classifier: B#Classifier) extends Cancellable {

  private val cancelled: AtomicBoolean = new AtomicBoolean(false)

  override def cancel(): Boolean =
    if (!isCancelled &&
      bus.unsubscribe(subscriber.asInstanceOf[bus.Subscriber], classifier.asInstanceOf[bus.Classifier])) {
      cancelled.set(true)
      true
    } else {
      false
    }

  override def isCancelled: Boolean = cancelled.get()
}