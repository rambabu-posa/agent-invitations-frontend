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

package uk.gov.hmrc.agentinvitationsfrontend.models

import play.api.libs.json._
import uk.gov.hmrc.agentmtdidentifiers.model.Vrn
import uk.gov.hmrc.domain.{Nino, TaxIdentifier}

trait KnownFact {
  val value: String
}

case class Postcode(value: String) extends KnownFact

object Postcode {
  implicit val format: Format[Postcode] = Json.format[Postcode]
}

case class VatRegDate(value: String) extends KnownFact

object VatRegDate {
  implicit val format: Format[VatRegDate] = Json.format[VatRegDate]
}

case class DOB(value: String) extends KnownFact

object DOB {
  implicit val format: Format[DOB] = Json.format[DOB]
}

sealed trait Invitation {
  val clientType: Option[ClientType]

  val service: String

  val clientIdentifier: TaxIdentifier

  val clientIdentifierType: String

  val knownFact: Option[KnownFact]

  val clientId: String = clientIdentifier.value
}

object Invitation {
  def apply(
    clientType: Option[ClientType],
    service: String,
    clientIdentifier: String,
    knownFact: Option[String]): Invitation =
    service match {
      case Services.HMRCMTDIT  => ItsaInvitation(Nino(clientIdentifier), knownFact.map(Postcode(_)))
      case Services.HMRCMTDVAT => VatInvitation(clientType, Vrn(clientIdentifier), knownFact.map(VatRegDate(_)))
      case Services.HMRCPIR    => PirInvitation(Nino(clientIdentifier), knownFact.map(DOB(_)))
    }

  implicit val format: Format[Invitation] = new Format[Invitation] {

    override def reads(json: JsValue): JsResult[Invitation] = {
      val t = (json \ "type").as[String]
      t match {
        case "ItsaInvitation" => JsSuccess((json \ "data").as[ItsaInvitation])
        case "PirInvitation"  => JsSuccess((json \ "data").as[PirInvitation])
        case "VatInvitation"  => JsSuccess((json \ "data").as[VatInvitation])
        case _                => JsError(s"invalid json type for parsing invitation object, type=$t")
      }
    }

    override def writes(invitation: Invitation): JsValue = {
      val toJson = {
        val json = Json.parse(s""" {
                                 |"clientType": "${invitation.clientType.getOrElse(
                                   throw new RuntimeException("missing clientType from the invitation"))}",
                                 |"service": "${invitation.service}",
                                 |"clientIdentifier": "${invitation.clientIdentifier.value}",
                                 |"clientIdentifierType": "${invitation.clientIdentifierType}"
                                 |}""".stripMargin)

        val knownFact: (String, JsValue) = invitation match {
          case p: ItsaInvitation => "postcode" -> Json.toJson(p.postcode)
          case p: PirInvitation  => "dob" -> Json.toJson(p.dob)
          case p: VatInvitation  => "vatRegDate" -> Json.toJson(p.vatRegDate)
          case _                 => throw new RuntimeException(s"unknown invitation type")
        }

        json.as[JsObject] + knownFact
      }

      Json.obj("type" -> invitation.getClass.getSimpleName, "data" -> toJson)
    }
  }
}

case class ItsaInvitation(
  clientIdentifier: Nino,
  postcode: Option[Postcode],
  clientType: Option[ClientType] = Some(ClientType.personal),
  service: String = Services.HMRCMTDIT,
  clientIdentifierType: String = "ni")
    extends Invitation {
  val knownFact: Option[Postcode] = postcode
}

object ItsaInvitation {
  implicit val format: Format[ItsaInvitation] = Json.format[ItsaInvitation]
}

case class PirInvitation(
  clientIdentifier: Nino,
  dob: Option[DOB],
  clientType: Option[ClientType] = Some(ClientType.personal),
  service: String = Services.HMRCPIR,
  clientIdentifierType: String = "ni")
    extends Invitation {
  val knownFact: Option[DOB] = dob
}

object PirInvitation {
  implicit val format: Format[PirInvitation] = Json.format[PirInvitation]
}

case class VatInvitation(
  clientType: Option[ClientType],
  clientIdentifier: Vrn,
  vatRegDate: Option[VatRegDate],
  service: String = Services.HMRCMTDVAT,
  clientIdentifierType: String = "vrn")
    extends Invitation {
  val knownFact: Option[VatRegDate] = vatRegDate
}

object VatInvitation {
  implicit val format: Format[VatInvitation] = Json.format[VatInvitation]
}