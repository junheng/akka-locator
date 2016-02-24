package io.github.junheng.akka.locator

import akka.actor.{Actor, ActorLogging}
import org.apache.curator.x.discovery.ServiceInstance

trait Service extends Actor with ActorLogging {

  private val instance = ServiceLocator.createServiceInstance(self, context.system, 0.0, "normal")

  override def preStart(): Unit = {
    super.preStart()
    ServiceLocator.discovery.registerService(instance)
    ServiceLocator.locals += instance.getId -> self
    log.info(s"service registered ${instance.getName} - ${instance.getId} - ${new String(instance.getPayload.url)}")
  }

  override def postStop(): Unit = {
    super.postStop()
    ServiceLocator.discovery.unregisterService(instance)
    ServiceLocator.locals -= instance.getName
    log.info(s"service quited: ${new String(instance.getPayload.url)}")
  }

  protected def reportLoad(load: Double, status: String = "normal") = {
    ServiceLocator.discovery.updateService(ServiceLocator.createServiceInstance(self, context.system, load, status))
  }

}
