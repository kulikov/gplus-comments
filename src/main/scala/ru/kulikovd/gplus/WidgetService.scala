package ru.kulikovd.gplus

import scala.concurrent.duration._

import akka.actor.{ActorRef, Actor}
import akka.pattern.ask
import akka.util.Timeout
import spray.http._
import spray.routing.HttpService
import scala.util.Success


class WidgetService(profileRepo: ActorRef) extends Actor with HttpService {
  import context.dispatcher

  implicit val timeout = Timeout(20 seconds)

  def actorRefFactory = context

  def receive = runRoute {
    get {
      (path("pingback") & parameters('profile, 'url)) { (profile, url) ⇒
        respondWithMediaType(MediaTypes.`application/javascript`) {
          complete {
            profileRepo ? ForwardTo(profile, GetActivityCommentsBy(url)) map {
              case CommentsFound(activ, comments) ⇒ activ + " " + comments
              case other ⇒ other.toString
            }
          }
        }
      }
    }
  }
}
