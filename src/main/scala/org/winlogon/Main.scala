package org.winlogon

import sttp.client3._
import sttp.client3.asynchttpclient.future.AsyncHttpClientFutureBackend
import sttp.client3.circe._

import io.circe.Json
import io.circe.parser._
import io.circe.syntax._

import org.jline.reader.LineReaderBuilder
import org.jline.reader.{LineReader, Completer, ParsedLine}
import org.jline.terminal.{Terminal, TerminalBuilder}
import org.slf4j.LoggerFactory

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.{Logger => LogbackLogger, Level}

import com.typesafe.scalalogging.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

object Main {
  implicit val backend: SttpBackend[Future, Any] = AsyncHttpClientFutureBackend()

  // Initialize logger
  val rootLogger = LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).asInstanceOf[LogbackLogger]
  rootLogger.setLevel(Level.INFO)
  val logger = Logger("DuckCLI")


  // List of models
  val models = Map(
    "o3-mini" -> "o3-mini",
    "gpt-4o-mini" -> "gpt-4o-mini",
    "claude-3" -> "claude-3-haiku-20240307",
    "llama" -> "meta-llama/Llama-3.3-70B-Instruct-Turbo",
    "mixtral" -> "mistralai/Mixtral-8x7B-Instruct-v0.1"
  )

  def fetchToken(): Future[Option[String]] = {
    val url = "https://duckduckgo.com/duckchat/v1/status"
    val headers = Map(
      "User-Agent" -> "Mozilla/5.0 (X11; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0",
      "Accept" -> "*/*",
      "Referer" -> "https://duckduckgo.com/",
      "Origin" -> "https://duckduckgo.com",
      "x-vqd-accept" -> "1",
      "Connection" -> "keep-alive",
      "Cookie" -> "dcm=5; ah=it-it; l=wt-wt"
    )

    logger.info("Fetching token to start conversation...")
    basicRequest
      .get(uri"$url")
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

  def fetchMessages(token: Option[String], prompt: String, model: String): Future[Unit] = {
    logger.info("Checking whether token is valid...")
    token match {
      case Some(t) => sendMessage(t, prompt, model).flatMap(processStream)
      case None =>
        logger.error("No token provided.")
        Future.successful(())
    }
  }

  // Sends the prompt and returns a stream of words in a String
  def sendMessage(token: String, prompt: String, model: String): Future[String] = {
    val url = "https://duckduckgo.com/duckchat/v1/chat"
    val headers = Map(
      "User-Agent" -> "Mozilla/5.0 (X11; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0",
      "Accept" -> "text/event-stream",
      "Content-Type" -> "application/json",
      "x-vqd-4" -> token,
      "Sec-GPC" -> "1"
    )

    val body = Json.obj(
      "model" -> model.asJson,
      "messages" -> Json.arr(Json.obj("role" -> "user".asJson, "content" -> prompt.asJson)),
      "stream" -> true.asJson
    )

    logger.info(s"Sending prompt to $model")
    basicRequest
      .post(uri"$url")
      .headers(headers)
      .body(body.noSpaces)
      .response(asString)
      .send(backend)
      .map { response =>
        if (response.code.isSuccess) response.body.getOrElse("")
        else {
          logger.error(s"Request failed with status: ${response.code}")
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

  def completer: Completer = new ConcreteModelCompleter(models)

  def main(args: Array[String]): Unit = {
    val terminal: Terminal = TerminalBuilder.builder().build()

    val lineReader: LineReader = LineReaderBuilder
      .builder
      .completer(completer)
      .build()

    println("Models:")
    models.keys.foreach(key => println(s"- $key"))
    val modelInput = lineReader.readLine("Enter model (default is llama): ")
    val selectedModel = if (modelInput.isEmpty) "llama" else modelInput

    val prompt = lineReader.readLine("Enter prompt: ")

    val result = fetchToken().flatMap {
      case Some(token) => fetchMessages(Some(token), prompt, selectedModel)
      case None =>
        println("Failed to fetch token.")
        Future.successful(())
    }
    Await.result(result, Duration.Inf)
  }
}
