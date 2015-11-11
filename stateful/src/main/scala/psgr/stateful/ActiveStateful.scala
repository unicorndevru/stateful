package psgr.stateful

import scala.reflect.ClassTag

trait ActiveStateful {
  type Context
  type Data
  type State
  type Model <: Product

  def modelTag: ClassTag[Model]

  def state(d: Data): State

  def model(d: Data): Model

  case class DataLens[T <: Product: ClassTag](f: Data ⇒ Option[T]) {
    type Model = T

    val tag: ClassTag[Model] = implicitly[ClassTag[T]]
  }

  val lenses: Seq[DataLens[_]] = Seq()
}
