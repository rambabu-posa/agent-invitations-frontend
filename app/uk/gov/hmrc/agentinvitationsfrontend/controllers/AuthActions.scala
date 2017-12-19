/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.agentinvitationsfrontend.controllers

import play.api.Configuration
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.agentmtdidentifiers.model.Arn
import uk.gov.hmrc.auth.core.AuthProvider.GovernmentGateway
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.Retrievals.authorisedEnrolments
import uk.gov.hmrc.auth.otac.{Authorised, NoOtacTokenInSession, OtacAuthConnector, OtacFailureThrowable}
import uk.gov.hmrc.http.{HeaderCarrier, SessionKeys}

import scala.concurrent.{ExecutionContext, Future}

trait AuthActions extends AuthorisedFunctions {

  val configuration: Configuration

  def otacAuthConnector: OtacAuthConnector

  private lazy val passcodeRegime: String = configuration.getString("passcodeAuthentication.regime").getOrElse("agent-fi-agent-frontend")
  private lazy val passcodeEnabled: Boolean = configuration.getBoolean("passcodeAuthentication.enabled").getOrElse(true)

  protected def withAuthorisedAsAgent[A](body: Arn => Future[Result])(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    withEnrolledAsAgent {
      case Some(arn) => withVerifiedPasscode(passcodeRegime) {
        body(Arn(arn))
      }
      case None => Future.failed(InsufficientEnrolments("AgentReferenceNumber identifier not found"))
    }

  protected def withAuthorisedAsClient[A](serviceName: String, identifierKey: String)(body: String => Future[Result])(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] =
    withEnrolledAsClient(serviceName, identifierKey) {
      case Some(clientId) => body(clientId)
      case None => Future.failed(InsufficientEnrolments(s"$identifierKey identifier not found"))
    }

  protected def withEnrolledAsAgent[A](body: Option[String] => Future[Result])(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    authorised(
      Enrolment("HMRC-AS-AGENT")
        and AuthProviders(GovernmentGateway))
      .retrieve(authorisedEnrolments) { enrolments =>
        val id = for {
          enrolment <- enrolments.getEnrolment("HMRC-AS-AGENT")
          identifier <- enrolment.getIdentifier("AgentReferenceNumber")
        } yield identifier.value

        body(id)
      }
  }

  protected def withEnrolledAsClient[A](serviceName: String, identifierKey: String)(body: Option[String] => Future[Result])(implicit request: Request[A], hc: HeaderCarrier, ec: ExecutionContext): Future[Result] = {
    authorised(
      Enrolment(serviceName)
        and AuthProviders(GovernmentGateway)
        and ConfidenceLevel.L200
    )
      .retrieve(authorisedEnrolments) { enrolments =>
        val id = for {
          enrolment <- enrolments.getEnrolment(serviceName)
          identifier <- enrolment.getIdentifier(identifierKey)
        } yield identifier.value

        body(id)
      }
  }

  private def withVerifiedPasscode[A, T](serviceName: String)(body: => Future[T])
                                        (implicit request: Request[A], headerCarrier: HeaderCarrier, ec: ExecutionContext): Future[T] = {
    if (passcodeEnabled) {
      request.session.get(SessionKeys.otacToken).fold[Future[T]](
        Future.failed(OtacFailureThrowable(NoOtacTokenInSession))
      ) {
        otacToken =>
          otacAuthConnector.authorise(serviceName, headerCarrier, Option(otacToken)).flatMap {
            case Authorised => body
            case otherResult => Future.failed(OtacFailureThrowable(otherResult))
          }
      }
    } else {
      body
    }
  }
}
