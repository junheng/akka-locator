package io.github.junheng.akka.locator

import akka.actor.{ActorContext, ActorSelection}
import akka.event.LoggingAdapter
import io.github.junheng.akka.locator.Located.CanNotLocatedGuaranteedService
import org.apache.curator.x.discovery.details.InstanceProvider
import org.apache.curator.x.discovery.{ProviderStrategy, ServiceInstance}

import scala.util.Random

class LocatedService(path: String)(implicit log: LoggingAdapter) extends Located {
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
    var retry = 0
    if (found == null) log.info(s"no available service [$path], suspend current service and wait...")
    while (found == null) {
      found = resolveService(context)
      Thread.sleep(100)
      retry += 0
    }
    if(retry > 0) log.info(s"hardly found service [$path] after $retry reties")
    found
  }

  override def actorOpt(implicit context: ActorContext): Option[ActorSelection] = Option(resolveService(context))

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

  private def resolveService(context: ActorContext): ActorSelection = {
    ServiceLocator.locals.get(name) match {
      case Some(ref) => context.actorSelection(ref.path)
      case None =>
        Option(service.getInstance()) match {
          case Some(found) => context.actorSelection(found.getPayload.url)
          case None => null
        }
    }
  }
}

import scala.collection.JavaConversions._


class LoadProviderStrategy extends ProviderStrategy[ServiceLocation] {

  override def getInstance(instanceProvider: InstanceProvider[ServiceLocation]): ServiceInstance[ServiceLocation] = {
    val normal = instanceProvider.getInstances.filter(_.getPayload.status == "normal")
    if (normal.nonEmpty) {
      normal(Random.nextInt(normal.length))
    }else null
  }
}



