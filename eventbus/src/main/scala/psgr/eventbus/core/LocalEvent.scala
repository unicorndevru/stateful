package psgr.eventbus.core

import psgr.eventbus.ModelEvent

import scala.reflect.ClassTag

case class LocalEvent[T <: Product: ClassTag](event: Option[ModelEvent[T]], prevState: Option[T], currState: Option[T]) {
  def modelClassTag = implicitly[ClassTag[T]]
}