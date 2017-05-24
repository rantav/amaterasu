package io.shinto.amaterasu.leader.utilities

//import io.shinto.amaterasu.Logging
import io.shinto.amaterasu.common.logging.Logging
import org.apache.log4j.{BasicConfigurator, Level, Logger}
import org.eclipse.jetty.server.{Server, ServerConnector}
import org.eclipse.jetty.server.handler._
import org.eclipse.jetty.servlet.{DefaultServlet, ServletContextHandler, ServletHolder}
import org.eclipse.jetty.toolchain.test.MavenTestingUtils
import org.eclipse.jetty.util.thread.QueuedThreadPool
import org.eclipse.jetty.util.log.StdErrLog
import org.eclipse.jetty.util.resource.Resource
import org.jsoup.Jsoup
import org.jsoup.select.Elements

import scala.collection.JavaConverters._
import scala.io.{BufferedSource, Source}
import scala.text.Document
/**
  * Created by kirupa
  * Implementation of Jetty Web server to server Amaterasu libraries and other distribution files
  */
object HttpServer extends Logging {
  val logger = Logger.getLogger(HttpServer.getClass)
  var server: Server = null

  def start(port: String, serverRoot: String): Unit = {

    /*val threadPool = new QueuedThreadPool(Runtime.getRuntime.availableProcessors() * 16)
    threadPool.setName("Jetty")*/
    BasicConfigurator.configure()
    initLogging()
    server = new Server()
    val connector = new ServerConnector(server)
    connector.setPort(port.toInt)
    server.addConnector(connector)

    val rh0 = new ResourceHandler()
    rh0.setDirectoriesListed(true)
    //rh0.setWelcomeFiles(new String[] { "index.html" });

    rh0.setResourceBase(serverRoot)

    val context0 = new ContextHandler()
    context0.setContextPath("/*")
    //context0.setContextPath("/")
    //val dir0 = MavenTestingUtils.getTestResourceDir("dist")
    //context0.setBaseResource(Resource.newResource(dir0))
    context0.setHandler(rh0)

    val context = new ServletContextHandler(ServletContextHandler.SESSIONS)
    context.setResourceBase(serverRoot)
    context.setContextPath("/")
    context.setErrorHandler(new ErrorHandler())
    context.setInitParameter("dirAllowed", "true")
    context.setInitParameter("pathInfoOnly", "true")
    context.addServlet(new ServletHolder(new DefaultServlet()), "/")

    val contexts = new ContextHandlerCollection()
    contexts.setHandlers(Array(context0, context))

    server.setHandler(contexts)

    server.start()
  }

  def stop() {
    if (server == null) throw new IllegalStateException("Server not started")

    server.stop()
    server = null
  }

  def initLogging(): Unit = {
    System.setProperty("org.eclipse.jetty.util.log.class", classOf[StdErrLog].getName)
    Logger.getLogger("org.eclipse.jetty").setLevel(Level.OFF)
    Logger.getLogger("org.eclipse.jetty.websocket").setLevel(Level.OFF)
  }

  def getFilesInDirectory(amaNode:String,port:String,directory:String): Array[String] ={
    val html:BufferedSource = Source.fromURL("http://"+amaNode+":"+port+"/"+directory)
    val htmlDoc = Jsoup.parse(html.mkString)
    val htmlElement:Elements = htmlDoc.body().select("a")
    val files = htmlElement.asScala
    val fileNames = files.map(url=>url.attr("href")).filter(file=>(!file.contains(".."))).map(name=>name.replace("/","")).toArray
    fileNames
  }
}
