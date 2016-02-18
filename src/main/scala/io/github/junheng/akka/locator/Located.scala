package io.github.junheng.akka.locator

import akka.actor.{ActorContext, ActorSelection}

trait Located {
  def actor(implicit context: ActorContext): ActorSelection

  def guaranteed(retry: Int = 15): Located
}

object Located {

  case class CanNotLocatedGuaranteedService(name: String) extends RuntimeException {
    override def getMessage: String = s"can not located guaranteed service -> $name"
  }

}