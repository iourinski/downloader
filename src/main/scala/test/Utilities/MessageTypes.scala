package test.Utilities

import scala.util.matching.Regex

// this is a list of `allowed` types of messages
case class CheckRunTime(period: Int, url: String)
// here we also checking whether the url is appropriate for processing
case class DownloadRequest (url: String, parentDir: String) {
  val requestType = checkProtocol(url)
  def checkProtocol(url: String): protocols.PType = {
    val protocolRegex = new Regex("^\\w+")
    val protocolName = protocolRegex.findFirstIn(url) match {
      case Some(s) => s
      case None => null
    }
    protocolName  match {
      case "ftp" => return protocols.ftp
      case "http" => return protocols.http
      case _  => return protocols.unknown
    }
  }
}

//more messages with requests
case class UrlToDownload(url: String, parentDir: String)
case class DownloadURLS(urls: List[String], parentDir: String)
case class Finish()
case class InitTT(url: String)

// reporting messages
case class ResultMessage( result: String, url: String, parentDir: String)
case class TimeTracking(time: Long, url: String)