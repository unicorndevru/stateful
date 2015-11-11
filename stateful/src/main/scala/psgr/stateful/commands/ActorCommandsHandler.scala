package psgr.stateful.commands

import akka.actor.Status
import psgr.stateful.{ PersistentBson, ActiveStateful }

trait ActorCommandsHandler[A <: ActiveStateful] extends ActorOpsExecutor[A] {
  it: PersistentBson ⇒

  def currentDataOpt: Option[active.Data]

  override def receiveCommand: Receive = lockedOr {
    case m: InitiateCommand[_, A] if currentDataOpt.isEmpty && emptyReceive.isDefinedAt(m) ⇒
      execute(emptyReceive(m))
    case m: StateCommand[_, A] if currentDataOpt.isDefined && stateReceive.isDefinedAt(m → currentData) ⇒
      execute(stateReceive(m → currentDataOpt.get))
    case m ⇒
      sender() ! Status.Failure(new IllegalStateException(s"Cannot handle $m in ${currentDataOpt.fold("empty")(v ⇒ v.toString)} state"))
  }

  def emptyReceive: PartialFunction[InitiateCommand[_, A], ActiveOp[_, A]] = {
    case c ⇒
      c.init
  }

  def stateReceive: PartialFunction[(StateCommand[_, A], A#Data), ActiveOp[_, A]] = {
    case (c, s) if c.filter.isDefinedAt(active.state(currentData)) ⇒
      c.action(s)
  }
}
