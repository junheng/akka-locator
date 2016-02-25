package io.github.junheng.akka.locator

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.github.junheng.akka.monitor.mailbox.SafeMailboxMonitor.{GetSpecificMessageQueueDetail, MessageQueueDetail}

import scala.concurrent.duration._
import scala.language.postfixOps

class LoadMonitor(target: ActorRef, service: Service) extends Actor with ActorLogging {

  import context.dispatcher

  private val monitor = context.system.scheduler.schedule(15 seconds, 1 seconds, LoadMonitor.monitorActorRef, GetSpecificMessageQueueDetail(target.path.toStringWithoutAddress))


  override def postStop(): Unit = {
    super.postStop()
    monitor.cancel()
  }

  override def receive: Receive = {
    case MessageQueueDetail(path, numberOfMessage) =>
      //calculate load and with
      val load = ((numberOfMessage / LoadMonitor.upperLimitOfMessageQueue) * 100).toInt / 100.0
      val status = if (load > 0.1) Service.STATUS_OVERLOAD else Service.STATUS_NORMAL
      service.reportLoad(load, status)
  }
}

object LoadMonitor {

  def props(target: ActorRef, service: Service) = Props(new LoadMonitor(target, service))

  //override this when monitor started
  var monitorActorRef: ActorRef = ActorRef.noSender

  //default 10000 message as load 1.0
  var upperLimitOfMessageQueue = 10000.0
}
