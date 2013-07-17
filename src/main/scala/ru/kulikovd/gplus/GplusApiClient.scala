package ru.kulikovd.gplus

import scala.concurrent.Future


class GplusApiClient(apiKey: String) {

  def activities(profileId: String, maxResult: Int = 50): Future[List[Activity]] = ???

  def comments(activityId: String, maxResult: Int = 200): Future[List[Comment]] = ???
}
