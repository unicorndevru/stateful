package psgr.stateful.commands

import psgr.stateful.ActiveStateful

import scala.collection.GenSetLike

trait ActiveCommand[+T, A <: ActiveStateful] extends OpsProducer[A]

trait InitiateCommand[+T, A <: ActiveStateful] extends ActiveCommand[T, A] {
  def init: ActiveOp[T, A]
}

trait StateCommand[+T, A <: ActiveStateful] extends ActiveCommand[T, A] {

  type StateFilter = PartialFunction[A#State, Unit]

  final def stateIs(s: A#State): StateFilter = {
    case st if st == s ⇒ ()
  }

  final def stateNot(s: A#State): StateFilter = {
    case st if st != s ⇒ ()
  }

  final def stateIn(ss: GenSetLike[A#State, _]): StateFilter = {
    case st if ss.contains(st) ⇒ ()
  }

  def filter: StateFilter = {
    case _ ⇒ ()
  }

  def action(data: A#Data): ActiveOp[T, A]
}