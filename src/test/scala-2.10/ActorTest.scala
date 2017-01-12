import java.io.File

import akka.testkit.TestActorRef
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}

import test.Utilities._
import test.Actors._

import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

// simple test of actors' syncronous behaviour, fairly standard and self-explanatory
class WorkerTests() extends TestKit(ActorSystem("DownloadSystem")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
  // we need to save results somewhere
  val downloadDir = "test_downl_dir"
  // this is a standard recursive delete procedure (probably not written by me, but pretty handy, so we use it)
  def deleteRecursively(file: File): Unit = {
    if (file.isDirectory)
      file.listFiles.foreach(deleteRecursively)
    if (file.exists && !file.delete)
      throw new Exception(s"Unable to delete ${file.getAbsolutePath}")
  }
  // some actors create and delete files, we need to see the results
  def checkFileExistence(file: String, parentDir: String): Boolean = {
    val fileCreator = new FileCreator(DownloadRequest(file, parentDir))
    val savePath = fileCreator.makeFilePath()
    val newFile = new File(savePath)
    newFile.exists()
  }

  override def afterAll {
    // since we tested download we created some junk, which we are deleting now
    val tmpDir = new File(downloadDir)
    deleteRecursively(tmpDir)
    TestKit.shutdownActorSystem(system)
  }

  //On success ftp actor creates a file, on failure it does not clean after itself, but delegates it to listener
  val ftpActor = TestActorRef[FtpActor]
  val ftpOkFile = "ftp://anonymous@ftp.math.ucla.edu/pub/camreport/cam10-24.pdf"
  val badCredFile = "ftp://wrongUser:wrongPw@ftp.math.ucla.edu/pub/camreport/cam10-24.pdf"

  "An Ftp actor" must {
    s"""download $ftpOkFile""" in {
      ftpActor !  DownloadRequest(ftpOkFile, downloadDir)
      expectMsg(ResultMessage("OK",ftpOkFile, downloadDir))
      assert(checkFileExistence(ftpOkFile, downloadDir) == true)
    }
    s""" fail to download $badCredFile""" in {
      ftpActor ! DownloadRequest(badCredFile, downloadDir)
      expectMsg(ResultMessage("FAIL", badCredFile, downloadDir))
    }
    s""" not understand any other messages""" in {
      ftpActor ! TimeTracking
      expectMsg(ResultMessage("UNKNOWN", "", ""))
    }
  }

  //On success http actor creates a file, on failure it does not clean after itself, but delegates this to listener
  val httpActor = TestActorRef[HttpActor]
  val okFile = "http://www.the-village.ru/village/people"
  val nonExistentFile  = "http://non-existent.ftp/file"

  "An http actor" must {
    s"""download $okFile""" in {
      httpActor ! DownloadRequest(okFile, downloadDir)
      expectMsg(ResultMessage("OK", okFile, downloadDir))
      assert(checkFileExistence(okFile, downloadDir) == true)
    }
    s"""fail to download non-existent $nonExistentFile""" in {
      httpActor ! DownloadRequest(nonExistentFile, downloadDir)
      expectMsg(ResultMessage("FAIL",nonExistentFile, downloadDir))
    }
    s"""be confused about other requests""" in {
      httpActor ! TimeTracking
      expectMsg(ResultMessage("UNKNOWN","",""))
    }
  }

  //Listener logs the results and takes notice of what is already processed (both success and fail)
  val listener = TestActorRef[Listener]
  // we need to enqueue things first
  "A listener " must {
    s"""enqueue $okFile and $nonExistentFile""" in {
      listener ! UrlToDownload(okFile, downloadDir)
      listener ! UrlToDownload(nonExistentFile, downloadDir)
      assert(
        listener.underlyingActor.answers(okFile) == false && listener.underlyingActor.answers(nonExistentFile) == false
      )
    }
    s"""log success for $okFile and take notice""" in {
      listener ! ResultMessage("OK",okFile, downloadDir)
      assert(listener.underlyingActor.answers(okFile))
    }
    s"""clear out the file for unsuccessful $nonExistentFile""" in {
      listener ! ResultMessage("FAIL",nonExistentFile, downloadDir)
      assert(listener.underlyingActor.answers(nonExistentFile) && !checkFileExistence(nonExistentFile, downloadDir) )
    }
  }
}
