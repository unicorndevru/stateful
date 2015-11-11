package psgr.eventbus

import com.google.inject.ImplementedBy

import akka.actor.Cancellable
import psgr.eventbus.core.{ LocalBusImpl, LocalEvent }

import scala.reflect._

@ImplementedBy(classOf[LocalBusImpl])
trait LocalBus {
  private[eventbus] def emit[T <: Product: ClassTag](e: LocalEvent[T]): LocalEvent[T]

  def emitEvent[T <: Product: ClassTag](event: Option[ModelEvent[T]], prevState: Option[T], currState: Option[T]) = emit(LocalEvent(event, prevState, currState))

  def emitCreated[T <: Product: ClassTag](m: T): T = {
    emitEvent(event = None, prevState = None, currState = Some(m))
    m
  }

  def emitCreated[T <: Product: ClassTag](e: ModelEvent[T], m: T): T = {
    emitEvent(event = Some(e), prevState = None, currState = Some(m))
    m
  }

  def emitChanged[T <: Product: ClassTag](prev: T, curr: T): T = {
    emitEvent(event = None, prevState = Some(prev), currState = Some(curr))
    curr
  }

  def emitChanged[T <: Product: ClassTag](e: ModelEvent[T], prev: T, curr: T): T = {
    emitEvent(event = Some(e), prevState = Some(prev), currState = Some(curr))
    curr
  }

  private[eventbus] def subscribe[T <: Product: ClassTag](f: PartialFunction[LocalEvent[T], Unit]): Cancellable

  def subscribeCreated[T <: Product: ClassTag](f: PartialFunction[T, Unit]): Cancellable = subscribe[T] {
    case LocalEvent(_, None, Some(x: T)) if f.isDefinedAt(x) ⇒ f(x)
  }

  def subscribeCreatedEvents[T <: Product: ClassTag](f: PartialFunction[(ModelEvent[T], T), Unit]): Cancellable = subscribe[T] {
    case LocalEvent(Some(e), None, Some(x)) if f.isDefinedAt((e, x)) ⇒ f((e, x))
  }

  def subscribeChangedEvents[T <: Product: ClassTag](f: PartialFunction[(ModelEvent[T], T, T), Unit]): Cancellable = subscribe[T] {
    case LocalEvent(Some(e), Some(prev), Some(curr)) if f.isDefinedAt((e, prev, curr)) ⇒ f((e, prev, curr))
  }

  def subscribeChanged[T <: Product: ClassTag](f: PartialFunction[(T, T), Unit]): Cancellable = subscribe[T] {
    case LocalEvent(_, Some(prev), Some(curr)) if f.isDefinedAt((prev, curr)) ⇒ f((prev, curr))
  }
}

