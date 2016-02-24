package io.github.junheng.akka.locator

import akka.actor.{ActorContext, ActorSelection}
import io.github.junheng.akka.locator.Located.CanNotLocatedGuaranteedService
import org.apache.curator.x.discovery.details.InstanceProvider
import org.apache.curator.x.discovery.{ProviderStrategy, ServiceInstance}

class LocatedService(path: String) extends Located {
  val name = (if (path.startsWith("/user/")) path.replaceFirst("/user/", "") else path).replaceAll("/", "-")

  val service = ServiceLocator.discovery
    .serviceProviderBuilder()
    .providerStrategy(new LoadProviderStrategy())
    .serviceName(name)
    .build()

  service.start()

  //first local then remote, if no service currently block until available
  override def actor(implicit context: ActorContext): ActorSelection = {
    var found = resolveService(context)
    while (found == null) {
      found = resolveService(context)
      Thread.sleep(100)
    }
    found
  }

  def resolveService(context: ActorContext): ActorSelection = {
    ServiceLocator.locals.get(name) match {
      case Some(ref) => context.actorSelection(ref.path)
      case None =>
        Option(service.getInstance()) match {
          case Some(found) => context.actorSelection(found.getPayload.url)
          case None => null
        }
    }
  }

  override def guaranteed(retry: Int): Located = {
    var located = false
    var retryCount = 0
    while (retryCount < retry && !located) {
      ServiceLocator.locals.get(name) match {
        case Some(ref) => located = true
        case None => Option(service.getInstance()) match {
          case Some(found) => located = true
          case None => retryCount += 1
        }
      }
      Thread.sleep(1000)
    }
    if (!located) throw CanNotLocatedGuaranteedService(name)
    this
  }
}

import scala.collection.JavaConversions._


class LoadProviderStrategy extends ProviderStrategy[ServiceLocation] {

  override def getInstance(instanceProvider: InstanceProvider[ServiceLocation]): ServiceInstance[ServiceLocation] = {
    val sorted = instanceProvider.getInstances.filter(_.getPayload.status == "normal").sortWith {
      case (left, right) => left.getPayload.load < right.getPayload.load
    }
    sorted.headOption.orNull
  }
}



