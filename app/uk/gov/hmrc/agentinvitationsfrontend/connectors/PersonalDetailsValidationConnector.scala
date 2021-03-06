/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.agentinvitationsfrontend.connectors

import com.codahale.metrics.MetricRegistry
import com.kenshoo.play.metrics.Metrics
import javax.inject.{Inject, Singleton}
import play.api.http.Status._
import uk.gov.hmrc.agent.kenshoo.monitoring.HttpAPIMonitor
import uk.gov.hmrc.agentinvitationsfrontend.config.AppConfig
import uk.gov.hmrc.domain.Nino
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PersonalDetailsValidationConnector @Inject()(http: HttpClient)(implicit appConfig: AppConfig, metrics: Metrics)
    extends HttpAPIMonitor {

  override val kenshooRegistry: MetricRegistry = metrics.defaultRegistry

  def getPdvResult(
    validationId: String)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[PdvError, Nino]] =
    monitor("ConsumedAPI-Client-Get-PDVResult-GET") {
      val url = s"${appConfig.personalDetailsValidationBaseUrl}/personal-details-validation/$validationId"
      http
        .GET[HttpResponse](url)
        .map { response =>
          response.status match {
            case OK =>
              val result = (response.json \ "validationStatus").as[String]
              if (result == "success") {
                (response.json \ "personalDetails" \ "nino").asOpt[Nino].toRight[PdvError](PdvValidationNoNino)
              } else {
                Left(PdvValidationFailure)
              }

            case NOT_FOUND => Left(PdvValidationNotFound)
          }
        }
    }
}

sealed trait PdvError

case object PdvValidationNotFound extends PdvError
case object PdvValidationNoNino extends PdvError
case object PdvValidationFailure extends PdvError
