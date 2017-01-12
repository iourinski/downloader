package test.Utilities

import akka.actor.{Actor, ActorRef, Props}
import akka.event.Logging
import test.Actors.{FtpActor, HttpActor, TimeTracker}

// This is the wrapper for actors, which has to be adapted when new protocols are added
// we can only download over finite number of protocols, here we enumerated them, new protocols should be added here
object protocols extends  Enumeration {
  type PType = Value
  val http, ftp, unknown = Value
}

// the actors for particular protocol are spawned here, in case of extending the set of protocols,
// new actors should be added here
class Worker(listener: ActorRef, timeout: Int = 5000, timeTracking: Boolean = false) extends Actor {
  var time = System.currentTimeMillis()
  //time tracker is just a watcher, that informs if something is taking long, it can not force stop actors
  // it is not activated by default
  var timeTracker = context.actorOf(Props(new TimeTracker(5, 50)))
  val log = Logging(context.system, this)
  var processed = false
  var child: ActorRef = null
  def receive = {
    case UrlToDownload(url, parentDir) =>
      time =  System.currentTimeMillis()
      if (timeTracking)
        timeTracker ! InitTT(url)
      // actual choice of protocol-specific actors
      val dr  = new DownloadRequest(url, parentDir)
      dr.requestType match  {
        case protocols.ftp =>
          child = context.actorOf(Props[FtpActor])
          child ! DownloadRequest(url, parentDir)
        case protocols.http =>
          child = context.actorOf(Props[HttpActor])
          child ! DownloadRequest(url, parentDir)
        case _ =>
          context.stop(timeTracker)
          listener ! ResultMessage("WRONGPROTOCOL", url, parentDir)
      }
    case ResultMessage(txt, url, parentDir) =>
      if(timeTracking)
        timeTracker ! test.Utilities.Finish
      processed = true
      listener ! ResultMessage(txt, url, parentDir)
    // since actors can not be stopped until they process current message, this only informs us,
    // that something took long time
    case CheckRunTime(period, url) =>
      val ctime = System.currentTimeMillis()
      if (child != null && ctime - time > timeout && !processed) {
        listener ! ResultMessage("TIMEOUT", url, "")
      } else {
        if (child != null){
          sender ! TimeTracking(ctime - time, url)
        }
      }
    case _ => log.info("received unknown message")
  }
}
