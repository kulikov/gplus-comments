package ru.kulikovd.gplus

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import com.typesafe.config.ConfigFactory
import spray.can.Http


object Main extends App {

  implicit val system = ActorSystem()

  val config = ConfigFactory.load().getConfig("gplus")

  val gPlusClient = new GplusApiClient(config.getString("api-key"))
  val storageFactory = new BytecaskStorageFactory(config.getString("storage-dir"))

  val profileRepo = system.actorOf(Props(
    new ProfileRepository(gPlusClient, storageFactory)
  ))

  IO(Http) ! Http.Bind(
    listener  = system.actorOf(Props(new WidgetService(profileRepo))),
    interface = config.getString("server.host"),
    port      = config.getInt("server.port")
  )
}
