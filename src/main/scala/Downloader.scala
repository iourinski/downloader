/*
  * Created by dmitri on 03/01/2017.
  */
import java.io.File

import akka.actor._
import com.typesafe.config.ConfigFactory
import test.Utilities.{DownloadURLS, Listener, Master}

import scala.collection.mutable.ListBuffer
import scala.io.Source
import scala.util.matching.Regex

object Downloader extends App {
  override def main(args: Array[String]): Unit = {

    // read simple config from resources/application.conf
    val config = ConfigFactory.load()
    val numWorkers = config.getInt("master.numWorkers")
    val timeoutLength = config.getInt("master.timeout")
    val parentDir = config.getString("master.parentDir")
    val periodicity = config.getDouble("master.periodicity")
    val idling = config.getInt("master.idling")

    var inputFilePath = ""
    try {
      inputFilePath = args(0)
    } catch {
      case e: IndexOutOfBoundsException => println("You need to pass a file with urls in command line")
    }
    if (inputFilePath != "") {
      val system = ActorSystem("DownloadSystem")
      // create the result listener, which will print the result and shutdown the system
      val listener = system.actorOf(Props(new Listener(0)))
      val master = system.actorOf(Props(new Master(numWorkers, listener)), name = "master")
      val file  = new File(inputFilePath)

      if (file.exists()){
        val comment = new Regex("^#|^\\s*$")
        val urls = ListBuffer[String]()

        for (line <- Source.fromFile(file).getLines()){
          val commentCheck = comment.findFirstIn(line) match {
            case Some(s) => true
            case None => false
          }
          if (! commentCheck){
            urls.+=(line)
          }
        }
        if (urls.length > 0)
          master ! DownloadURLS(urls.toList,parentDir)
      } else {
        println("The file with urls list doesn't exist")
      }
    }
  }
}
