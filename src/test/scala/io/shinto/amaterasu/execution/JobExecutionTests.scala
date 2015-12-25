package io.shinto.amaterasu.execution

import java.util.concurrent.LinkedBlockingQueue

import io.shinto.amaterasu.dataObjects.ActionData
import io.shinto.amaterasu.dsl.JobParser

import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.curator.test.TestingServer

import org.scalatest.{ Matchers, FlatSpec }

import scala.io.Source

class JobExecutionTests extends FlatSpec with Matchers {

  val retryPolicy = new ExponentialBackoffRetry(1000, 3)
  val server = new TestingServer(2183, true)
  val client = CuratorFrameworkFactory.newClient(server.getConnectString, retryPolicy)
  client.start()

  val jobId = s"job_${System.currentTimeMillis}"
  val yaml = Source.fromURL(getClass.getResource("/simple-maki.yaml")).mkString
  val queue = new LinkedBlockingQueue[ActionData]()
  val job = JobParser.parse(jobId, yaml, queue, client)

  "a job" should "queue the first action when the JobManager.start method is called " in {

    job.start
    queue.peek.name should be ("start")

    // making sure that the status is reflected in zk
    val taskStatus = client.getData.forPath(s"/${jobId}/task-0000000000")
    new String(taskStatus) should be("queued")

  }

  it should "return the start action when calling getNextAction and dequeue it" in {

    job.getNextActionData.name should be ("start")
    queue.size should be (0)

    // making sure that the status is reflected in zk
    val taskStatus = client.getData.forPath(s"/${jobId}/task-0000000000")
    new String(taskStatus) should be("started")

  }

  it should "be marked as complete when the actionComplete method is called" in {

    job.actionComplete("0000000000")

    // making sure that the status is reflected in zk
    val taskStatus = client.getData.forPath(s"/${jobId}/task-0000000000")
    new String(taskStatus) should be("complete")
  }

  "the next step2 job" should "be queued as a result of the compleation" in {

    queue.peek.name should be ("step2")

    // making sure that the status is reflected in zk
    val taskStatus = client.getData.forPath(s"/${jobId}/task-0000000001")
    new String(taskStatus) should be("queued")

  }
}