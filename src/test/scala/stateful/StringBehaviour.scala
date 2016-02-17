package stateful

import reactivemongo.bson.{ BSONValue, BSONString, BSONHandler }

object StringBehaviour {
  case class SetString(s: String) extends Change[String] {
    override def applyChange = {
      case _ ⇒ s
    }
  }
  implicit val setString = new BSONHandler[BSONString, SetString] {
    override def read(bson: BSONString) = SetString(bson.value)

    override def write(t: SetString) = BSONString(t.s)
  }

  case class PrependString(s: String) extends Change[String] {
    override def applyChange = {
      case ss ⇒ s + ss
    }
  }

  implicit val prependString = new BSONHandler[BSONString, PrependString] {
    override def read(bson: BSONString) = PrependString(bson.value)

    override def write(t: PrependString) = BSONString(t.s)
  }
  case class AppendString(s: String) extends Change[String] {
    override def applyChange = {
      case ss ⇒ ss + s
    }
  }

  implicit val appendString = new BSONHandler[BSONString, AppendString] {
    override def read(bson: BSONString) = AppendString(bson.value)

    override def write(t: AppendString) = BSONString(t.s)
  }

  case object GetString extends Change[String] {
    override def applyChange = noop
  }
  implicit val getString = new BSONHandler[BSONValue, GetString.type] {
    override def read(bson: BSONValue) = ???

    override def write(t: GetString.type) = ???
  }
}

trait StringBehaviour {
  self: Stateful ⇒
  import StringBehaviour._

  private var string: String = ""

  def stringState: Any = string

  new register(string, string_=, stringState) {
    change[SetString]("setstr")
    change[PrependString]("prepend")
    change[AppendString]("append")
    change[GetString.type]("getstr")
  }
}