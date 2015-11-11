package psgr.stateful

import psgr.eventbus.LocalBus

trait LocalEmitter {
  def emitterBus: LocalBus
}
