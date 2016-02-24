package io.github.junheng.akka.locator

import akka.actor.ActorRef
import io.github.junheng.akka.monitor.mailbox.SafeMailboxMonitor.{GetSpecificMessageQueueDetail, MessageQueueDetail}

import scala.concurrent.duration._
import scala.language.postfixOps

trait LoadedService extends Service {

  import context.dispatcher

  private val monitor = context.system.scheduler.schedule(15 seconds, 1 seconds, LoadedService.monitorActorRef, GetSpecificMessageQueueDetail(self.path.toStringWithoutAddress))

  override def postStop(): Unit = {
    super.postStop()
    monitor.cancel()
  }

  abstract override def receive: Receive = super.receive orElse {
    case MessageQueueDetail(path, numberOfMessage) =>
      //calculate load and with
      val load = BigDecimal(numberOfMessage / LoadedService.upperLimitOfMessageQueue).setScale(2, BigDecimal.RoundingMode.HALF_UP).toDouble
      val status = if (load > 0.1) Service.STATUS_OVERLOAD else Service.STATUS_NORMAL
      reportLoad(load, status)
  }
}

object LoadedService {
  //override this when monitor started
  var monitorActorRef: ActorRef = ActorRef.noSender
  
  //default 10000 message as load 1.0
  var upperLimitOfMessageQueue = 10000.0
}
