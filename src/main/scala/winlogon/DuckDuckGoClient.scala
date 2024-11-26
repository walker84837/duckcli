import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import io.circe.Json
import io.circe.parser.parse
import io.circe.syntax._

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object DuckDuckGoClient {
  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  def fetchToken(): Future[Option[String]] = {
    val url = "https://duckduckgo.com/duckchat/v1/status"
    val headers = List(
      HttpHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0"),
      HttpHeader("Accept", "*/*"),
      HttpHeader("Referer", "https://duckduckgo.com/"),
      HttpHeader("x-vqd-accept", "1"),
      HttpHeader("Connection", "keep-alive"),
      HttpHeader("Cookie", "dcm=5; ah=it-it; l=wt-wt")
    )

    Http().singleRequest(HttpRequest(uri = url, method = HttpMethods.HEAD, headers = headers))
      .flatMap { response =>
        if (response.status.isSuccess()) {
          response.headers.find(_.name() == "x-vqd-4").map(_.value()).map(Some(_))
        } else {
          println(s"Failed to fetch token. Status code: ${response.status}")
          Future.successful(None)
        }
      }
  }

  def fetchMessages(token: Option[String], prompt: String): Future[Unit] = {
    if (token.isEmpty) {
      println("No token provided.")
      Future.successful(())
    } else {
      val url = "https://duckduckgo.com/duckchat/v1/chat"
      val headers = List(
        HttpHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64; rv:132.0) Gecko/20100101 Firefox/132.0"),
        HttpHeader("Accept", "text/event-stream"),
        HttpHeader("Content-Type", "application/json"),
        HttpHeader("x-vqd-4", token.get),
        HttpHeader("Sec-GPC", "1")
      )

      val body = Json.obj(
        "model" -> "meta-llama/Meta-Llama-3.1-70B-Instruct-Turbo".asJson,
        "messages" -> Json.arr(Json.obj("role" -> "user".asJson, "content" -> prompt.asJson))
      )

      Http().singleRequest(HttpRequest(uri = url, method = HttpMethods.POST, headers = headers, entity = HttpEntity(ContentTypes.`application/json`, body.noSpaces)))
        .flatMap { response =>
          if (response.status.isSuccess()) {
            val fullResponse = Source
              .fromFuture(response.entity.toStrict(Duration(5, scala.concurrent.duration.SECONDS)))
              .mapAsync(1) { chunk =>
                Unmarshal(chunk).to[String]
              }
              .map { chunk =>
                val lines = chunk.split("\n")
                lines.find(_.trim.startsWith("data:")).map(_.trim.drop(5).trim)
              }
              .filter(_.nonEmpty)
              .map { data =>
                parse(data.get).flatMap(_.hcursor.downField("message").as[String])
              }
              .filter(_.nonEmpty)
              .map(_.get.trim.stripPrefix("\"").stripSuffix("\""))
              .reduce(_ + " " + _)
              .runWith(Sink.head)

            fullResponse.onComplete {
              case Success(response) =>
                println(s"Reply: $response")
              case Failure(ex) =>
                println(s"Failed to fetch messages: $ex")
            }
            Future.successful(())
          } else {
            println(s"Request failed with status: ${response.status}")
            Future.successful(())
          }
        }
    }
  }

  def main(args: Array[String]): Unit = {
    val prompt = scala.io.StdIn.readLine("Enter a prompt: ")
    val token = Await.result(fetchToken(), Duration(5, scala.concurrent.duration.SECONDS))
    Await.result(fetchMessages(token, prompt), Duration(10, scala.concurrent.duration.SECONDS))
  }
}
