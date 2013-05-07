package ru.kulikovd.gplus

import scala.collection.immutable.TreeMap
import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import scala.util.{Success, Failure}

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import ru.kulikovd.gplus.FeedItem


sealed trait ProfileRepoMessages
case class ForwardTo(profileId: String, msg: Any) extends ProfileRepoMessages


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


class Profile(gplusClient: GplusApiClient, storage: Storage) extends Actor with ActorLogging {
  import context.dispatcher

  def receive = {
    case FindActivityBy(str) ⇒
    case GetActivityComments(activityId) ⇒
  }
}
