package stateful.events

import akka.persistence.RecoveryCompleted
import org.joda.time.DateTime
import stateful.{ ModelEvent, PersistentBson, ActiveStateful }

trait ActorEventsRecovery[A <: ActiveStateful] {
  it: PersistentBson ⇒

  val active: A

  def currentData: active.Data

  def currentData_=(data: active.Data): Unit

  def dataLastTimestamp(d: active.Data, t: DateTime): active.Data = d

  def currentDataOpt: Option[active.Data]

  def events: Vector[(DateTime, ModelEvent[active.Model])]

  def events_=(v: Vector[(DateTime, ModelEvent[active.Model])]): Unit

  override def receiveRecoverBson: Receive = {
    case e: CreatePersistentEvent[A] if currentDataOpt.isEmpty ⇒
      currentData = dataLastTimestamp(e.create.asInstanceOf[active.Data], lastTimestamp)

      e match {
        case ev: WithDomainEvent[A] ⇒
          events = events :+ (lastTimestamp, ev.domainEvent.asInstanceOf[ModelEvent[active.Model]])
        case _ ⇒
      }

    case e: ChangePersistentEvent[A] if currentDataOpt.isDefined ⇒
      val os = currentData
      currentData = e.change(dataLastTimestamp(currentData, lastTimestamp)).asInstanceOf[active.Data]

      e match {
        case ev: WithDomainEvent[A] ⇒
          events = events :+ (lastTimestamp, ev.domainEvent.asInstanceOf[ModelEvent[active.Model]])
        case _ ⇒
      }

    case RecoveryCompleted ⇒
  }
}
