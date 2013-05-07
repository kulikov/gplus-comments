package ru.kulikovd.gplus


import akka.actor._
import akka.pattern.ask
import akka.actor.Status.Success
import scala.concurrent.Promise
import spray.client.pipelining._
import spray.http._
import spray.http.HttpHeaders.RawHeader
import spray.http.HttpHeaders.`Set-Cookie`
import spray.json._


sealed trait ProfileRepoMessage
case class ForwardTo(profileId: String, msg: Any) extends ProfileRepoMessage

sealed trait ProfileMessage
case class FindActivityBy(str: String) extends ProfileMessage
case class GetActivityComments(activityId: String) extends ProfileMessage
case class GetActivityCommentsBy(str: String) extends ProfileMessage


case class Activity(
  id: String,
  url: String,
  content: String = ""
)

case class Comment(
  authorName: String,
  authorImageUrl: String,
  authorId: String,
  text: String,
  url: String,
  date: String,
  plusOneValue: Int
)

case class Comments(items: List[Comment])


class ProfileRepository(gplusClient: GplusApiClient, storageFactory: StorageFactory) extends Actor with ActorLogging {

  val profiles = collection.mutable.Map.empty[String, ActorRef]

  def receive = {
    case ForwardTo(profileId, msg) ⇒
      profiles getOrElseUpdate(profileId, createProfile(profileId)) forward msg
  }

  private def createProfile(profileId: String) =
    context.actorOf(Props(
      new Profile(profileId, gplusClient, storageFactory.create(profileId))
    ), name = "profile/" + profileId)
}


class Profile(profileId: String, gplusClient: GplusApiClient, storage: Storage) extends Actor with ActorLogging {

  def receive = {
    case FindActivityBy(str) ⇒
      storage.get(str) map (bytes ⇒ Promise.successful(Some(deserialize(bytes)))) getOrElse {
        gplusClient.activities(profileId) map { case json: String =>
          JsonParser(json).asJsObject.fields.get("items") match {
            case Some(JsArray(items)) ⇒
              items find(_.toString().contains(str)) map { case JsObject(act) ⇒
                val activity = Activity(
                  id = act("id").asInstanceOf[JsString].value,
                  url = act("url").asInstanceOf[JsString].value
                )

                storage.put(str, serialize(activity))

                activity
              }

            case other ⇒
              log.error("Malformed json format {}", json)
              None
          }
        }
      }

    case GetActivityComments(activityId) ⇒
      ???

    case GetActivityCommentsBy(str) ⇒
      val originalSender = sender

      self ? FindActivityBy(str) onComplete {
        case Success(activ: Activity) ⇒
          self ? GetActivityComments(activ.id) onComplete {
            case Success(cs: Comments) ⇒
              originalSender ! (activ, cs)

            case other ⇒
              originalSender ! other
          }

        case other ⇒
          originalSender ! other
      }
  }

  private def deserialize(bytes: Array[Byte]) = {
    val proto = ActivityProto.parseFrom(bytes)
    Activity(proto.getId, proto.getUrl)
  }

  private def serialize(activ: Activity) =
    ActivityProto.newBuilder
      .setId(activ.id)
      .setUrl(activ.url)
      .build
      .toByteArray
}
