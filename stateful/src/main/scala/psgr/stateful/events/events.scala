package psgr.stateful.events

import psgr.eventbus.ModelEvent
import psgr.stateful.ActiveStateful

sealed trait PersistentEvent[A <: ActiveStateful]

private[events] sealed trait ChangePersistentEvent[A <: ActiveStateful] extends PersistentEvent[A] {
  def change(d: A#Data): A#Data
}

private[events] sealed trait CreatePersistentEvent[A <: ActiveStateful] extends PersistentEvent[A] {
  def create: A#Data
}

private[stateful] sealed trait WithDomainEvent[A <: ActiveStateful] {
  def domainEvent: ModelEvent[A#Model]
}

abstract class CreateEvent[A <: ActiveStateful](val domainEvent: CreateModelEvent[A]) extends CreatePersistentEvent[A] with WithDomainEvent[A] {
  override def create = domainEvent.create
}

abstract class ChangeEvent[A <: ActiveStateful](val domainEvent: ChangeModelEvent[A]) extends ChangePersistentEvent[A] with WithDomainEvent[A] {

  override def change(d: A#Data) = domainEvent.change(d)
}

abstract class TransitionEvent[A <: ActiveStateful](f: A#Data ⇒ A#Data) extends ChangePersistentEvent[A] {
  override def change(d: A#Data) = f(d)
}

abstract class InitiationEvent[A <: ActiveStateful](f: A#Data) extends CreatePersistentEvent[A] {
  override def create: A#Data = f
}

