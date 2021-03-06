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

package uk.gov.hmrc.agentinvitationsfrontend.models
import play.api.libs.json.Format

sealed trait ClientType

object ClientType {

  case object personal extends ClientType
  case object business extends ClientType // note: also includes trusts; consider making a subtype and/or discriminator

  def toEnum: String => ClientType = {
    case "personal" => personal
    case "business" => business
    case alien      => throw new Exception(s"Client type $alien not supported")
  }

  def fromEnum: ClientType => String = {
    case ClientType.personal => "personal"
    case ClientType.business => "business"
  }

  implicit val formats: Format[ClientType] = new EnumFormats[ClientType] {
    override val deserialize: String => ClientType = toEnum
  }.formats

  def clientTypeFor(clientType: Option[ClientType], service: String): Option[ClientType] =
    clientType.orElse(service match {
      case "HMRC-MTD-IT"            => Some(ClientType.personal)
      case "PERSONAL-INCOME-RECORD" => Some(ClientType.personal)
      case "HMRC-TERS-ORG"          => Some(ClientType.business)
      case _                        => None
    })
}
