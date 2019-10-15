/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.Logger
import play.api.i18n.Messages
import play.api.mvc.Call
import uk.gov.hmrc.agentinvitationsfrontend.controllers.{FeatureFlags, routes}
import uk.gov.hmrc.agentinvitationsfrontend.journeys.AgentInvitationJourneyModel.Basket
import uk.gov.hmrc.agentinvitationsfrontend.models.PersonalInvitationsBasket
import uk.gov.hmrc.agentinvitationsfrontend.models.Services._

case class PersonalSelectServicePageConfig(
  basket: Basket,
  featureFlags: FeatureFlags,
  services: Set[String],
  backLink: String,
  reviewAuthsCall: Call)(implicit messages: Messages)
    extends SelectServicePageConfig {

  def submitCall: Call =
    if (showMultiSelect) {
      routes.AgentInvitationJourneyController.submitPersonalSelectService()
    } else {
      remainingService match {
        case HMRCMTDIT  => routes.AgentInvitationJourneyController.submitPersonalSelectItsa()
        case HMRCMTDVAT => routes.AgentInvitationJourneyController.submitPersonalSelectVat()
        case HMRCPIR    => routes.AgentInvitationJourneyController.submitPersonalSelectPir()
        case HMRCCGTPD  => routes.AgentInvitationJourneyController.submitPersonalSelectCgt()
        case _ =>
          Logger.error(s"Unexpected service in personal service selection form: $remainingService")
          routes.AgentInvitationJourneyController.showSelectService()
      }
    }

  def availableServices: Seq[(String, String)] =
    new PersonalInvitationsBasket(services, basket, featureFlags).availableServices

  def selectSingleHeaderMessage: String =
    Messages(s"select-single-service.$firstServiceKey.personal.header")

}