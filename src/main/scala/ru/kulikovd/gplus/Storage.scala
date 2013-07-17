package ru.kulikovd.gplus

import com.github.bytecask.Bytecask


trait StorageFactory {
  def create(name: String): Storage
}


trait Storage {
  def get(key: String): Option[Array[Byte]]
  def put(key: String, data: Array[Byte])
}


class BytecaskStorageFactory(rootPath: String) extends StorageFactory {
  def create(name: String) = new BytecaskStorage(rootPath, name)
}


class BytecaskStorage(rootPath: String, name: String) extends Storage {
  private val db = new Bytecask(rootPath + "/" + name)

  def get(key: String) = db.get(key.getBytes).map(_.bytes)

  def put(key: String, data: Array[Byte]) {
    db.put(key.getBytes, data)
  }
}
