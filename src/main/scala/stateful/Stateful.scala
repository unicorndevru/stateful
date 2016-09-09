package stateful

import java.time.Instant

import akka.actor.{ ActorLogging, Status }
import reactivemongo.bson.{ BSONDateTime, BSONDocument, BSONHandler, BSONValue }
import akka.persistence.PersistentActor

trait Stateful extends PersistentActor with ActorLogging with Lockable {
  self ⇒

  private var changeCommandDefs: Map[Manifest[_], ChangeCommandDef[_, _]] = Map.empty
  private var changeEventDefs: Map[String, ChangeEventDef[_, _]] = Map.empty

  protected class register[S](get: ⇒ S, set: S ⇒ Unit, resp: ⇒ Any) {
    val lens = DefLens(() ⇒ get, set, () ⇒ resp)

    def change[T <: Change[S]](name: String, async: Boolean = false)(implicit h: BSONHandler[_ <: BSONValue, T], m: Manifest[T]): Unit = {

      if (changeEventDefs.contains(name)) {
        throw new RuntimeException(s"You're trying to register event with name `$name`, but it's already registered")
      }

      val cd = ChangeWrapper[T#State](
        name, h, m, async
      )
      changeCommandDefs += (cd.manifest → cd.commandDef(lens))
      changeEventDefs += (cd.name → cd.eventDef(lens))
    }
  }

  private val TimestampFieldname = "_t"
  private val LegacyTimestampFieldname = "_timestamp"

  private[this] var _timestamp: Instant = null

  def lastTimestamp: Instant = _timestamp

  def receiveCustom: Receive = PartialFunction.empty

  override def receiveCommand = lockedOr {
    case c: Change[_] if changeCommandDefs.get(manifest[c.type]).nonEmpty ⇒
      log.debug("Received change: {}", c)

      val changeDef = changeCommandDefs(manifest[c.type]).asInstanceOf[ChangeCommandDef[c.State, c.type]]
      val state = changeDef.lens.get()

      log.debug("Going to check the change...")

      // For duplicate idempotent requests, just reply
      if (!c.applyChange.asInstanceOf[PartialFunction[c.State, c.State]].isDefinedAt(state)) {
        log.debug("Duplicate idempotent request received, return: {}", changeDef.lens.resp())
        sender() ! changeDef.lens.resp()

      } else {

        val changedState = c.applyChange(state).asInstanceOf[c.State]
        log.debug("State changed: {}", changedState)

        // If state is not changed, there's no reason to store the event
        if (changedState == state) {
          log.debug("No event is saved, return: {}", changeDef.lens.resp())
          sender() ! changeDef.lens.resp()

        } else {

          val ts = Instant.now()
          val d = BSONDocument(Traversable(
            changeDef.name → changeDef.handler.asInstanceOf[BSONHandler[_, c.type]].write(c).asInstanceOf[BSONValue],
            TimestampFieldname → BSONDateTime(ts.toEpochMilli)
          ))

          log.debug("Going to actually persist an event, async = {}", changeDef.async)

          // Actually persisting
          (if (changeDef.async) persistAsync(d)_ else persist(d)_) { _ ⇒
            log.debug("Event persisted, going to change the state")

            try {
              _timestamp = ts
              changeDef.lens.set(changedState)
              log.debug("Event persisted, going to reply: {}", changeDef.lens.resp())
              sender() ! changeDef.lens.resp()
            } catch {
              case e: Throwable ⇒
                log.error(e, "Failed to change a state after persistence")
                sender() ! Status.Failure(e)
            }
          }
        }
      }

    case c ⇒
      receiveCustom.applyOrElse(c, { (m: Any) ⇒
        log.debug("Message is not handled: {}", m)
        sender() ! Status.Failure(new IllegalArgumentException("Unknown command: " + c))
      })
  }

  override def receiveRecover = {
    case doc: BSONDocument ⇒
      doc.elements.foreach {
        case (`TimestampFieldname` | `LegacyTimestampFieldname`, BSONDateTime(t)) ⇒
          _timestamp = Instant.ofEpochMilli(t)

        case (k, v) if changeEventDefs.contains(k) ⇒
          val changeDef = changeEventDefs(k).typed
          val change = changeDef.handler.asInstanceOf[BSONHandler[v.type, changeDef.C]].read(v)
          val state = changeDef.lens.get()
          if (change.applyChange.isDefinedAt(state.asInstanceOf[changeDef.State])) {
            val changedState = change.applyChange(state.asInstanceOf[changeDef.State])
            changeDef.set(changedState.asInstanceOf[changeDef.State])
          }

        case (k, v) ⇒
          log.warning("Unknown bson event {} with data {}", k, v)
      }
  }
}