package ru.kulikovd.gplus

import scala.concurrent.{Promise, Future}
import scala.concurrent.duration._
import scala.util.Success

import akka.actor._
import akka.util.Timeout
import spray.client.pipelining._
import spray.http._
import spray.http.HttpHeaders.RawHeader
import spray.http.HttpHeaders.`Set-Cookie`
import spray.json._


class GplusApiClient(apiKey: String) {

  def activities(profileId: String, maxResult: Int = 50) = {

  }

  def activitiesComments(activityId: String, maxResult: Int = 200) = {

  }
}
