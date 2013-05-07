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


trait StorageFactory {
  def create(name: String): Storage
}


trait Storage {
  def get(key: String): Option[Array[Byte]]
  def put(key: String, data: Array[Byte])
}


class BytecaskStorageFactory(rootPath: String) extends StorageFactory {
  def create(name: String) = ???
}


class BytecaskStorage extends Storage {
  def get(key: String) = ???

  def put(key: String, data: Array[Byte]) {}
}
