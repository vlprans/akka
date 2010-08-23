/**
 * Copyright (C) 2009-2010 Scalable Solutions AB <http://scalablesolutions.se>
 */

package se.scalablesolutions.akka.dataflow

import org.scalatest.Spec
import org.scalatest.Assertions
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterAll
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith

import java.util.concurrent.{TimeUnit, CountDownLatch}
import java.util.concurrent.atomic.{AtomicLong, AtomicReference, AtomicInteger}

import scala.annotation.tailrec

import se.scalablesolutions.akka.dispatch.DefaultCompletableFuture
import se.scalablesolutions.akka.actor.ActorRegistry

@RunWith(classOf[JUnitRunner])
class DataFlowTest extends Spec with ShouldMatchers with BeforeAndAfterAll {
    describe("DataflowVariable") {
      it("should work and generate correct results") {
        import DataFlow._

        val latch = new CountDownLatch(1)
        val result = new AtomicInteger(0)
        val x, y, z = new DataFlowVariable[Int]
        thread {
          z << x() + y()
          latch.countDown
          result.set(z())
        }
        thread { x << 40 }
        thread { y << 2 }

        latch.await(3,TimeUnit.SECONDS) should equal (true)
        List(x,y,z).foreach(_.shutdown)
        result.get should equal (42)
        ActorRegistry.shutdownAll
      }

      it("should be able to transform a stream") {
        import DataFlow._

        def ints(n: Int, max: Int): List[Int] =
          if (n == max) Nil
          else n :: ints(n + 1, max)

        def sum(s: Int, stream: List[Int]): List[Int] = stream match {
          case Nil => s :: Nil
          case h :: t => s :: sum(h + s, t)
        }

        val latch = new CountDownLatch(1)
        val result = new AtomicReference[List[Int]](Nil)
        val x = new DataFlowVariable[List[Int]]
        val y = new DataFlowVariable[List[Int]]
        val z = new DataFlowVariable[List[Int]]

        thread { x << ints(0, 1000) }
        thread { y << sum(0, x())   }

        thread { z << y()
          result.set(z())
          latch.countDown
        }

        latch.await(3,TimeUnit.SECONDS) should equal (true)
        List(x,y,z).foreach(_.shutdown)
        result.get should equal (sum(0,ints(0,1000)))
        ActorRegistry.shutdownAll
      }
    }

    /*it("should be able to join streams") {
      import DataFlow._

      def ints(n: Int, max: Int, stream: DataFlowStream[Int]): Unit = if (n != max) {
        stream <<< n
        ints(n + 1, max, stream)
      }

      def sum(s: Int, in: DataFlowStream[Int], out: DataFlowStream[Int]): Unit = {
        out <<< s
        sum(in() + s, in, out)
      }

      val producer = new DataFlowStream[Int]
      val consumer = new DataFlowStream[Int]
      val latch = new CountDownLatch(1)
      val result = new AtomicInteger(0)

      thread { ints(0, 1000, producer) }
      thread {
        Thread.sleep(1000)
        result.set(producer.map(x => x * x).foldLeft(0)(_ + _))
        latch.countDown
      }
      
      latch.await(3,TimeUnit.SECONDS) should equal (true)
      result.get should equal (332833500)
      ActorRegistry.shutdownAll
    }

    it("should be able to sum streams recursively") {
      import DataFlow._

      def ints(n: Int, max: Int, stream: DataFlowStream[Int]): Unit = if (n != max) {
        stream <<< n
        ints(n + 1, max, stream)
      }

      def sum(s: Int, in: DataFlowStream[Int], out: DataFlowStream[Int]): Unit = {
        out <<< s
        sum(in() + s, in, out)
      }

      val result = new AtomicLong(0)

      val producer = new DataFlowStream[Int]
      val consumer = new DataFlowStream[Int]
      val latch = new CountDownLatch(1)

      @tailrec def recurseSum(stream: DataFlowStream[Int]): Unit = {
        val x = stream()

        if(result.addAndGet(x) == 166666500)
          latch.countDown

        recurseSum(stream)
      }

      thread { ints(0, 1000, producer) }
      thread { sum(0, producer, consumer) }
      thread { recurseSum(consumer) }
      
      latch.await(15,TimeUnit.SECONDS) should equal (true)
      ActorRegistry.shutdownAll
    }*/

  /* Test not ready for prime time, causes some sort of deadlock */
  /*  it("should be able to conditionally set variables") {

    import DataFlow._

    val latch  = new CountDownLatch(1)
    val x, y, z, v = new DataFlowVariable[Int]

    val main = thread {
      x << 1
      z << Math.max(x(),y())
      latch.countDown
    }

    val setY = thread {
      Thread sleep 2000
      y << 2
    }

    val setV = thread {
      v << y
    }

    latch.await(2,TimeUnit.SECONDS) should equal (true)
    List(x,y,z,v) foreach (_.shutdown)
    List(main,setY,setV) foreach (_ ! Exit)
    println("Foo")
    ActorRegistry.shutdownAll
  }*/
}
