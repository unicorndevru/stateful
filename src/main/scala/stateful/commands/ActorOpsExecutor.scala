package stateful.commands

import akka.actor.Status
import org.joda.time.DateTime
import stateful._
import stateful.events.{ WithDomainEvent, PersistentEvent }
import scala.reflect.ClassTag

trait ActorOpsExecutor[A <: ActiveStateful] extends OpsProducer[A] with PersistentBson with Lockable {
  val active: A

  def currentDataOpt: Option[active.Data]

  def currentData: active.Data

  def currentData_=(d: active.Data): Unit

  def currentContext: active.Context = ???

  def events: Vector[(DateTime, ModelEvent[active.Model])]

  private case class PersistInThread(e: PersistentEvent[A])

  private case class FutureResolved[T](t: T)

  def execute(action: ActiveOp[_, A]): Unit = {
    val s = sender()
    executeOps(action, (t: Any) ⇒ {
      s ! t
      unlock()
    })
  }

  def afterEvent[E <: PersistentEvent[A]](dataBefore: Option[active.Data], e: E): Unit = {
    this match {
      case em: LocalEmitter ⇒
        val event = e match {
          case w: WithDomainEvent[A] ⇒
            Some(w.domainEvent)
          case _ ⇒
            None
        }
        active.lenses.foreach { l ⇒
          type M = l.Model with Product
          /*TODO: em.emitterBus.emitEvent[M](
            None,
            dataBefore.flatMap(l.f).asInstanceOf[Option[M]],
            currentDataOpt.flatMap(l.f).asInstanceOf[Option[M]]
          )(l.tag.asInstanceOf[ClassTag[M]])*/
        }
      // TODO: em.emitterBus.emitEvent[A#Model](event, dataBefore.map(active.model), currentDataOpt.map(active.model))(active.modelTag.asInstanceOf[ClassTag[A#Model]])
    }
  }

  private def executeOps[T](action: ActiveOp[T, A], andThen: T ⇒ Unit): Unit = action match {
    case co: ComposeOp[_, T, A] ⇒
      executeOps(co.head, (k: co.Ks) ⇒ executeOps(co.tail(k), andThen))

    case wad: WithActiveDataOp[T, A] ⇒
      andThen(wad.lens(active, currentData))

    case wc: WithContextOp[A] ⇒
      andThen(currentContext.asInstanceOf[T])

    case wc: WithEventsOp[A] ⇒
      andThen(events.map(_._2).asInstanceOf[T])

    case ea: EventOp[_, A] ⇒
      log.info("Event: {}", ea)
      implicit val m = ea.manifest.asInstanceOf[Manifest[ea.Event]]
      val dataBefore = currentDataOpt
      persistBson(ea.event.asInstanceOf[ea.Event]) { ev ⇒
        afterEvent(dataBefore, ev)
        andThen(active.model(currentData).asInstanceOf[T])
      }

    case fa: FutureOp[T, A] ⇒
      log.debug("Future: {}", fa)
      val s = sender()
      import context.dispatcher
      lock {
        case sf: Status.Failure ⇒
          log.debug("Failure from future: " + sf, sf)
          s ! sf
          unlock()
        case FutureResolved(t: fa.Msg) ⇒
          andThen(t)
      }
      fa.future(dispatcher) map {
        dm ⇒
          self.tell(FutureResolved(dm), s)
      } recover {
        case e ⇒
          s ! Status.Failure(e)
          unlock()
      }

    case fo: FailureOp[A] ⇒
      sender() ! Status.Failure(fo.failure)
      unlock()

    case ro: ConstOp[T, A] ⇒
      andThen(ro.reply)

    case md: ModifyingDataOp[A] ⇒
      currentData = md.modify(currentData).asInstanceOf[active.Data]
      andThen(currentData.asInstanceOf[T])
  }
}
