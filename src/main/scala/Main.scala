package org.winlogon

import sttp.client3._
import sttp.client3.circe._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.circe.Json
import io.circe.parser._
import io.circe.syntax._

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

object Main {
  implicit val backend: SttpBackend[Future, Any] = AsyncHttpClientFutureBackend()

  // Fetch the token
  def fetchToken(): Future[Option[String]] = {
    val url = "https://duckduckgo.com/duckchat/v1/status"
    val headers = Map(
      "User-Agent" -> "Mozilla/5.0 (X11; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0",
      "Accept" -> "*/*",
      "Referer" -> "https://duckduckgo.com/",
      "x-vqd-accept" -> "1",
      "Connection" -> "keep-alive",
      "Cookie" -> "dcm=5; ah=it-it; l=wt-wt"
    )

    basicRequest
      .head(uri"$url")
      .headers(headers)
      .send(backend)
      .map { response =>
        if (response.code.isSuccess) {
          response.headers.find(_.name == "x-vqd-4").map(_.value)
        } else {
          println(s"Failed to fetch token. Status code: ${response.code}")
          None
        }
      }
  }

  // Fetch messages, split into 3 functions
  def fetchMessages(token: Option[String], prompt: String): Future[Unit] = {
    token match {
      case Some(t) => sendMessage(t, prompt).flatMap(processStream)
      case None =>
        println("No token provided.")
        Future.successful(())
    }
  }

  // Sends the prompt and returns a stream of words in a String
  def sendMessage(token: String, prompt: String): Future[String] = {
    val url = "https://duckduckgo.com/duckchat/v1/chat"
    val headers = Map(
      "User-Agent" -> "Mozilla/5.0 (X11; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0",
      "Accept" -> "text/event-stream",
      "Content-Type" -> "application/json",
      "x-vqd-4" -> token,
      "Sec-GPC" -> "1"
    )

    val body = Json.obj(
      "model" -> "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo".asJson,
      "messages" -> Json.arr(Json.obj("role" -> "user".asJson, "content" -> prompt.asJson))
    )

    basicRequest
      .post(uri"$url")
      .headers(headers)
      .body(body.noSpaces)
      .response(asString)
      .send(backend)
      .map { response =>
        if (response.code.isSuccess) response.body.getOrElse("")
        else {
          println(s"Request failed with status: ${response.code}")
          ""
        }
      }
  }

  // Clean the raw string data
  def processStream(rawStream: String): Future[Unit] = Future {
    val cleanedData = rawStream.split("\n")
      .collect {
        case line if line.trim.startsWith("data:") => line.trim.drop(5).trim
      }
      .flatMap(data => parse(data).flatMap(_.hcursor.downField("message").as[String]).toOption)
      .mkString("")
    println("Response:")
    println(s"$cleanedData")
  }

  def main(args: Array[String]): Unit = {
    val prompt = scala.io.StdIn.readLine("Enter a prompt: ")
    val result = fetchToken().flatMap {
      case Some(token) => fetchMessages(Some(token), prompt)
      case None =>
        println("Failed to fetch token.")
        Future.successful(())
    }
    Await.result(result, Duration.Inf)
  }
}
