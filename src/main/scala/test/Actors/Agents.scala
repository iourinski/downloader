package test.Actors

import akka.actor.{Props, _}
import akka.event.Logging
import akka.routing.RoundRobinRouter

import test.Utilities._
// definition of actors in the system that are protocol-independent

// main actor, that routes and accumulates queries
class Master (nrOfWorkers: Int, listener: ActorRef, timeout: Int = 5000, timeTracking: Boolean = false) extends Actor {
  val workerRouter = context.actorOf(
    Props(new Worker(listener, timeout, timeTracking)).withRouter(RoundRobinRouter(nrOfWorkers)), name = "workerRouter"
  )
  var time = System.currentTimeMillis()
  val log = Logging(context.system, this)
  def processRequests(urls: List[String], parentDir: String) = {
    for(url <- urls) {
      listener ! UrlToDownload(url, parentDir)
      workerRouter ! UrlToDownload(url, parentDir)
    }
  }
  def receive = {
    case DownloadURLS(urls, parentDir) => processRequests(urls, parentDir)
    case _ => log.error("Unknown message should not be here")
  }
}

// actor for communicating with outside world and stopping the whole system
class Listener extends Actor {
  val log = Logging(context.system, this)
  var answers = scala.collection.mutable.Map[String,Boolean]()
  def receive = {
    case UrlToDownload(url, parentDir) =>
      answers.+=(url -> false)
    case ResultMessage(result, url, parentDir) =>
      val fileCreator = new FileCreator(DownloadRequest(url, parentDir))
      result match {
        case "OK" =>
          if (answers.contains(url)){
            log.info( url + " completed download")
            answers(url) = true
          }
        case "TIMEOUT" =>
          log.info(url + " takes long time")
        case "FAIL" =>
          log.info(url + " failed to download")
          answers(url) = true
          fileCreator.delFile()
        case "WRONGPROTOCOL" =>
          log.warning(url + " can not be downloaded")
          answers(url) = true
          fileCreator.delFile()
        case "WRONGURL" =>
          log.warning(url + " is malformed")
          answers(url) = true
          fileCreator.delFile()
        case "UNKNOWN" =>
          log.warning("meaningless message")
      }
      // check if anything is left in the `queue` if everything is processed, finish
      if (answers.filter(!_._2).isEmpty){
        context.system.shutdown()
      }
    // if something wrong comes, we probably shut everything down
    case  _ =>
      log.error("meaningless message, shutting down")
      context.system.shutdown()
  }
}

// simple actor that only keeps track of time and reports if some actor took too long
class TimeTracker(periodicity: Double, idlingPeriod: Int) extends Actor {
  def receive = {
    case InitTT(url) => sender ! CheckRunTime(idlingPeriod, url)
    case Finish => context.stop(self)
    case TimeTracking(time: Long, url: String) =>
      if (time > idlingPeriod * 1000) {
        context.system.shutdown()
      }
      Thread sleep((periodicity * 1000).toInt)
      sender ! CheckRunTime(idlingPeriod, url)
  }
}