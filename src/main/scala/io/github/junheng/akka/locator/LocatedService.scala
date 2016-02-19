package io.github.junheng.akka.locator

import akka.actor.{ActorContext, ActorSelection}
import io.github.junheng.akka.locator.Located.CanNotLocatedGuaranteedService
import org.apache.curator.x.discovery.strategies.RoundRobinStrategy

class LocatedService(path: String) extends Located {
  val name = (if (path.startsWith("/user/")) path.replaceFirst("/user/", "") else path).replaceAll("/","-")

  val service = ServiceLocator.discovery
    .serviceProviderBuilder()
    .providerStrategy(new RoundRobinStrategy())
    .serviceName(name)
    .build()

  service.start()

  //first local then remote
  override def actor(implicit context: ActorContext): ActorSelection = {
    ServiceLocator.locals.get(name) match {
      case Some(ref) => context.actorSelection(ref.path)
      case None =>
        Option(service.getInstance()) match {
          case Some(found) => context.actorSelection(found.getPayload)
          case None => context.actorSelection("/deadLetters")
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


