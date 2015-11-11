package psgr.eventbus.core

import javax.inject.Inject

import psgr.eventbus.LocalBus

import scala.reflect._

private[eventbus] class LocalBusImpl @Inject() (bus: AkkaLocalBus) extends LocalBus {
  override private[eventbus] def emit[T <: Product: ClassTag](e: LocalEvent[T]) = {
    bus.publish(e)
    e
  }

  override private[eventbus] def subscribe[T <: Product: ClassTag](f: PartialFunction[LocalEvent[T], Unit]) = {
    bus.subscribe(f.asInstanceOf[bus.Subscriber], classTag[T])
    new CancellableWatcher[AkkaLocalBus](bus, f.asInstanceOf[bus.Subscriber], classTag[T])
  }
}
