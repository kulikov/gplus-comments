package ru.kulikovd.gplus

import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import scala.util.Success

import akka.actor._
import akka.util.Timeout
import spray.client.pipelining._
import spray.http._
import spray.http.Uri._
import spray.json._


class GplusApiClient(apiKey: String, system: ActorSystem) {
  import system.dispatcher

  implicit def actorRefFactory = system

  private val pipeline: HttpRequest => Future[HttpResponse] = sendReceive

  private val url = Uri("https://www.googleapis.com/")

  implicit private def js2string(js: JsValue): String = js match {
    case JsString(v) => v
    case JsNumber(n) => n.toInt.toString
    case other => other.toString
  }
  
  def activities(profile: String, maxResults: Int = 50): Future[List[Activity]] = 
    sendReceive.apply(Get(Uri(s"https://www.googleapis.com/plus/v1/people/$profile/activities/public").copy(
      query = Query(
        "key" -> apiKey,
        "maxResults" -> maxResults.toString,
        "fields" -> "items(id,url,object(content,attachments(content,displayName,embed/url,url)))"
      )))) map {
        case HttpResponse(StatusCodes.OK, entity, _, _) ⇒
          JsonParser(entity.asString).asJsObject.fields("items").asInstanceOf[JsArray].elements collect { 
            case JsObject(item) ⇒ 
              new Activity(
                id = item("id"),
                url = item("url"),
                content = item("object") match {
                  case JsObject(obj) ⇒ obj("content") + (obj.get("attachments") collect { case JsArray(atts) =>
                    atts collect { case JsObject(at) => at.mapValues(js2string).mkString }
                  })
                  case _ => throw new Exception
                })
          }
      }

  def comments(activity: String, maxResults: Int = 200): Future[List[Comment]] = 
    sendReceive.apply(Get(Uri(s"https://www.googleapis.com/plus/v1/activities/$activity/comments").copy(
      query = Query(
        "key" -> apiKey,
        "maxResults" -> maxResults.toString,
        "fields" -> "items(actor(displayName,id,image),object/content,published,plusoners/totalItems)"
      )))) map {
        case HttpResponse(StatusCodes.OK, entity, _, _) ⇒
          JsonParser(entity.asString) match {
            case JsObject(ent) if ent.contains("error") ⇒
              throw new Exception("Post not found. " + ent("error").asJsObject.fields("message"))

            case JsObject(ent) ⇒ ent("items").asInstanceOf[JsArray].elements collect { 
              case JsObject(item) ⇒
                val actor = item("actor").asJsObject.fields
                new Comment(
                  authorName = actor("displayName"),
                  authorImageUrl = actor("image").asJsObject.fields("url"),
                  authorId = actor("id"),
                  text = item("object").asJsObject.fields("content"),
                  date = item("published"),
                  plusOneValue = item("plusoners").asJsObject.fields("totalItems").toString.toInt
                )
            }

            case _ => throw new Exception
          }
      }
}
