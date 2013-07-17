package ru.kulikovd.gplus

import scala.util.Success

import akka.actor._

import ru.kulikovd.gplus.proto.Activity.ActivityProto


sealed trait ProfileMessage
case class ForwardTo(profileId: String, msg: Any) extends ProfileMessage
case class GetActivityCommentsBy(substring: String) extends ProfileMessage
case class ActivityFound(activity: Activity) extends ProfileMessage
case class CommentsFound(activity: Activity, comments: List[Comment]) extends ProfileMessage

sealed trait GplusError extends ProfileMessage
case object ActivityNotFound extends GplusError
case class UnexpectedServerError(message: String) extends GplusError


class Activity(val id: String, val url: String, content: String = "") {
  def contains(str: String) = content.contains(str)
}

class Comment(
    val authorName: String,
    val authorImageUrl: String,
    val authorId: String,
    val text: String,
    val url: String,
    val date: String,
    val plusOneValue: Int)


class ProfileRepository(gplusClient: GplusApiClient, storageFactory: StorageFactory) extends Actor with ActorLogging {

  val profiles = collection.mutable.Map.empty[String, ActorRef]

  def receive = {
    case ForwardTo(profileId, msg) ⇒
      profiles.getOrElseUpdate(profileId, createProfile(profileId)) forward msg
  }

  private def createProfile(profileId: String) =
    context.actorOf(Props(
      new Profile(profileId, gplusClient, storageFactory.create(profileId))
    ), name = "profile-" + profileId)
}


class Profile(profileId: String, gplusClient: GplusApiClient, storage: Storage)
  extends Actor
    with Stash
    with ActorLogging {

  import context.dispatcher

  var originSender: Option[ActorRef] = None

  def receive = {
    case GetActivityCommentsBy(str) ⇒
      originSender = Some(sender)
      context.become(loadComments)

      storage.get(str) match {
        case Some(bytes) =>
          self ! ActivityFound(deserialize(bytes))

        case None =>
          gplusClient.activities(profileId) onComplete {
            case Success(activities) =>
              activities find (_.contains(str)) match {
                case Some(activity) =>
                  storage.put(str, serialize(activity))
                  self ! ActivityFound(activity)

                case _ => self ! ActivityNotFound
              }

            case error =>
              self ! UnexpectedServerError(s"Failed load activities fror profile '$profileId'. Reason: $error")
          }
      }
  }

  def loadComments: Receive = {
    case ActivityFound(activity) =>
      gplusClient.comments(activity.id) onComplete {
        case Success(comments) =>
          self ! CommentsFound(activity, comments)

        case error =>
          self ! UnexpectedServerError(s"Not exists comments for activity ${activity.id}. Reason: $error")
      }

    case result @ (_: CommentsFound | _: GplusError) =>
      originSender.get ! result

      originSender = None
      unstashAll()
      context.unbecome()

    case other => stash()
  }

  private def deserialize(bytes: Array[Byte]) = {
    val proto = ActivityProto.parseFrom(bytes)
    new Activity(proto.getId, proto.getUrl)
  }

  private def serialize(activ: Activity) =
    ActivityProto.newBuilder
      .setId(activ.id)
      .setUrl(activ.url)
      .build()
      .toByteArray
}
