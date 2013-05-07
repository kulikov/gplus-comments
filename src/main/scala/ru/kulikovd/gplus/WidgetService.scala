package ru.kulikovd.gplus

import scala.concurrent.duration._

import akka.actor.{ActorRef, Actor}
import akka.pattern.ask
import akka.util.Timeout
import spray.http._
import spray.routing.HttpService
import akka.actor.Status.Success


class WidgetService(profileRepo: ActorRef) extends Actor with HttpService {

  implicit val timeout = Timeout(20 seconds)

  def actorRefFactory = context

  def receive = runRoute {
    get {
      (path("pingback") & parameters('profile, 'url)) { (profileId, url) ⇒
        respondWithMediaType(MediaTypes.`application/javascript`) {
          complete {
            profileRepo ? ForwardTo(profileId, GetActivityCommentsBy(url)) onComplete {
              case Success((activ: Activity, Comments(items))) ⇒
                activ + " " + items

              case other ⇒
                other
            }
          }
        }
      }
    }
  }
}
