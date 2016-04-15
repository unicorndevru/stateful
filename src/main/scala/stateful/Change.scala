package stateful

import reactivemongo.bson.{ BSONValue, BSONHandler }

trait Change[M] {
  final type State = M

  final def init(f: ⇒ State): PartialFunction[M, M] = {
    case null ⇒ f
  }

  final def noop: PartialFunction[M, M] = PartialFunction.empty

  def applyChange: PartialFunction[M, M]
}

private[stateful] case class DefLens[M](get: () ⇒ M, set: M ⇒ Unit, resp: () ⇒ Any) {
  type State = M
}

private[stateful] case class ChangeCommandDef[M, T <: Change[M]](lens: DefLens[M], name: String, handler: BSONHandler[_ <: BSONValue, _], async: Boolean) {
  type State = M
  def typed = this.asInstanceOf[ChangeCommandDef[State, Change[State]]]
}

private[stateful] case class ChangeEventDef[M, T <: Change[M]](lens: DefLens[M], handler: BSONHandler[_ <: BSONValue, _]) {
  type State = M
  type C = Change[State]
  def typed = this.asInstanceOf[ChangeEventDef[State, C]]
  def set: State ⇒ Unit = lens.set
}

private[stateful] case class ChangeWrapper[M](name: String, handler: BSONHandler[_ <: BSONValue, _], manifest: Manifest[_], async: Boolean) {
  def commandDef(lens: DefLens[M]): ChangeCommandDef[M, _ <: Change[M]] = ChangeCommandDef(lens, name, handler, async)
  def eventDef(lens: DefLens[M]): ChangeEventDef[M, _ <: Change[M]] = ChangeEventDef(lens, handler)
}