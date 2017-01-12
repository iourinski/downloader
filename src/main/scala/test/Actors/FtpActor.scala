package test.Actors

import java.io.{BufferedOutputStream, FileNotFoundException, FileOutputStream, IOException}
import scala.util.matching.Regex
import org.apache.commons.net.ftp.{FTP, FTPClient}

import akka.actor._

import test.Utilities._

// simple actor that downloads files over ftp
// there are only three possible exceptions-- in this case we just send message that the download failed
class FtpActor extends Actor {
  def receive = {
    case DownloadRequest(url, parentDir) =>
      val res = downloadUrl(url, parentDir)
      sender() ! ResultMessage(res, url, parentDir)
    case Finish => context.stop(self)
    case _ =>
      sender() ! ResultMessage("UNKNOWN", "", "")
  }

  private def downloadUrl(url: String, parentDir: String): String = {
    val (server, filepath, login, passw) = getServerAddress(url)
    val ftpClient = new FTPClient()
    val fileCreator = new FileCreator(DownloadRequest(url, parentDir))
    try {
      ftpClient.connect(server)
      ftpClient.login(login,passw)
      ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
      val downloadFile = fileCreator.makeFile()
      val outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile))
      val inputStream = ftpClient.retrieveFileStream(filepath)
      var bytesArray  = new Array[Byte](4096)
      var bytesRead = 0
      while (bytesRead  != -1) {
        outputStream.write(bytesArray, 0, bytesRead)
        try {
          bytesRead = inputStream.read(bytesArray)
        } catch {
          case ex: NullPointerException => return "FAIL"
        }
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
      case ex: FileNotFoundException => return "FAIL"
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

  // lets try to get login, password, server and filepath from address
  private def getServerAddress(url: String):(String, String, String, String) = {
    val pattern = new Regex("^ftp:\\/\\/[^\\/]+")
    var server = pattern.findFirstIn(url) match {
      case Some(s) => s
      case None => ""
    }
    val file = url.replace(server,"")
    var login, password = ""
    val bits = server.split("@")
    if (bits.length == 2) {
      server = bits(1)
      val credentials = bits(0).replace("ftp://","").split(":")
      if (credentials.length == 2) {
        login = credentials(0)
        password = credentials(1)
      } else {
        login = credentials(0)
      }
    } else {
      login = "anonymous"
    }
    return (server.replace("ftp://",""), file, login, password)
  }
}
