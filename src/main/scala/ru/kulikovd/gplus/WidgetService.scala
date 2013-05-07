package ru.kulikovd.gplus

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._

import akka.actor.{ActorRef, Actor}
import akka.pattern.ask
import akka.util.Timeout
import spray.can.Http
import spray.can.server.Stats
import spray.http._
import spray.routing.HttpService


class WidgetService(profileRepo: ProfileRepository) extends Actor with HttpService {
  import context.dispatcher

  implicit val timeout = Timeout(20 seconds)

  def actorRefFactory = context

  def receive = runRoute {
    get {
      path("pingback") {
        respondWithMediaType(MediaTypes.`application/javascript`) {
          complete {
            """{"result": "ok!"}"""
          }
        }
      }
    }
  }
}
