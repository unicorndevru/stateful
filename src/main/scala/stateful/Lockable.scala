package stateful

import akka.actor.{ Actor, ActorLogging, ReceiveTimeout, Stash }

trait Lockable extends Stash {
  it: Actor with ActorLogging ⇒

  private case object Unlock

  @volatile private var locked: Boolean = false
  @volatile private var lockedReceive: Option[Receive] = None

  def receiveTimedOut(): Unit = context.stop(self)

  def lock() = {
    locked = true
    lockedReceive = None
  }

  def lock(r: Receive) = {
    locked = true
    lockedReceive = Some(r)
  }

  def unlock() = {
    self ! Unlock
    lockedReceive = None
  }

  def lockedOr(receive: Receive): Receive = ({
    case Unlock ⇒
      locked = false
      unstashAll()

    case cmd if locked && lockedReceive.exists(_.isDefinedAt(cmd)) ⇒
      lockedReceive.foreach(_(cmd))

    case ReceiveTimeout ⇒
      receiveTimedOut()

    case cmd if locked ⇒
      stash()
  }: Receive).orElse(receive)
}
