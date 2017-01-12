package test.Utilities

import java.io.File
import scala.util.matching.Regex
/*
Small helper that forms filepaths and actually creates files, the logic about forming filename from url should be here
In this case the files are stored in folders according to their protocols, if file extension is not known
it is replaced by dwl (download). The naming logic is in method makeFileName
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

  def makeFilePath(): String = {
    if (url.parentDir != "")
      url.parentDir + "/" + url.requestType + "/" + makeFileName()
    else
      url.requestType + "/" + makeFileName()
  }

  private def checkDir(dirPath: String): String = {
    val dir = new File(dirPath)
    if (!dir.exists()) {
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
