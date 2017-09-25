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

package controllers

import javax.inject.Inject

import actions.SessionAction
import cats.syntax.either._
import config.{PageConfig, ServiceConfig}
import org.scalactic.TripleEquals._
import play.api.Logger
import play.api.data.FormError
import play.api.i18n.MessagesApi
import play.api.mvc.Controller
import questionnaire._
import services.SessionService

import scala.concurrent.{ExecutionContext, Future}

object QuestionnaireController {
  val startTitle              = "Find out if your business needs to publish reports"
  val startButtonId           = "start-button"
  val exemptTitle             = "Your business does not need to publish reports"
  val mustReportTitle         = "Your business must publish reports"
  val companyReasonsListId    = "company-reasons"
  val subsidiaryReasonsListId = "subsidiary-reasons"
}

class QuestionnaireController @Inject()(
  summarizer: Summarizer,
  val pageConfig: PageConfig,
  val serviceConfig: ServiceConfig,
  sessionAction: SessionAction,
  sessionService: SessionService
)
  (implicit messages: MessagesApi, ec: ExecutionContext) extends Controller with PageHelper {

  import QuestionnaireController._
  import views.html.{questionnaire => pages}

  private val answersKey = "answers"

  /**
    * The default behaviour of the session action is to kick the user to a timeout page if it
    * detects that the application session (identified by the session id on the Play session) has
    * timed out. For the questionnaire we're happy that if their application session has timed out
    * we simply create a new one for them and carry on.
    */
  private val withSession: SessionAction = sessionAction.refreshOnTimeout

  //noinspection TypeAnnotation
  def start = withSession.async { implicit request =>
    val externalRouter = implicitly[ExternalRouter]
    sessionService.clear(request.sessionId, answersKey).map { _ =>
      Ok(page(startTitle)(home, views.html.questionnaire.start(externalRouter)))
    }
  }

  //noinspection TypeAnnotation
  def answer(question: Question) = withSession.async(parse.urlFormEncoded) { implicit request =>
    val formAnswer = question.bindAnswer(request.body)

    formAnswer match {
      case Left(error)   => Future.successful(BadRequest(page(messages(question.textKey))(home, pages.question(question, Some(error)))))
      case Right(answer) =>
        val checkAnswer = sessionService.get[Seq[Answer]](request.sessionId, answersKey).map(_.getOrElse(Seq())).map { currentAnswers =>
          // If the user has gone back and answered an earlier question then drop later answers
          val adjustedAnswers = currentAnswers.takeWhile(_.questionId != answer.questionId)
          for {
            expectedAnswer <- DecisionTree.checkAnswers(adjustedAnswers)
            updatedAnswers <- checkQuestionMatches(adjustedAnswers, answer, expectedAnswer)
          } yield updatedAnswers
        }

        checkAnswer.flatMap {
          case Left(error) =>
            Logger.warn(error)
            Future.successful(Unit)

          case Right(updatedAnswers) =>
            sessionService.put(request.sessionId, answersKey, updatedAnswers)
        }.map(_ => Redirect(routes.QuestionnaireController.nextQuestion()))
    }
  }

  private def checkQuestionMatches(currentAnswers: Seq[Answer], formAnswer: Answer, expectedAnswer: DecisionTree): Either[String, Seq[Answer]] = {
    expectedAnswer match {
      case YesNoNode(q, _, _) if q.id === formAnswer.questionId   => Right(currentAnswers :+ formAnswer)
      case YearNode(q, _, _, _) if q.id === formAnswer.questionId => Right(currentAnswers :+ formAnswer)
      case _                                                      => Left(s"$formAnswer does not match the expected answer")
    }
  }

  //noinspection TypeAnnotation
  def nextQuestion = withSession.async { implicit request =>
    sessionService.get[Seq[Answer]](request.sessionId, answersKey).map(_.getOrElse(Seq())).flatMap { answers =>

      val back = breadcrumbs(Breadcrumb(routes.QuestionnaireController.back(), "Back"))

      DecisionTree.checkAnswers(answers) match {
        case Left(error) =>
          Logger.warn(error)
          sessionService.clear(request.sessionId, answersKey).map(_ => Redirect(routes.QuestionnaireController.nextQuestion()))

        case Right(YesNoNode(q, _, _))                => Future.successful(Ok(page(messages(q.textKey))(back, pages.question(q, None))))
        case Right(YearNode(q, _, _, _))              => Future.successful(Ok(page(messages(q.textKey))(back, pages.question(q, None))))
        case Right(DecisionNode(NotACompany(reason))) => Future.successful(Ok(page(exemptTitle)(back, pages.notACompany(reason))))
        case Right(DecisionNode(Exempt(reason)))      => Future.successful(Ok(page(exemptTitle)(back, pages.exempt(reason))))
        case Right(DecisionNode(Required))            => Future.successful(Ok(page(mustReportTitle)(back, pages.required(summarizer.summarize(answers)))))
      }
    }
  }

  //noinspection TypeAnnotation
  def back = withSession.async { implicit request =>
    sessionService.getOrElse[Seq[Answer]](request.sessionId, answersKey, Seq()).flatMap { answers =>
      answers.dropRight(1) match {
        case Nil =>
          Future.successful(Redirect(routes.QuestionnaireController.start()))

        case updatedAnswers =>
          sessionService.put(request.sessionId, answersKey, updatedAnswers).map { _ =>
            Redirect(routes.QuestionnaireController.nextQuestion())
          }
      }
    }
  }

  /**
    * Is the question to ask next the same as the one we just asked? If so, this indicates the form
    * was posted without an answer (i.e. the user did not select a choice), in which case we should
    * show an error message when we re-display the form.
    */
  private def questionError(q: Question, questionKey: Option[String]): Option[FormError] = questionKey.flatMap {
    case k if k === q.fieldKey => Some(FormError(k, "error.needchoicetocontinue"))
    case _                     => None
  }
}
