package test.Actors

import java.io._
import java.net.{MalformedURLException, URL}

import akka.actor.Actor
import akka.event.Logging

import scala.sys.process._
import test.Utilities._

/**
  * Created by dmitri on 03/01/2017.
  */
class HttpActor extends Actor {
    val log = Logging(context.system, this)
    def receive = {
      case DownloadRequest(url, parentDir) =>
        //Thread sleep(10000)
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
