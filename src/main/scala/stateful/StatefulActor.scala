package stateful

import org.joda.time.DateTime
import stateful.commands.ActorCommandsHandler
import stateful.events.ActorEventsRecovery

trait StatefulActor[A <: ActiveStateful] extends PersistentBson with ActorCommandsHandler[A] with ActorEventsRecovery[A] {
  var currentDataOpt: Option[active.Data] = None

  override def currentData_=(data: active.Data): Unit = currentDataOpt = Some(data)

  override def currentData: active.Data = currentDataOpt.get

  override var events: Vector[(DateTime, ModelEvent[active.Model])] = Vector.empty
}
