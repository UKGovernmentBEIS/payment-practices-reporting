package controllers

import javax.inject.Inject

import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import play.api.libs.json.{Json, Reads, Writes}
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import play.api.mvc.Call
import play.api.test.Helpers

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

trait ResultDecoder[A] {
  def apply(result: WSResponse, baseUrl: String): Future[A]
}

object ResultDecoder {
  implicit val docDecoder: ResultDecoder[Document] = new ResultDecoder[Document] {
    override def apply(result: WSResponse, baseUrl: String): Future[Document] = Future.successful(Parser.parse(result.body, baseUrl))
  }
}

class WebClient @Inject()(ws: WSClient) {
  val baseUrl =
    s"http://localhost:${Helpers.testServerPort}"

  implicit class WSClientOps(ws: WSClient) {
    def url(call: Call)(implicit ec: ExecutionContext): WSRequest = {
      ws.url(baseUrl + call.url)
    }
  }

  implicit class ResultDecoderOps(result: WSResponse) {
    def decodeAs[A](baseUrl: String)(implicit decoder: ResultDecoder[A]): Future[A]
    = decoder(result, baseUrl)
  }

  def get[A: ResultDecoder](call: Call)(implicit ec: ExecutionContext): Future[A] =
    ws.url(call)
      .get
      .flatMap(_.decodeAs[A](baseUrl))

  def getUrl[A: ResultDecoder](url: String)(implicit ec: ExecutionContext): Future[A] =
    ws.url(baseUrl + url)
      .get
      .flatMap(_.decodeAs[A](baseUrl))

  def post[A: Writes, B: Reads : ResultDecoder](call: Call)(body: A)(implicit ec: ExecutionContext): Future[B] =
    ws.url(call)
      .post(Json.toJson(body))
      .flatMap(_.decodeAs[B](baseUrl))

  def postForm[B: ResultDecoder](url: String)(body: String)(implicit ec: ExecutionContext): Future[B] =
    ws.url(baseUrl + url)
      .withHeaders("Content-Type" -> "application/x-www-form-urlencoded; charset=utf-8")
      .post(body)
      .flatMap(_.decodeAs[B](baseUrl))

  def put[A: Writes, B: Reads : ResultDecoder](call: Call)(body: A)(implicit ec: ExecutionContext): Future[B] =
    ws.url(call)
      .put(Json.toJson(body))
      .flatMap(_.decodeAs[B](baseUrl))

  def delete[A: Reads : ResultDecoder](call: Call)(implicit ec: ExecutionContext): Future[A] =
    ws.url(call)
      .delete
      .flatMap(_.decodeAs[A](baseUrl))
}