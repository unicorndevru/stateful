package psgr.eventbus

import java.util.concurrent.atomic.AtomicInteger

import org.specs2.specification.AfterEach

trait CleanableCounter extends AfterEach {

  val counter = new AtomicInteger(0)

  def after = cleanCounter()

  private def cleanCounter() = counter.set(0)
}