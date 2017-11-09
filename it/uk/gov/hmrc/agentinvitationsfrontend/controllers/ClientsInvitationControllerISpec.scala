package uk.gov.hmrc.agentinvitationsfrontend.controllers

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

import play.api.mvc.{Action, AnyContent}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.agentinvitationsfrontend.support.BaseISpec
import uk.gov.hmrc.agentmtdidentifiers.model.{Arn, MtdItId}

class ClientsInvitationControllerISpec extends BaseISpec {

  lazy val controller: ClientsInvitationController = app.injector.instanceOf[ClientsInvitationController]
  val arn = Arn("TARN0000001")
  val mtdItId = MtdItId("ABCDEF123456789")

  "GET /:invitationId (landing page)" should {
    "show the landing page even if the user is not authenticated" in {
      val result = controller.start("someInvitationID")(FakeRequest())
      status(result) shouldBe OK
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("landing-page.title"))
    }

    "session contains invitation ID when loading the landing page even if the user is not authenticated" in {
      implicit val request = FakeRequest()
      val result = controller.start("someInvitationID")(request)

      await(result).session.get("invitationId") shouldBe Some("someInvitationID")
    }
  }

  "POST / (clicking accept on the landing page)" should {

    val submitStart: Action[AnyContent] = controller.submitStart

    "redirect to /accept-tax-agent-invitation/2" in {
      getInvitationStub(arn, mtdItId, "1")
      val result = submitStart(authorisedAsValidClient(FakeRequest().withSession("invitationId" -> "1"), mtdItId.value))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.ClientsInvitationController.getConfirmInvitation().url
    }

    "redirect to /client/not-signed-up if an authenticated user does not have the HMRC-MTD-IT Enrolment" in {
      val result = submitStart(authorisedAsValidAgent(FakeRequest(), ""))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.ClientsInvitationController.notSignedUp().url
    }

    "redirect to /not-found/ if authenticated user has HMRC-MTD-IT enrolment but the invitationId they supplied does not exist" in {
      notFoundGetInvitationStub(mtdItId, "1")
      val result = submitStart(authorisedAsValidClient(FakeRequest().withSession("invitationId" -> "1"), mtdItId.value))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.ClientsInvitationController.notFoundInvitation().url
    }

    "redirect to /incorrect/ if authenticated user has HMRC-MTD-IT enrolment but with a different MTDITID" in {
      incorrectGetInvitationStub(mtdItId, "1")
      val result = submitStart(authorisedAsValidClient(FakeRequest().withSession("invitationId" -> "1"), mtdItId.value))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.ClientsInvitationController.incorrectInvitation().url
    }
  }

  "GET /accept-tax-agent-invitation/2 (confirm invitation page)" should {

    val getConfirmInvitation: Action[AnyContent] = controller.getConfirmInvitation

    "show the confirm invitation page" in {
      val result = getConfirmInvitation(authorisedAsValidClient(FakeRequest(), mtdItId.value))

      status(result) shouldBe OK
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("confirm-invitation.title"))
    }
  }

  "POST /accept-tax-agent-invitation/2 (clicking continue on the confirm invitation page)" should {
    val submitConfirmInvitation: Action[AnyContent] = controller.submitConfirmInvitation

    "reshow the page when neither yes nor no choices were selected with an error message" in {
      val result = submitConfirmInvitation(authorisedAsValidClient(FakeRequest(), mtdItId.value))

      status(result) shouldBe OK
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("confirm-invitation.title"))
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("error.confirmInvite.invalid"))
    }

    "redirect to confirm terms page when yes was selected" in {
      val req = authorisedAsValidClient(FakeRequest(), mtdItId.value).withFormUrlEncodedBody("confirmInvite" -> "true")
      val result = controller.submitConfirmInvitation().apply(req)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.ClientsInvitationController.getConfirmTerms().url
    }

    "show an error page when no was selected" in {
      val req = authorisedAsValidClient(FakeRequest(), mtdItId.value).withFormUrlEncodedBody("confirmInvite" -> "false")
      val result = controller.submitConfirmInvitation().apply(req)

      status(result) shouldBe NOT_IMPLEMENTED // TODO APB-1543
    }
  }

  "GET /accept-tax-agent-invitation/3 (confirm terms page)" should {

    val getConfirmTerms: Action[AnyContent] = controller.getConfirmTerms

    "show the confirm terms page" in {
      val req = authorisedAsValidClient(FakeRequest(), mtdItId.value)
      val result = getConfirmTerms(req)

      status(result) shouldBe OK
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("confirm-terms.title"))
    }
  }

  "POST /accept-tax-agent-invitation/3 (clicking confirm on the confirm terms page)" should {
    def withSessionData[A](req: FakeRequest[A], key: String, value: String): FakeRequest[A] = {
      req.withSession((req.session + (key -> value)).data.toSeq: _*)
    }

    val submitConfirmTerms: Action[AnyContent] = controller.submitConfirmTerms

    "redirect to complete page when the checkbox was checked" in {
      acceptInvitationStub(mtdItId, "someInvitationId")

      val req = authorisedAsValidClient(FakeRequest(), mtdItId.value).withFormUrlEncodedBody("confirmTerms" -> "true")
      val reqWithSession = withSessionData(req, "invitationId", "someInvitationId")
      val result = submitConfirmTerms(reqWithSession)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).get shouldBe routes.ClientsInvitationController.getCompletePage().url
    }

    "call agent-client-authorisation to accept the invitation and create the relationship in ETMP when the checkbox was checked" in {
      acceptInvitationStub(mtdItId, "someInvitationId")

      val req = authorisedAsValidClient(FakeRequest(), mtdItId.value).withFormUrlEncodedBody("confirmTerms" -> "true")
      val reqWithSession = withSessionData(req, "invitationId", "someInvitationId")
      await(submitConfirmTerms(reqWithSession))

      verifyAcceptInvitationAttempt(mtdItId, "someInvitationId")
    }

    "reshow the page when the checkbox was not checked with an error message" in {
      val req = authorisedAsValidClient(FakeRequest(), mtdItId.value).withFormUrlEncodedBody("confirmTerms" -> "")
      val result = submitConfirmTerms(req)

      status(result) shouldBe OK
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("confirm-terms.title"))
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("error.confirmTerms.invalid"))
    }

  }

  "GET /accept-tax-agent-invitation/4 (complete page)" should {

    val getCompletePage: Action[AnyContent] = controller.getCompletePage

    "show the complete page" in {
      val result = getCompletePage(authorisedAsValidClient(FakeRequest(), mtdItId.value))

      status(result) shouldBe OK
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("client-complete.title1"))
    }
  }
}
