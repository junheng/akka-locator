package io.github.junheng.akka.locator

case class ServiceLocation(serviceType: String, url: String, load: Double, status: String = "normal")