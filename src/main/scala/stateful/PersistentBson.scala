package stateful

import akka.actor.ActorLogging
import akka.persistence.RecoveryCompleted
import org.joda.time.DateTime
import reactivemongo.bson._

trait PersistentBson extends akka.persistence.PersistentActor with ActorLogging {

  private[this] var classMap: Map[Manifest[_], BSONWriter[_, _]] = Map.empty
  private[this] var namesMap: Map[String, BSONReader[_, _]] = Map.empty

  private val TimestampFieldname = "_timestamp"

  private[this] var _timestamp: DateTime = null

  def lastTimestamp: DateTime = _timestamp

  def registerEventFmt[T: Manifest](name: String, fmt: BSONHandler[_, T]): Unit = {
    classMap = classMap + (manifest[T] → new BSONWriter[T, BSONDocument] {
      override def write(t: T): BSONDocument = BSONDocument(
        Traversable(
          name → fmt.write(t).asInstanceOf[BSONValue],
          TimestampFieldname → BSONDateTime(System.currentTimeMillis())
        )
      )
    })
    namesMap = namesMap + (name → fmt.asInstanceOf[BSONReader[BSONValue, T]])
  }

  def persistBson[T: Manifest](e: T)(f: T ⇒ Unit = (_: T) ⇒ ()) =
    classMap.get(manifest[T]).flatMap(_.asInstanceOf[BSONWriter[T, BSONDocument]].writeOpt(e)) match {
      case Some(bson) ⇒
        persist(bson) { _ ⇒
          _timestamp = bson.get(TimestampFieldname).fold(_timestamp){
            case BSONDateTime(t) ⇒ new DateTime(t)
            case _               ⇒ _timestamp
          }
          receiveRecoverBson(e)
          f(e)
        }
      case None ⇒
        log.error("Event is not registered for persistence: " + manifest[T] + ", available events: " + classMap.keys)
        throw new IllegalArgumentException("Event is not registered for persistence: " + e)
    }

  def receiveRecoverBson: Receive

  def decodeBsonEvent: PartialFunction[Any, Any] = {
    case bson: BSONDocument if bson.elements.headOption.map(_._1).exists(namesMap.contains) ⇒
      val (k, v) = bson.elements.head
      _timestamp = bson.get(TimestampFieldname).fold(_timestamp){
        case BSONDateTime(t) ⇒ new DateTime(t)
        case _               ⇒ _timestamp
      }
      namesMap(k).asInstanceOf[BSONReader[v.type, _]].read(v)

    case bson: BSONDocument ⇒
      log.warning("Unregistered bson event: " + bson.elements.headOption.map(_._1).getOrElse("(empty object)"))
      bson

    case o ⇒
      o
  }

  override def receiveRecover: Receive = decodeBsonEvent andThen receiveRecoverBson.orElse {
    case RecoveryCompleted ⇒
    case m                 ⇒ log.warning(s"${self.path} Unhandled recovery event: " + m)
  }
}
