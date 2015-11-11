package psgr.stateful.events

import psgr.eventbus.ModelEvent
import psgr.stateful.ActiveStateful

import scala.reflect.ClassTag

abstract class ChangeModelEvent[A <: ActiveStateful](val change: A#Data ⇒ A#Data)(implicit ct: ClassTag[A#Model]) extends ModelEvent[A#Model]

abstract class CreateModelEvent[A <: ActiveStateful](val create: A#Data)(implicit ct: ClassTag[A#Model]) extends ModelEvent[A#Model]