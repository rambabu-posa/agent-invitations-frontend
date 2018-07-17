/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.agentinvitationsfrontend.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import uk.gov.hmrc.agentmtdidentifiers.model.Vrn
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}

trait KnownFact {
  val value: String
}

case class Postcode(value: String) extends KnownFact
case class VatRegDate(value: String) extends KnownFact
case class DOB(value: String) extends KnownFact

case class CurrentInvitationInput(
  service: Option[String],
  clientIdentifierType: Option[String],
  clientIdentifier: Option[String],
  knownFact: Option[String],
  fromFastTrack: Boolean = false)

object CurrentInvitationInput {

  val fromFastTrack: Boolean = true
  val fromManual: Boolean = false

  def apply(): CurrentInvitationInput = CurrentInvitationInput(None, None, None, None, fromManual)
  def apply(service: String): CurrentInvitationInput =
    CurrentInvitationInput(Some(service), None, None, None, fromManual)

  implicit val format = Json.format[CurrentInvitationInput]

  implicit val reads: Reads[CurrentInvitationInput] = {
    ((JsPath \ "service").readNullable[String] and
      (JsPath \ "clientIdentifierType").readNullable[String] and
      (JsPath \ "clientIdentifier").readNullable[String] and
      (JsPath \ "knownFact").readNullable[String])((a, b, c, d) => CurrentInvitationInput(a, b, c, d))
  }
}

trait FastTrackInvitation[T <: TaxIdentifier] {
  def service: String
  def clientIdentifier: T
  def clientIdentifierType: String
  def knownFact: Option[KnownFact]
}

case class FastTrackItsaInvitation(clientIdentifier: Nino, postcode: Option[Postcode])
    extends FastTrackInvitation[Nino] {
  val service = Services.HMRCMTDIT
  val clientIdentifierType = "ni"
  val knownFact = postcode
}

case class FastTrackPirInvitation(clientIdentifier: Nino, dob: Option[DOB]) extends FastTrackInvitation[Nino] {
  val service = Services.HMRCPIR
  val clientIdentifierType = "ni"
  val knownFact = dob
}

case class FastTrackVatInvitation(clientIdentifier: Vrn, vatRegDate: Option[VatRegDate])
    extends FastTrackInvitation[Vrn] {
  val service = Services.HMRCMTDVAT
  val clientIdentifierType = "vrn"
  val knownFact = vatRegDate
}
