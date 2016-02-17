package stateful

import reactivemongo.bson.{ BSONInteger, BSONValue, BSONBoolean, BSONHandler }

object CounterBehaviour {

  case object PlusOne extends Change[Int] {
    override def applyChange = {
      case n ⇒ n + 1
    }
  }

  implicit val plusOneHandler = new BSONHandler[BSONBoolean, PlusOne.type] {
    override def read(bson: BSONBoolean) = PlusOne

    override def write(t: PlusOne.type) = BSONBoolean(true)
  }

  case object MinusOne extends Change[Int] {
    override def applyChange = {
      case n ⇒ n - 1
    }
  }

  case class SetCounter(i: Int) extends Change[Int] {
    override def applyChange = {
      case n if n != i ⇒ i
    }
  }
  implicit val setCounterHandler = new BSONHandler[BSONInteger, SetCounter] {
    override def read(bson: BSONInteger) = SetCounter(bson.value)

    override def write(t: SetCounter) = BSONInteger(t.i)
  }

  case object GetCounter extends Change[Int] {
    override def applyChange = noop
  }
  implicit val getCounterHandler = new BSONHandler[BSONValue, GetCounter.type] {
    override def read(bson: BSONValue) = ???

    override def write(t: GetCounter.type) = ???
  }

  implicit val minusOneHandler = new BSONHandler[BSONBoolean, MinusOne.type] {
    override def read(bson: BSONBoolean) = MinusOne

    override def write(t: MinusOne.type) = BSONBoolean(false)
  }
}

trait CounterBehaviour {
  self: Stateful ⇒

  import CounterBehaviour._

  private var counter: Int = 0

  def counterState: Any = counter

  new register(counter, counter_=, counterState) {
    change[PlusOne.type]("plus")
    change[MinusOne.type]("minus")
    change[SetCounter]("setc")
    change[GetCounter.type]("getc")
  }
}