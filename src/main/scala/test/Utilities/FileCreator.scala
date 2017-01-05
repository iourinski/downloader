package test.Utilities

import java.io.File

import scala.util.matching.Regex
/**
  * Created by dmitri on 04/01/2017.
  */
class FileCreator(url: DownloadRequest) {
  def makeFile(): File = {
    if (url.parentDir != "") {
      checkDir(url.parentDir)
      checkDir(url.parentDir + "/" + url.requestType)
    } else {
      checkDir(url.requestType.toString)
    }

    val filePath = makeFilePath()
    val file = new File(filePath)
    if (!file.exists()) {
      file.createNewFile()
    }
    file
  }

  def delFile() = {
    val filePath = makeFilePath()
    val file = new File(filePath)
    if (file.exists()) {
      file.delete()
    }
  }

  private def makeFilePath(): String = {
    if (url.parentDir != "")
      url.parentDir + "/" + url.requestType + "/" + makeFileName()
    else
      url.requestType + "/" + makeFileName()
  }

  private def checkDir(dirPath: String): String = {
    val dir = new File(dirPath)
    if (! dir.exists()) {
      dir.mkdir()
    }
    dirPath
  }

  private def makeFileName(): String = {
    val extPat = new Regex("\\.[a-zA-Z0-9]+$")
    val protocolPat = new Regex("^[a-zA-Z0-9]+[\\/\\:]+")
    val ext = extPat.findFirstIn(url.url) match {
      case Some(s) => s
      case None => ".dwl"
    }
    var fileName = extPat.replaceFirstIn(url.url,"")
    fileName = protocolPat.replaceFirstIn(fileName,"").replace("/","_").replace(".","_") + ext
    fileName
  }
}
