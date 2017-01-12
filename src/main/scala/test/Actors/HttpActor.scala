package test.Actors

import java.io._
import java.net.{MalformedURLException, URL}

import akka.actor.Actor
import akka.event.Logging

import test.Utilities._
// simple actor that tries to download a URL over http and reports the result
// same as ftp agent it reads and writes to a stream
class HttpActor extends Actor {
  val log = Logging(context.system, this)
  def receive = {
    case DownloadRequest(url, parentDir) =>
      val res  = downloadUrl(DownloadRequest(url, parentDir))
      sender ! ResultMessage(res, url, parentDir)
    case Finish => context.stop(self)
    case _ =>
      log.info("unexpected message")
      sender() ! ResultMessage("UNKNOWN", "", "")
    }

  def downloadUrl(downloadRequest: DownloadRequest): String = {
    val fileCreator = new FileCreator(downloadRequest)
    val file = fileCreator.makeFile()
    try {
      val url = new URL(downloadRequest.url)
      val connection = url.openConnection()
      val outputStream = new BufferedOutputStream(new FileOutputStream(file))
      val inputStream = connection.getInputStream
      var bytesArray  = new Array[Byte](4096)
      var bytesRead = 0
      while (bytesRead  != -1) {
        outputStream.write(bytesArray, 0, bytesRead)
        bytesRead = inputStream.read(bytesArray)
      }
      outputStream.close()
      inputStream.close()
    } catch {
      case e: java.lang.RuntimeException => return "FAIL"
      case e: MalformedURLException => return  "WRONGURL"
      case e: IOException =>  return "FAIL"
    }
    return "OK"
  }
}
