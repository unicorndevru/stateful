package psgr.eventbus

import java.util.UUID

import scala.util.Random

case class BusTestModel(id: String, value: Int)

object BusTestModel {

  def generate = BusTestModel(UUID.randomUUID().toString, Random.nextInt())
}