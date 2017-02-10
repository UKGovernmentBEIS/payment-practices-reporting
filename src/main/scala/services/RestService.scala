/*
 * Copyright (C) 2017  Department for Business, Energy and Industrial Strategy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package services

import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSRequest, WSResponse}
import services.RestService.{JsonParseException, RestFailure}

import scala.concurrent.{ExecutionContext, Future}

trait RestService {

  def ws: WSClient

  implicit def ec: ExecutionContext

  def getOpt[A: Reads](url: String, auth:String): Future[Option[A]] = {
    val request: WSRequest = ws.url(url).withHeaders(("Authorization", auth))
    request.get.map { response =>
      response.status match {
        case 200 => response.json.validate[A] match {
          case JsSuccess(a, _) => Some(a)
          case JsError(errs) => throw JsonParseException("GET", request, response, errs)
        }
        case 404 => None
        case _ => throw RestFailure("GET", request, response)
      }
    }
  }

  def get[A: Reads](url: String, auth:String): Future[A] = {
    val request: WSRequest = ws.url(url).withHeaders(("Authorization", auth))
    request.get.map { response =>
      response.status match {
        case 200 => response.json.validate[A] match {
          case JsSuccess(as, _) => as
          case JsError(errs) => throw JsonParseException("GET", request, response, errs)
        }
        case _ => throw RestFailure("GET", request, response)
      }
    }
  }

  def getMany[A: Reads](url: String): Future[Seq[A]] = {
    val request: WSRequest = ws.url(url)
    request.get.map { response =>
      response.status match {
        case 200 => response.json.validate[Seq[A]] match {
          case JsSuccess(as, _) => as
          case JsError(errs) => throw JsonParseException("GET", request, response, errs)
        }
        case _ => throw RestFailure("GET", request, response)
      }
    }
  }

  def post[A: Writes](url: String, body: A): Future[Unit] = {
    val request = ws.url(url)
    request.post(Json.toJson(body)).map(_ => ())
  }

  def put[A: Writes](url: String, body: A): Future[Unit] = {
    val request = ws.url(url)
    request.put(Json.toJson(body)).map(_ => ())
  }

  def delete(url: String): Future[Unit] = {
    val request = ws.url(url)
    request.delete().map(_ => ())
  }

  def postWithResult[A: Reads, B: Writes](url: String, body: B): Future[Option[A]] = {
    val request:WSRequest = ws.url(url)
    request.post(Json.toJson(body)).map { response =>
      response.status match {
        case 200 => response.json.validate[A] match {
          case JsSuccess(a, _) =>  Some(a)
          case JsError(errs) => throw JsonParseException("POST", request, response, errs)
        }
        case 404 => None
        case _ => throw RestFailure("POST", request, response)
      }
    }
  }
}

object RestService {

  case class JsonParseException(method: String, request: WSRequest, response: WSResponse, errs: Seq[(JsPath, Seq[ValidationError])]) extends Exception

  case class RestFailure(method: String, request: WSRequest, response: WSResponse) extends Exception {
    val status = response.status
  }

}
