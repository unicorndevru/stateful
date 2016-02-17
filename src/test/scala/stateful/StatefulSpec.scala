package stateful

import java.util.concurrent.TimeUnit

import akka.actor.{ ActorSystem, PoisonPill, Props }
import akka.pattern.ask
import akka.persistence.PersistentActor
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.scalatest.concurrent.{ Eventually, ScalaFutures }
import org.scalatest.time.{ Seconds, Span }
import org.scalatest.{ Matchers, WordSpec }

class StatefulSpec extends WordSpec with MongoSupport with Eventually with Matchers with ScalaFutures {

  implicit val timeout = Timeout(15, TimeUnit.SECONDS)

  def config = ConfigFactory.parseString(
    s"""
       |akka.contrib.persistence.mongodb.mongo.mongouri = "$mongouri"
       |""".stripMargin
  ).withFallback(ConfigFactory.defaultReference())

  override implicit val patienceConfig = PatienceConfig(Span(15, Seconds))

  val system = ActorSystem(getClass.getSimpleName, config)

  class SimpleActor() extends PersistentActor {
    var data: List[String] = Nil

    override def receiveRecover = {
      case s: String ⇒
        data = s :: data
    }

    override def receiveCommand = {
      case s: String ⇒
        persist(s) { _ ⇒
          data = s :: data
          sender() ! data
        }
      case 43 ⇒
        context.stop(self)
        Thread.sleep(100)
        sender() ! 43

      case 13 ⇒
        sender() ! data
    }

    override def persistenceId = "simple-" + self.path.name
  }

  class CounterActor() extends Stateful with CounterBehaviour {
    override def persistenceId = "counter-" + self.path.name
  }

  class ComplexActor() extends Stateful with CounterBehaviour with StringBehaviour {
    override def persistenceId = "complex-" + self.path.name
  }

  "stateful" should {
    "persist in simple actor" in {

      val a = system.actorOf(Props(new SimpleActor()), "s1")

      (a ? "test").futureValue should be("test" :: Nil)

      (a ? 43).futureValue should be(43)

      val b = system.actorOf(Props(new SimpleActor()), "s1")

      (b ? 13).futureValue should be("test" :: Nil)
    }

    "persist and recover from counter actor" in {
      Seq(1, 2, 3).par.foreach { n ⇒
        val a = system.actorOf(Props(new CounterActor), s"c$n")
        import CounterBehaviour.{ GetCounter, MinusOne, PlusOne, SetCounter }

        (a ? PlusOne).futureValue should be(1)
        (a ? PlusOne).futureValue should be(2)
        (a ? PlusOne).futureValue should be(3)
        (a ? MinusOne).futureValue should be(2)
        (a ? MinusOne).futureValue should be(1)
        (a ? MinusOne).futureValue should be(0)
        (a ? PlusOne).futureValue should be(1)

        a ! PoisonPill

        Thread.sleep(100)

        val b = system.actorOf(Props(new CounterActor), s"c$n")
        (b ? PlusOne).futureValue should be(2)
        (b ? GetCounter).futureValue should be(2)
        (b ? SetCounter(2)).futureValue should be(2)
        (b ? SetCounter(5)).futureValue should be(5)

        b ! PoisonPill

        Thread.sleep(100)

        val c = system.actorOf(Props(new CounterActor), s"c$n")
        (c ? GetCounter).futureValue should be(5)
      }
    }

    "persist and recover from complex actor" in {

      Seq(1, 2, 3).par.foreach { n ⇒
        val a = system.actorOf(Props(new ComplexActor), s"x$n")
        import CounterBehaviour.{ GetCounter, MinusOne, PlusOne, SetCounter }
        import StringBehaviour.{ AppendString, GetString, PrependString, SetString }

        (a ? PlusOne).futureValue should be(1)
        (a ? PlusOne).futureValue should be(2)
        (a ? PlusOne).futureValue should be(3)
        (a ? MinusOne).futureValue should be(2)
        (a ? MinusOne).futureValue should be(1)
        (a ? MinusOne).futureValue should be(0)
        (a ? PlusOne).futureValue should be(1)

        a ! PoisonPill

        Thread.sleep(100)

        val b = system.actorOf(Props(new ComplexActor), s"x$n")
        (b ? PlusOne).futureValue should be(2)
        (b ? GetCounter).futureValue should be(2)
        (b ? SetCounter(2)).futureValue should be(2)
        (b ? SetCounter(5)).futureValue should be(5)

        b ! PoisonPill

        Thread.sleep(100)

        val c = system.actorOf(Props(new ComplexActor), s"x$n")
        (c ? GetCounter).futureValue should be(5)

        (c ? GetString).futureValue should be("")
        (c ? SetString("s")).futureValue should be("s")

        c ! PoisonPill

        Thread.sleep(100)

        val d = system.actorOf(Props(new ComplexActor), s"x$n")
        (d ? GetCounter).futureValue should be(5)
        (d ? PrependString("p")).futureValue should be("ps")
        (d ? AppendString("a")).futureValue should be("psa")
        (d ? SetString("psa")).futureValue should be("psa")

        d ! PoisonPill
        Thread.sleep(100)

        val e = system.actorOf(Props(new ComplexActor), s"x$n")
        (e ? GetCounter).futureValue should be(5)
        (e ? GetString).futureValue should be("psa")
      }

    }
  }

}
