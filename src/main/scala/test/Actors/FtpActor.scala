package test.Actors

/**
  * Created by dmitri on 03/01/2017.
  */

import java.io.{BufferedOutputStream, FileOutputStream, IOException}

import akka.actor._
import org.apache.commons.net.ftp.{FTP, FTPClient}

import test.Utilities._

import scala.util.matching.Regex

class FtpActor extends Actor {
  def receive = {
    case DownloadRequest(url, parentDir) =>
      val res = downloadUrl(url, parentDir)
      sender() ! ResultMessage(res, url, parentDir)
    case Finish => context.stop(self)
    case _ =>
      sender() ! ResultMessage("UNKNOWN", "", "")
  }
  def downloadUrl(url: String, parentDir: String): String = {
    val (server, filepath) = getServerAddress(url)
    val ftpClient = new FTPClient()
    val fileCreator = new FileCreator(DownloadRequest(url, parentDir))

    try {

      ftpClient.connect(server)
      ftpClient.login("anonymous","")
      ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

      // APPROACH #1: using retrieveFile(String, OutputStream)

      val downloadFile = fileCreator.makeFile()
      val outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile))
      val inputStream = ftpClient.retrieveFileStream(filepath)
      var bytesArray  = new Array[Byte](4096)
      var bytesRead = 0
      //println(inputStream.read(bytesArray))
      while (bytesRead  != -1) {

        outputStream.write(bytesArray, 0, bytesRead)
        bytesRead = inputStream.read(bytesArray)
      }

      val success = ftpClient.completePendingCommand()
      outputStream.close()
      inputStream.close()
      if(success){
        return "OK"
      } else {
        return "FAIL"
      }

    } catch {
      case ex: IOException => return "FAIL"
    } finally {
      try {
        if (ftpClient.isConnected()) {
          ftpClient.logout();
          ftpClient.disconnect();
        }
      } catch {
        case ex: IOException => return "FAIL"
      }
    }
  }

  private def getServerAddress(url: String):(String, String) = {
    val pattern = new Regex("^ftp:\\/\\/[^\\/]+")
    val server = pattern.findFirstIn(url) match {
      case Some(s) => s
      case None => ""
    }
    val file = url.replace(server,"")
    return (server.replace("ftp://",""), file)
  }
}
