package uk.gov.hmrc.agentinvitationsfrontend.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{put, _}
import org.joda.time.LocalDate
import uk.gov.hmrc.agentinvitationsfrontend.UriPathEncoding._
import uk.gov.hmrc.agentinvitationsfrontend.support.WireMockSupport
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, InvitationId, Vrn}

trait ACAStubs {
  me: WireMockSupport =>

  def createInvitationStubWithKnownFacts(
    arn: Arn,
    clientId: String,
    invitationId: InvitationId,
    suppliedClientId: String,
    service: String,
    serviceIdentifier: String,
    postcode: Option[String]): Unit =
    stubFor(
      post(urlEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .withRequestBody(
          equalToJson(s"""
                         |{
                         |   "service": "$service",
                         |   "clientIdType": "ni",
                         |   "clientId":"$suppliedClientId"
                         |   ${postcode.map(p => s""", "clientPostcode":"$p" """).getOrElse("")}
                         |}""".stripMargin)
        )
        .willReturn(
          aResponse()
            .withStatus(201)
            .withHeader(
              "location",
              s"$wireMockBaseUrlAsString/agent-client-authorisation/clients/$serviceIdentifier/${encodePathSegment(
                clientId)}/invitations/received/${invitationId.value}"
            )))

  def createInvitationStubForNoKnownFacts(
    arn: Arn,
    clientId: String,
    invitationId: InvitationId,
    suppliedClientId: String,
    suppliedClientType: String,
    service: String,
    serviceIdentifier: String): Unit =
    stubFor(
      post(urlEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .withRequestBody(
          equalToJson(s"""
                         |{
                         |   "service": "$service",
                         |   "clientIdType": "$suppliedClientType",
                         |   "clientId":"$suppliedClientId"
                         |}
           """.stripMargin)
        )
        .willReturn(
          aResponse()
            .withStatus(201)
            .withHeader(
              "location",
              s"$wireMockBaseUrlAsString/agent-client-authorisation/clients/$serviceIdentifier/${encodePathSegment(
                clientId)}/invitations/received/${invitationId.value}"
            )))

  def failedCreateInvitation(arn: Arn): Unit =
    stubFor(
      post(urlEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .willReturn(aResponse()
          .withStatus(400)))

  def failedCreateInvitationForNotEnrolled(arn: Arn): Unit =
    stubFor(
      post(urlEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .willReturn(
          aResponse()
            .withStatus(403)
            .withBody(s"""
                         |{
                         |   "code":"CLIENT_REGISTRATION_NOT_FOUND",
                         |   "message":"The Client's MTDfB registration was not found."
                         |}
           """.stripMargin)))

  def failedCreateInvitationFoInvalidPostcode(arn: Arn): Unit =
    stubFor(
      post(urlEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .willReturn(
          aResponse()
            .withStatus(403)
            .withBody(s"""
                         |{
                         |   "code":"POSTCODE_DOES_NOT_MATCH",
                         |   "message":"The submitted postcode did not match the client's postcode as held by HMRC."
                         |}
           """.stripMargin)))

  def getInvitationStub(
    arn: Arn,
    clientId: String,
    invitationId: InvitationId,
    service: String,
    serviceIdentifier: String,
    status: String): Unit =
    stubFor(
      get(urlEqualTo(
        s"/agent-client-authorisation/clients/$serviceIdentifier/${encodePathSegment(clientId)}/invitations/received/${invitationId.value}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "arn" : "${arn.value}",
                         |  "service" : "$service",
                         |  "clientId" : "$clientId",
                         |  "status" : "$status",
                         |  "created" : "2017-10-31T23:22:50.971Z",
                         |  "lastUpdated" : "2017-10-31T23:22:50.971Z",
                         |  "expiryDate" : "2017-12-18",
                         |  "_links": {
                         |    	"self" : {
                         |			  "href" : "$wireMockBaseUrlAsString/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"
                         |		  }
                         |  }
                         |}""".stripMargin)))

  def getExpiredInvitationStub(
    arn: Arn,
    clientId: String,
    invitationId: InvitationId,
    service: String,
    serviceIdentifier: String): Unit =
    stubFor(
      get(urlEqualTo(
        s"/agent-client-authorisation/clients/$serviceIdentifier/${encodePathSegment(clientId)}/invitations/received/${invitationId.value}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "arn" : "${arn.value}",
                         |  "service" : "$service",
                         |  "clientId" : "$clientId",
                         |  "status" : "Expired",
                         |  "created" : "2017-7-31T23:22:50.971Z",
                         |  "lastUpdated" : "2017-10-31T23:22:50.971Z",
                         |  "expiryDate" : "2017-12-18",
                         |  "_links": {
                         |    	"self" : {
                         |			  "href" : "/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"
                         |		  }
                         |  }
                         |}""".stripMargin)))

  def getAlreadyAcceptedInvitationStub(
    arn: Arn,
    clientId: String,
    invitationId: InvitationId,
    service: String,
    serviceIdentifier: String): Unit =
    stubFor(
      get(urlEqualTo(
        s"/agent-client-authorisation/clients/$serviceIdentifier/${encodePathSegment(clientId)}/invitations/received/${invitationId.value}"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(s"""
                         |{
                         |  "arn" : "${arn.value}",
                         |  "service" : "$service",
                         |  "clientId" : "$clientId",
                         |  "status" : "Accepted",
                         |  "created" : "2017-10-31T23:22:50.971Z",
                         |  "lastUpdated" : "2017-10-31T23:22:50.971Z",
                         |  "expiryDate" : "2017-12-18",
                         |  "_links": {
                         |    	"self" : {
                         |			  "href" : "$wireMockBaseUrlAsString/agent-client-authorisation/agencies/${arn.value}/invitations/sent/${invitationId.value}"
                         |		  }
                         |  }
                         |}""".stripMargin)))

  def notFoundGetInvitationStub(clientId: String, invitationId: InvitationId, serviceIdentifier: String): Unit =
    stubFor(get(urlEqualTo(
      s"/agent-client-authorisation/clients/$serviceIdentifier/${encodePathSegment(clientId)}/invitations/received/${invitationId.value}"))
      .willReturn(aResponse()
        .withStatus(404)))

  def incorrectGetInvitationStub(clientId: String, invitationId: InvitationId, serviceIdentifier: String): Unit =
    stubFor(
      get(urlEqualTo(
        s"/agent-client-authorisation/clients/$serviceIdentifier/${encodePathSegment(clientId)}/invitations/received/${invitationId.value}"))
        .willReturn(
          aResponse()
            .withStatus(403)
            .withBody(
              s"""
                 |{
                 |   "code":"NO_PERMISSION_ON_CLIENT",
                 |   "message":"The logged in client is not permitted to access invitations for the specified client."
                 |}
           """.stripMargin
            )))

  def acceptInvitationStub(clientId: String, invitationId: InvitationId, serviceIdentifier: String): Unit =
    acceptInvitationStub(clientId, invitationId, responseStatus = 204, serviceIdentifier)

  def notFoundAcceptInvitationStub(clientId: String, invitationId: InvitationId, serviceIdentifier: String): Unit =
    acceptInvitationStub(clientId, invitationId, responseStatus = 404, serviceIdentifier)

  private def acceptInvitationStub(
    clientId: String,
    invitationId: InvitationId,
    responseStatus: Int,
    serviceIdentifier: String): Unit = {
    val mtdItIdEncoded = encodePathSegment(clientId)
    val invitationIdEncoded = encodePathSegment(invitationId.value)
    stubFor(put(urlEqualTo(
      s"/agent-client-authorisation/clients/$serviceIdentifier/$mtdItIdEncoded/invitations/received/$invitationIdEncoded/accept"))
      .willReturn(aResponse()
        .withStatus(responseStatus)))
  }

  def alreadyActionedAcceptInvitationStub(
    clientId: String,
    invitationId: InvitationId,
    serviceIdentifier: String): Unit = {
    val identifierEncoded = encodePathSegment(clientId)
    val invitationIdEncoded = encodePathSegment(invitationId.value)
    stubFor(
      put(urlEqualTo(
        s"/agent-client-authorisation/clients/$serviceIdentifier/$identifierEncoded/invitations/received/$invitationIdEncoded/accept"))
        .willReturn(
          aResponse()
            .withStatus(403)
            .withBody(
              s"""
                 |{
                 |   "code":"INVALID_INVITATION_STATUS",
                 |   "message":"The invitation cannot be transitioned to Rejected because its current status is Rejected. Only Pending invitations may be transitioned to Rejected."
                 |}
           """.stripMargin
            )))
  }

  def acceptInvitationNoPermissionStub(
    clientId: String,
    invitationId: InvitationId,
    serviceIdentifier: String): Unit = {
    val identifierEncoded = encodePathSegment(clientId)
    val invitationIdEncoded = encodePathSegment(invitationId.value)
    stubFor(
      put(urlEqualTo(
        s"/agent-client-authorisation/clients/$serviceIdentifier/$identifierEncoded/invitations/received/$invitationIdEncoded/accept"))
        .willReturn(
          aResponse()
            .withStatus(403)
            .withBody(
              s"""
                 |{
                 |   "code":"NO_PERMISSION_ON_CLIENT",
                 |   "message":"The logged in client is not permitted to access invitations for the specified client."
                 |}
           """.stripMargin
            )))
  }

  def verifyAcceptInvitationAttempt(clientId: String, invitationId: InvitationId, serviceIdentifier: String): Unit = {
    val identifierEncoded = encodePathSegment(clientId)
    val invitationIdEncoded = encodePathSegment(invitationId.value)
    verify(
      1,
      putRequestedFor(urlEqualTo(
        s"/agent-client-authorisation/clients/$serviceIdentifier/$identifierEncoded/invitations/received/$invitationIdEncoded/accept"))
    )
  }

  def rejectInvitationStub(clientId: String, invitationId: InvitationId, serviceIdentifier: String): Unit =
    rejectInvitationStub(clientId, invitationId, responseStatus = 204, serviceIdentifier)

  def notFoundRejectInvitationStub(clientId: String, invitationId: InvitationId, serviceIdentifier: String): Unit =
    rejectInvitationStub(clientId, invitationId, responseStatus = 404, serviceIdentifier)

  private def rejectInvitationStub(
    clientId: String,
    invitationId: InvitationId,
    responseStatus: Int,
    serviceIdentifier: String): Unit = {
    val identifierEncoded = encodePathSegment(clientId)
    val invitationIdEncoded = encodePathSegment(invitationId.value)
    stubFor(
      put(urlEqualTo(
        s"/agent-client-authorisation/clients/$serviceIdentifier/$identifierEncoded/invitations/received/$invitationIdEncoded/reject"))
        .willReturn(aResponse()
          .withStatus(responseStatus)))
  }

  def alreadyActionedRejectInvitationStub(
    clientId: String,
    invitationId: InvitationId,
    serviceIdentifier: String): Unit = {
    val identifierEncoded = encodePathSegment(clientId)
    val invitationIdEncoded = encodePathSegment(invitationId.value)
    stubFor(
      put(urlEqualTo(
        s"/agent-client-authorisation/clients/$serviceIdentifier/$identifierEncoded/invitations/received/$invitationIdEncoded/reject"))
        .willReturn(
          aResponse()
            .withStatus(403)
            .withBody(
              s"""
                 |{
                 |   "code":"INVALID_INVITATION_STATUS",
                 |   "message":"The invitation cannot be transitioned to Rejected because its current status is Rejected. Only Pending invitations may be transitioned to Rejected."
                 |}
           """.stripMargin
            )))
  }

  def verifyRejectInvitationAttempt(clientId: String, invitationId: InvitationId, serviceIdentifier: String): Unit = {
    val identifierEncoded = encodePathSegment(clientId)
    val invitationIdEncoded = encodePathSegment(invitationId.value)
    verify(
      1,
      putRequestedFor(urlEqualTo(
        s"/agent-client-authorisation/clients/$serviceIdentifier/$identifierEncoded/invitations/received/$invitationIdEncoded/reject"))
    )
  }

  def checkVatRegisteredClientStub(vrn: Vrn, date: LocalDate, responseStatus: Int) =
    stubFor(
      get(urlEqualTo(
        s"/agent-client-authorisation/agencies/check-vat-known-fact/${vrn.value}/registration-date/${date.toString}"))
        .willReturn(aResponse()
          .withStatus(responseStatus)))

  def verifyCheckVatRegisteredClientStubAttempt(vrn: Vrn, date: LocalDate): Unit = {
    val vrnEncoded = encodePathSegment(vrn.value)
    val dateEncoded = encodePathSegment(date.toString)
    verify(
      1,
      getRequestedFor(
        urlEqualTo(
          s"/agent-client-authorisation/agencies/check-vat-known-fact/$vrnEncoded/registration-date/$dateEncoded")))
  }

  def verifyNoCheckVatRegisteredClientStubAttempt(): Unit =
    verify(
      0,
      getRequestedFor(
        urlPathMatching("/agent-client-authorisation/agencies/check-vat-known-fact/.*/registration-date/.*")))

  def givenAllInvitationsStub(arn: Arn): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .withQueryParam("createdOnOrAfter", equalTo(LocalDate.now.minusDays(30).toString("yyyy-MM-dd")))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(halEnvelope(Seq(
              invitation(arn, "Pending", "HMRC-MTD-IT", "ni", "AB123456A", "foo1", "2017-12-18"),
              invitation(arn, "Pending", "HMRC-MTD-VAT", "vrn", "101747696", "foo2", "2017-12-18"),
              invitation(arn, "Pending", "PERSONAL-INCOME-RECORD", "ni", "AB123456B", "foo3", "2017-12-18"),
              invitation(arn, "Accepted", "HMRC-MTD-IT", "ni", "AB123456A", "foo4", "2017-12-18"),
              invitation(arn, "Accepted", "HMRC-MTD-VAT", "vrn", "101747696", "foo5", "2017-12-18"),
              invitation(arn, "Accepted", "PERSONAL-INCOME-RECORD", "ni", "AB123456B", "foo6", "2017-12-18"),
              invitation(arn, "Rejected", "HMRC-MTD-IT", "ni", "AB123456A", "foo7", "2017-12-18"),
              invitation(arn, "Rejected", "HMRC-MTD-VAT", "vrn", "101747696", "foo2", "2017-12-18"),
              invitation(arn, "Rejected", "PERSONAL-INCOME-RECORD", "ni", "AB123456B", "foo8", "2017-12-18"),
              invitation(arn, "Cancelled", "HMRC-MTD-IT", "ni", "AB123456A", "foo9", "2017-12-18"),
              invitation(arn, "Cancelled", "HMRC-MTD-VAT", "vrn", "101747696", "fo10", "2017-12-18"),
              invitation(arn, "Cancelled", "PERSONAL-INCOME-RECORD", "ni", "AB123456B", "fo11", "2017-12-18"),
              invitation(arn, "Expired", "HMRC-MTD-IT", "ni", "AB123456A", "fo12", "2017-12-18"),
              invitation(arn, "Expired", "HMRC-MTD-VAT", "vrn", "101747696", "fo13", "2017-12-18"),
              invitation(arn, "Expired", "PERSONAL-INCOME-RECORD", "ni", "AB123456B", "fo14", "2017-12-18"),
              invitation(arn, "Pending", "HMRC-MTD-IT", "ni", "AB123456A", "foo1", "2099-01-01"),
              invitation(arn, "Pending", "HMRC-MTD-VAT", "vrn", "101747696", "foo2", "2099-01-01"),
              invitation(arn, "Pending", "PERSONAL-INCOME-RECORD", "ni", "AB123456B", "foo3", "2099-01-01")
            ).mkString("[", ",", "]")))))

  def givenAllInvitationsEmptyStub(arn: Arn): Unit =
    stubFor(
      get(urlPathEqualTo(s"/agent-client-authorisation/agencies/${encodePathSegment(arn.value)}/invitations/sent"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(halEnvelope("[]"))
        ))

  val invitation = (
    arn: Arn,
    status: String,
    service: String,
    clientIdType: String,
    clientId: String,
    invitationId: String,
    expiryDate: String) => s"""
                              |{
                              |  "arn" : "${arn.value}",
                              |  "service" : "$service",
                              |  "clientId" : "$clientId",
                              |  "clientIdType" : "$clientIdType",
                              |  "status" : "$status",
                              |  "created" : "2017-10-31T23:22:50.971Z",
                              |  "lastUpdated" : "2018-09-11T21:02:00.000Z",
                              |  "expiryDate" : "$expiryDate",
                              |  "_links": {
                              |    	"self" : {
                              |			  "href" : "$wireMockBaseUrlAsString/agent-client-authorisation/agencies/${arn.value}/invitations/sent/$invitationId"
                              |		  }
                              |  }
                              |}""".stripMargin

  def halEnvelope(embedded: String): String =
    s"""{"_links": {
        "invitations": [
          {
            "href": "/agent-client-authorisation/agencies/TARN0000001/invitations/sent/AK77NLH3ETXM9"
          }
        ],
        "self": {
          "href": "/agent-client-authorisation/agencies/TARN0000001/invitations/sent"
        }
      },
      "_embedded": {
        "invitations": $embedded
      }
    }""".stripMargin
}
