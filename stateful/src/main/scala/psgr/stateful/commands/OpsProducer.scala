package psgr.stateful.commands

import psgr.eventbus.ModelEvent
import psgr.stateful.ActiveStateful
import psgr.stateful.events.PersistentEvent

import scala.concurrent.Future
import scala.language.implicitConversions

trait OpsProducer[A <: ActiveStateful] {
  def event[E <: PersistentEvent[A]: Manifest](e: E): ActiveOp[A#Model, A] =
    EventOp[E, A](e)

  def reply[T](v: T): ActiveOp[T, A] =
    ConstOp[T, A](v)

  def replyModel: ActiveOp[A#Model, A] =
    withModel

  def failure(t: Throwable): ActiveOp[Nothing, A] =
    FailureOp(t)

  def future[T](f: Future[T]): ActiveOp[T, A] =
    FutureOp[T, A](_ ⇒ f)

  def withData: ActiveOp[A#Data, A] =
    new WithActiveDataOp[A#Data, A]((a, d) ⇒ d)

  def withState: ActiveOp[A#State, A] =
    new WithActiveDataOp[A#State, A]((a, d) ⇒ a.state(d.asInstanceOf[a.Data]))

  def withModel: ActiveOp[A#Model, A] =
    new WithActiveDataOp[A#Model, A]((a, d) ⇒ a.model(d.asInstanceOf[a.Data]))

  def withContext: ActiveOp[A#Context, A] =
    new WithContextOp[A]()

  def withEvents: ActiveOp[Vector[ModelEvent[A#Model]], A] =
    new WithEventsOp[A]()

  def modifyingData(f: A#Data ⇒ A#Data): ActiveOp[A#Data, A] =
    new ModifyingDataOp[A](f)

  def stateCommand[T](cmd: StateCommand[T, A]): ActiveOp[T, A] =
    withData.flatMap(cmd.action)

  def noop(): ActiveOp[Unit, A] =
    ConstOp(())

  implicit def stateOp[T](cmd: StateCommand[T, A]): ActiveOp[T, A] = stateCommand(cmd)
}
