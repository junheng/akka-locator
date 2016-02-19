package io.github.junheng.akka.locator

import akka.TransportExtension
import akka.actor.{Actor, ActorLogging}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.curator.x.discovery.ServiceInstance

trait Service extends Actor with ActorLogging {

  private val instance = ServiceInstance.builder[String]()
    .payload(remote)
    .name(name)
    .id(identity)
    .build()

  override def preStart(): Unit = {
    super.preStart()
    ServiceLocator.discovery.registerService(instance)
    ServiceLocator.locals += name -> self
    log.info(s"service registered $name - $identity - ${new String(remote)}")
  }

  private def name = s"${self.path.elements.tail.mkString("-")}"

  private def identity = hex(DigestUtils.md5(remote))

  private def remote = self.path.toStringWithAddress(TransportExtension(context.system).address)

  override def postStop(): Unit = {
    super.postStop()
    ServiceLocator.discovery.unregisterService(instance)
    ServiceLocator.locals -= name
    log.info(s"service quited: ${new String(remote)}")
  }

  def hex(buf: Array[Byte]): String = buf.map("%02X" format _).mkString
}
