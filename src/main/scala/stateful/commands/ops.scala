package stateful.commands

import stateful.{ ModelEvent, ActiveStateful }
import stateful.events.PersistentEvent

import scala.concurrent.{ ExecutionContext, Future }

sealed trait ActiveOp[+T, A <: ActiveStateful] {

  def andThen[K](a: ActiveOp[K, A]): ActiveOp[K, A] =
    flatMap(_ ⇒ a)

  def ~[K](a: ActiveOp[K, A]): ActiveOp[K, A] =
    andThen(a)

  def flatMap[K](f: T ⇒ ActiveOp[K, A]): ActiveOp[K, A] =
    new ComposeOp[T, K, A](this, f)

  def map[K](f: T ⇒ K): ActiveOp[K, A] =
    flatMap(k ⇒ ConstOp(f(k)))

  def apply[K](f: T ⇒ ActiveOp[K, A]): ActiveOp[K, A] =
    flatMap(f)

  def withFilter(f: T ⇒ Boolean): ActiveOp[T, A] =
    flatMap(v ⇒ if (f(v)) this else FailureOp(new IllegalStateException("Operation cannot be executed")))
}

class ComposeOp[K, T, A <: ActiveStateful](val head: ActiveOp[K, A], val tail: K ⇒ ActiveOp[T, A]) extends ActiveOp[T, A] {
  type Ks = K
}

class WithActiveDataOp[T, A <: ActiveStateful](val lens: (A, A#Data) ⇒ T) extends ActiveOp[T, A]

class ModifyingDataOp[A <: ActiveStateful](val modify: A#Data ⇒ A#Data) extends ActiveOp[A#Data, A]

class WithContextOp[A <: ActiveStateful]() extends ActiveOp[A#Context, A]

class WithEventsOp[A <: ActiveStateful]() extends ActiveOp[Vector[ModelEvent[A#Model]], A]

case class EventOp[E <: PersistentEvent[A]: Manifest, A <: ActiveStateful](event: E) extends ActiveOp[A#Model, A] {
  type Event = E

  def manifest: Manifest[Event] = implicitly[Manifest[E]]
}

case class ConstOp[T, A <: ActiveStateful](reply: T) extends ActiveOp[T, A] {
  override def map[K](f: T ⇒ K): ActiveOp[K, A] = ConstOp(f(reply))
}

case class FailureOp[A <: ActiveStateful](failure: Throwable) extends ActiveOp[Nothing, A] {
  override def map[K](f: (Nothing) ⇒ K) = this
}

case class FutureOp[T, A <: ActiveStateful](future: ExecutionContext ⇒ Future[T]) extends ActiveOp[T, A] {
  type Msg = T
}