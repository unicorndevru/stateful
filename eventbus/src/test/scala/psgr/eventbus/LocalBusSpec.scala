package psgr.eventbus

import org.specs2.mutable.Specification
import psgr.eventbus.core.{ AkkaLocalBus, LocalBusImpl }

class LocalBusSpec extends Specification with CleanableCounter {
  val bus = new LocalBusImpl(new AkkaLocalBus)

  val initWatcherOne: PartialFunction[BusTestModel, Unit] = {
    case model: BusTestModel ⇒
      println("watcher 1!")
      counter.addAndGet(1)
  }

  val initWatcherTwo: PartialFunction[BusTestModel, Unit] = {
    case model: BusTestModel ⇒
      println("watcher 2!")
      counter.addAndGet(2)
  }

  sequential

  "InitBus" should {
    "Watch created event and cancel watcher" in {
      val watcher = bus.subscribeCreated(initWatcherOne)
      bus.emitCreated(BusTestModel.generate)
      counter.get() must_== 1
      watcher.cancel() must_== true
      watcher.isCancelled must_== true
    }

    "Cancel watcher properly.(Should return true for first cancel, and false for next.)" in {
      val watcher1 = bus.subscribeCreated(initWatcherOne)
      watcher1.isCancelled must_== false
      bus.emitCreated(BusTestModel.generate)
      watcher1.cancel() must_== true
      watcher1.isCancelled must_== true
      bus.emitCreated(BusTestModel.generate)
      counter.get() must_== 1
      watcher1.cancel() must_== false
      watcher1.isCancelled must_== true
    }

    "Successfully watch, cancel and watch again." in {
      val watcher1 = bus.subscribeCreated(initWatcherOne)
      val watcher2 = bus.subscribeCreated(initWatcherTwo)
      bus.emitCreated(BusTestModel.generate)
      watcher1.cancel()
      watcher2.cancel()
      counter.get() must_== 3
      watcher1.isCancelled must_== true
      watcher2.isCancelled must_== true
      bus.emitCreated(BusTestModel.generate)
      counter.get() must_== 3
      val watcher3 = bus.subscribeCreated(initWatcherTwo)
      bus.emitCreated(BusTestModel.generate)
      watcher3.cancel()
      counter.get() must_== 5
    }
  }
}
