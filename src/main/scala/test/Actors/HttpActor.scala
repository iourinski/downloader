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
      var res =  ResultMessage("OK", downloadRequest.url, downloadRequest.parentDir)
      val file = fileCreator.makeFile()
      try {
        val url = new URL(downloadRequest.url)
        url #> file !!
      } catch {
        case e: MalformedURLException =>  "WRONGURL"
        case e: IOException =>  "FAIL"
      } finally {
        return "OK"
      }
    }
}
