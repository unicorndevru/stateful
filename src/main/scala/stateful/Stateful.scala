package stateful

import akka.actor.{ Status, ActorLogging }
import org.joda.time.DateTime
import reactivemongo.bson.{ BSONDateTime, BSONDocument, BSONHandler, BSONValue }
import akka.persistence.PersistentActor

trait Stateful extends PersistentActor with ActorLogging with Lockable {
  self ⇒

  private var changeCommandDefs: Map[Manifest[_], ChangeCommandDef[_, _]] = Map.empty
  private var changeEventDefs: Map[String, ChangeEventDef[_, _]] = Map.empty

  protected class register[S](get: ⇒ S, set: S ⇒ Unit, resp: ⇒ Any) {
    val lens = DefLens(() ⇒ get, set, () ⇒ resp)

    def change[T <: Change[S]](name: String)(implicit h: BSONHandler[_ <: BSONValue, T], m: Manifest[T]): Unit = {

      if (changeEventDefs.contains(name)) {
        throw new RuntimeException(s"You're trying to register event with name `$name`, but it's already registered")
      }

      val cd = ChangeWrapper[T#State](
        name, h, m
      )
      changeCommandDefs += (cd.manifest → cd.commandDef(lens))
      changeEventDefs += (cd.name → cd.eventDef(lens))
    }
  }

  private val TimestampFieldname = "_t"
  private val LegacyTimestampFieldname = "_timestamp"

  private[this] var _timestamp: DateTime = null

  def lastTimestamp: DateTime = _timestamp

  def receiveCustom: Receive = PartialFunction.empty

  override def receiveCommand = lockedOr {
    case c: Change[_] if changeCommandDefs.get(manifest[c.type]).nonEmpty ⇒
      val changeDef = changeCommandDefs(manifest[c.type]).asInstanceOf[ChangeCommandDef[c.State, c.type]]
      val state = changeDef.lens.get()
      if (!c.applyChange.asInstanceOf[PartialFunction[c.State, c.State]].isDefinedAt(state)) {
        sender() ! changeDef.lens.resp()
      } else {
        val changedState = c.applyChange(state).asInstanceOf[c.State]
        if (changedState == state) {
          sender() ! changeDef.lens.resp()
        } else persist(BSONDocument(Traversable(
          changeDef.name → changeDef.handler.asInstanceOf[BSONHandler[_, c.type]].write(c).asInstanceOf[BSONValue],
          TimestampFieldname → BSONDateTime(System.currentTimeMillis())
        ))) { _ ⇒
          changeDef.lens.set(changedState)
          sender() ! changeDef.lens.resp()
        }
      }

    case c ⇒
      receiveCustom.applyOrElse(c, (_: Any) ⇒
        sender() ! Status.Failure(new IllegalArgumentException("Unknown command: " + c)))
  }

  override def receiveRecover = {
    case doc: BSONDocument ⇒
      doc.elements.toSeq.foreach {
        case (`TimestampFieldname` | `LegacyTimestampFieldname`, BSONDateTime(t)) ⇒
          _timestamp = new DateTime(t)

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