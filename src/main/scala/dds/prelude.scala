package dds

import scala.language.implicitConversions
import java.util.concurrent.atomic.AtomicReference

package object prelude {
  import org.omg.dds.core.event._
  import org.omg.dds.sub.DataReader

  class MCastReaderListener[T] extends org.omg.dds.sub.DataReaderListener[T] {
    val listeners = new AtomicReference[Map[Int, org.omg.dds.sub.DataReaderListener[T]]](Map())

    def addListener(l: org.omg.dds.sub.DataReaderListener[T]): Int = {
      var done = false
      do {
        val subs = listeners.get()
        val usubs = subs + (l.hashCode() -> l)
        done = listeners.compareAndSet(subs, usubs)
      }
      while(done == false)
      l.hashCode()
    }

    def removeListener(subid: Int) {
      var done = false
      do {
        val subs = listeners.get()
        val usubs = subs - subid
        done = listeners.compareAndSet(subs, usubs)
      }
      while(done == false)
    }

    def onRequestedDeadlineMissed(e: RequestedDeadlineMissedEvent[T]) {
      val subs = listeners.get()
      subs.foreach(s => s._2.onRequestedDeadlineMissed(e))
    }

    def onRequestedIncompatibleQos(e: RequestedIncompatibleQosEvent[T]) {
      val subs = listeners.get()
      subs.foreach(s => s._2.onRequestedIncompatibleQos(e))
    }

    def onSampleRejected(e: SampleRejectedEvent[T]) {
      val subs = listeners.get()
      subs.foreach(s => s._2.onSampleRejected(e))
    }

    def onLivelinessChanged(e: LivelinessChangedEvent[T]) {
      val subs = listeners.get()
      subs.foreach(s => s._2.onLivelinessChanged(e))
    }

    def onDataAvailable(e: DataAvailableEvent[T]) {
      val subs = listeners.get()
      subs.foreach(s => s._2.onDataAvailable(e))

    }

    def onSubscriptionMatched(e: SubscriptionMatchedEvent[T]) {
      val subs = listeners.get()
      subs.foreach(s => s._2.onSubscriptionMatched(e))
    }

    def onSampleLost(e: SampleLostEvent[T]) {
      val subs = listeners.get()
      subs.foreach(s => s._2.onSampleLost(e))
    }
  }

  implicit class ReaderListener[T, Q](val fun: PartialFunction[Any, Q])
    extends org.omg.dds.sub.DataReaderListener[T] {

    def onRequestedDeadlineMissed(e: RequestedDeadlineMissedEvent[T]) {
      val ce = RequestedDeadlineMissed(e)
      if (fun.isDefinedAt(ce)) fun(ce)
    }

    def onRequestedIncompatibleQos(e: RequestedIncompatibleQosEvent[T]) {
      val ce = RequestedIncompatibleQos(e)
      if (fun.isDefinedAt(ce)) fun(ce)
    }

    def onSampleRejected(e: SampleRejectedEvent[T]) {
      val ce = SampleRejected(e)
      if (fun.isDefinedAt(ce)) fun(ce)
    }

    def onLivelinessChanged(e: LivelinessChangedEvent[T]) {
      val ce = LivelinessChanged(e)
      if (fun.isDefinedAt(ce)) fun(ce)
    }

    def onDataAvailable(e: DataAvailableEvent[T]) {
      val evt = DataAvailable(e.getSource.asInstanceOf[DataReader[T]])
      if (fun.isDefinedAt(evt)) fun(evt)
    }

    def onSubscriptionMatched(e: SubscriptionMatchedEvent[T]) {
      val ce = SubscriptionMatched(e)
      if (fun.isDefinedAt(e)) fun(ce)
    }

    def onSampleLost(e: SampleLostEvent[T]) {
      val ce = SampleLost(e)
      if (fun.isDefinedAt(ce)) fun(ce)
    }
  }

  implicit class RichDataReader[T](dr: org.omg.dds.sub.DataReader[T]) {
    var subid = 0
    def listen[Q](fun: PartialFunction[Any, Q]): Int = {
      val mcl = dr.getListener().asInstanceOf[MCastReaderListener[T]]
      subid = mcl.addListener(fun)
      subid
    }
    def deaf() {
      val mcl = dr.getListener().asInstanceOf[MCastReaderListener[T]]
      mcl.removeListener(subid)

    }

    def deaf(lid: Int) {
      val mcl = dr.getListener().asInstanceOf[MCastReaderListener[T]]
      mcl.removeListener(lid)

    }
  }

  implicit class Command[T](fun:  () => T) extends Runnable {
    def run(): Unit = fun()
  }
}
