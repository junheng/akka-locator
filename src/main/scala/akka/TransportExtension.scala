package akka

import akka.actor.{ExtendedActorSystem, Extension, ExtensionKey}
import akka.remote.RemoteActorRefProvider

class TransportExtension(system: ExtendedActorSystem) extends Extension {
  def address = system.provider match {
    case provider: RemoteActorRefProvider => provider.transport.defaultAddress
    case _ => system.provider.rootPath.address
  }
}

object TransportExtension extends ExtensionKey[TransportExtension]