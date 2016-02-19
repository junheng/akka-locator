package io.github.junheng.akka.locator

import java.util.concurrent.TimeUnit

import akka.TransportExtension
import akka.actor._
import akka.util.Timeout
import org.apache.commons.codec.digest.DigestUtils
import org.apache.curator.framework.{CuratorFramework, CuratorFrameworkFactory}
import org.apache.curator.retry.RetryForever
import org.apache.curator.x.discovery.{ServiceDiscovery, ServiceDiscoveryBuilder, ServiceInstance}
import org.apache.zookeeper.CreateMode

import scala.concurrent.duration._
import scala.language.postfixOps

object ServiceLocator {
  var curator: CuratorFramework = null
  var discovery: ServiceDiscovery[String] = null
  var locals = Map[String, ActorRef]()

  def initialize(zookeeper: String, namespace: String = "dragon") = {
    curator = CuratorFrameworkFactory.builder()
      .namespace(namespace)
      .connectString(zookeeper)
      .sessionTimeoutMs(1000)
      .connectionTimeoutMs(1000)
      .retryPolicy(new RetryForever(1000))
      .build()

    discovery = ServiceDiscoveryBuilder.builder(classOf[String])
      .client(ServiceLocator.curator)
      .basePath("/")
      .build()

    curator.start()
    if (curator.blockUntilConnected(10, TimeUnit.SECONDS)) {
      curator.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL).forPath("/node")
    } else new RuntimeException(s"can not connect to zookeeper: $zookeeper")
  }

  def hex(buf: Array[Byte]): String = buf.map("%02X" format _).mkString

  def register(actorRef: ActorRef, system: ActorSystem) = {
    val name = s"${actorRef.path.elements.tail.mkString("-")}"

    val remote = actorRef.path.toStringWithAddress(TransportExtension(system).address)

    val identity = hex(DigestUtils.md5(remote))

    val instance = ServiceInstance.builder[String]()
      .payload(remote)
      .name(name)
      .id(identity)
      .build()
    ServiceLocator.discovery.registerService(instance)
  }
}


trait ServiceLocator {
  this: Actor with ActorLogging =>
  implicit val timeout = Timeout(10 seconds)

  def located(path: String): Located = new LocatedService(path)
}







