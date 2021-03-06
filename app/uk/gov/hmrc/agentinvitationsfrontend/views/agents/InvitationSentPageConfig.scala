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

package uk.gov.hmrc.agentinvitationsfrontend.views.agents
import org.joda.time.LocalDate
import play.api.mvc.Call
import uk.gov.hmrc.agentinvitationsfrontend.config.ExternalUrls
import uk.gov.hmrc.agentinvitationsfrontend.controllers.routes

case class InvitationSentPageConfig(
  relativeInvitationUrl: String,
  continueUrlOpt: Option[String],
  hasContinueUrl: Boolean,
  clientType: String,
  expiryDate: LocalDate,
  agencyEmail: String,
  services: Set[String],
  serviceType: String = "personal")(implicit externalUrls: ExternalUrls) {

  val continueUrl: String = continueUrlOpt match {
    case Some(url) => url
    case None      => externalUrls.agentServicesAccountUrl
  }

  val trackUrl: Call = routes.AgentsRequestTrackingController.showTrackRequests()

  val clientTypeUrl: Call = routes.AgentInvitationJourneyController.showClientType()
}
