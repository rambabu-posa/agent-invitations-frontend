package uk.gov.hmrc.agentinvitationsfrontend.controllers

import play.api.mvc.{Action, AnyContent, AnyContentAsEmpty}
import play.api.test.FakeRequest
import play.api.test.Helpers.{redirectLocation, _}
import uk.gov.hmrc.agentinvitationsfrontend.controllers.AgentsInvitationController._
import uk.gov.hmrc.agentinvitationsfrontend.models._
import uk.gov.hmrc.agentinvitationsfrontend.support.BaseISpec
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.logging.SessionId

import scala.concurrent.ExecutionContext.Implicits.global

class AgentInvitationsITSAControllerJourneyISpec extends BaseISpec with AuthBehaviours {

  lazy val controller: AgentsInvitationController = app.injector.instanceOf[AgentsInvitationController]

  implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("session12345")))

  "POST /agents/select-service" should {
    val request = FakeRequest("POST", "/agents/select-service")
    val submitService = controller.submitSelectService()

    "return 303 for authorised Agent with valid ITSA service, redirect to enter identify-client page" in {
      testCurrentAuthorisationRequestCache.save(CurrentAuthorisationRequest(personal, serviceITSA))
      val serviceForm = agentInvitationServiceForm.fill(UserInputNinoAndPostcode(personal, serviceITSA, None, None))
      val result =
        submitService(authorisedAsValidAgent(request.withFormUrlEncodedBody(serviceForm.data.toSeq: _*), arn.value))

      status(result) shouldBe 303
      redirectLocation(result) shouldBe Some("/invitations/agents/identify-client")
      verifyAuthoriseAttempt()
    }
  }

  "GET /agents/identify-client" should {
    val request = FakeRequest("GET", "/agents/identify-client")
    val showIdentifyClientForm = controller.showIdentifyClient()

    behave like anAuthorisedAgentEndpoint(request, showIdentifyClientForm)

    "return 200 for an Agent with HMRC-AS-AGENT enrolment for ITSA service" in {
      testCurrentAuthorisationRequestCache.save(CurrentAuthorisationRequest(personal, serviceITSA))
      val result = showIdentifyClientForm(authorisedAsValidAgent(request, arn.value))
      status(result) shouldBe 200

      checkHtmlResultWithBodyText(
        result,
        hasMessage(
          "generic.title",
          htmlEscapedMessage("identify-client.header"),
          htmlEscapedMessage("title.suffix.agents")))

      checkHtmlResultWithBodyMsgs(
        result,
        "identify-client.header",
        "identify-client.itsa.p1",
        "identify-client.nino.label",
        "identify-client.nino.hint",
        "identify-client.postcode.label",
        "identify-client.postcode.hint"
      )

      checkHasAgentSignOutLink(result)
    }
  }

  "POST /agents/identify-client" when {
    val request = FakeRequest("POST", "/agents/identify-client")
    val submitIdentifyClient = controller.submitIdentifyClient()

    behave like anAuthorisedAgentEndpoint(request, submitIdentifyClient)

    "service is HMRC-MTD-IT" should {

      "redirect to confirm-client when a valid NINO and postcode are submitted" in {
        givenInvitationCreationSucceeds(
          arn,
          validNino.value,
          invitationIdITSA,
          validNino.value,
          "ni",
          "HMRC-MTD-IT",
          "NI")
        givenMatchingClientIdAndPostcode(validNino, validPostcode)

        testCurrentAuthorisationRequestCache.save(
          CurrentAuthorisationRequest(personal, "HMRC-MTD-IT", "ni", validNino.value, Some(validPostcode)))
        val requestWithForm = request.withFormUrlEncodedBody(
          "clientType"       -> "personal",
          "service"          -> "HMRC-MTD-IT",
          "clientIdentifier" -> validNino.value,
          "knownFact"        -> validPostcode)
        val result = submitIdentifyClient(authorisedAsValidAgent(requestWithForm, arn.value))

        status(result) shouldBe 303
        redirectLocation(result).get shouldBe routes.AgentsInvitationController.showConfirmClient().url
      }

      "redirect to client-type when a valid NINO and postcode are submitted but cache is empty" in {
        givenInvitationCreationSucceeds(
          arn,
          validNino.value,
          invitationIdITSA,
          validNino.value,
          "ni",
          "HMRC-MTD-IT",
          "NI")
        givenMatchingClientIdAndPostcode(validNino, validPostcode)

        val requestWithForm = request.withFormUrlEncodedBody(
          "clientType"       -> "personal",
          "service"          -> "HMRC-MTD-IT",
          "clientIdentifier" -> validNino.value,
          "knownFact"        -> validPostcode)
        val result = submitIdentifyClient(authorisedAsValidAgent(requestWithForm, arn.value))

        status(result) shouldBe 303
        redirectLocation(result).get shouldBe routes.AgentsInvitationController.showClientType().url
      }

      "redisplay page with errors when an empty NINO is submitted" in {
        val requestWithForm = request
          .withFormUrlEncodedBody("service" -> "HMRC-MTD-IT", "clientIdentifier" -> "", "knownFact" -> validPostcode)
        val result = submitIdentifyClient(authorisedAsValidAgent(requestWithForm, arn.value))

        status(result) shouldBe 200
        checkHtmlResultWithBodyMsgs(result, "identify-client.header", "error.nino.required")
        checkHasAgentSignOutLink(result)
      }

      "redisplay page with errors when an invalid NINO is submitted" in {
        val requestWithForm = request.withFormUrlEncodedBody(
          "service"          -> "HMRC-MTD-IT",
          "clientIdentifier" -> "invalid",
          "knownFact"        -> validPostcode)
        val result = submitIdentifyClient(authorisedAsValidAgent(requestWithForm, arn.value))

        status(result) shouldBe 200
        checkHtmlResultWithBodyMsgs(result, "identify-client.header", "enter-nino.invalid-format")
        checkHasAgentSignOutLink(result)
      }

      "redisplay page with errors when an empty postcode is submitted" in {
        val requestWithForm = request
          .withFormUrlEncodedBody("service" -> "HMRC-MTD-IT", "clientIdentifier" -> validNino.value, "knownFact" -> "")
        val result = submitIdentifyClient(authorisedAsValidAgent(requestWithForm, arn.value))

        status(result) shouldBe 200
        checkHtmlResultWithBodyMsgs(result, "identify-client.header", "error.postcode.required")
        checkHasAgentSignOutLink(result)
      }

      "redisplay page with errors when a postcode with invalid format is submitted" in {
        val requestWithForm = request.withFormUrlEncodedBody(
          "service"          -> "HMRC-MTD-IT",
          "clientIdentifier" -> validNino.value,
          "knownFact"        -> "invalid")
        val result = submitIdentifyClient(authorisedAsValidAgent(requestWithForm, arn.value))

        status(result) shouldBe 200
        checkHtmlResultWithBodyMsgs(result, "identify-client.header", "enter-postcode.invalid-format")
        checkHasAgentSignOutLink(result)
      }

      "redisplay page with errors when a postcode with invalid characters is submitted" in {
        val requestWithForm = request.withFormUrlEncodedBody(
          "service"          -> "HMRC-MTD-IT",
          "clientIdentifier" -> validNino.value,
          "knownFact"        -> "invalid%")
        val result = submitIdentifyClient(authorisedAsValidAgent(requestWithForm, arn.value))

        status(result) shouldBe 200
        checkHtmlResultWithBodyMsgs(result, "identify-client.header", "enter-postcode.invalid-characters")
        checkHasAgentSignOutLink(result)
      }

      "redirect to /agents/select-service if service is missing" in {
        val requestWithForm = request
          .withFormUrlEncodedBody("service" -> "", "clientIdentifier" -> validNino.value, "knownFact" -> validPostcode)
        val result = submitIdentifyClient(authorisedAsValidAgent(requestWithForm, arn.value))

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(routes.AgentsInvitationController.showSelectService().url)
      }

      "redirect to /agents/select-service when there are errors in the form" in {
        val requestWithForm = request
          .withFormUrlEncodedBody("goo" -> "", "bah" -> "", "gah" -> "")
        val result = submitIdentifyClient(authorisedAsValidAgent(requestWithForm, arn.value))

        status(result) shouldBe 303
        redirectLocation(result) shouldBe Some(routes.AgentsInvitationController.showSelectService().url)
      }
    }
  }

  "GET /agents/invitation-sent" should {
    val request = FakeRequest("GET", "/agents/invitation-sent")
    val invitationSent = controller.showInvitationSent()

    "return 200 for authorised Agent successfully created ITSA invitation and redirected to Confirm Invitation Page (secureFlag = false) with no continue Url" in {
      givenAgentReference(arn, uid, "personal")
      val authRequest =
        AuthorisationRequest("clienty name", serviceITSA, validNino.value, AuthorisationRequest.CREATED, "itemId")
      testAgentMultiAuthorisationJourneyStateCache.save(
        AgentMultiAuthorisationJourneyState("personal", Set(authRequest)))

      val result = invitationSent(authorisedAsValidAgent(request, arn.value))
      status(result) shouldBe 200
      checkHtmlResultWithBodyText(
        result,
        htmlEscapedMessage(
          "generic.title",
          htmlEscapedMessage("invitation-sent-link.header"),
          htmlEscapedMessage("title.suffix.agents")))
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("invitation-sent.header"))
      checkHtmlResultWithBodyText(result, hasMessage("invitation-sent.l2", "someurl"))
      checkHtmlResultWithBodyText(result, hasMessage("invitation-sent.p1"))
      checkHtmlResultWithBodyText(result, hasMessage("invitation-sent.p2.personal"))
      checkHtmlResultWithBodyText(result, hasMessage("invitation-sent.l1.p.personal"))
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("invitation-sent.trackRequests.button"))
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("invitation-sent.continueToASAccount.button"))
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("invitation-sent.startNewAuthRequest"))
      checkHtmlResultWithBodyText(
        result,
        htmlEscapedMessage(
          s"$wireMockBaseUrlAsString${routes.ClientsMultiInvitationController.warmUp("personal", uid, "99-with-flake")}"))
      checkHtmlResultWithBodyText(result, wireMockBaseUrlAsString)
      checkInviteSentExitSurveyAgentSignOutLink(result)

      verifyAuthoriseAttempt()
      await(testCurrentAuthorisationRequestCache.fetch).get shouldBe CurrentAuthorisationRequest()
    }

    "throw a IllegalStateException when there is nothing in the cache" in {
      val result = invitationSent(authorisedAsValidAgent(request, arn.value))
      intercept[IllegalStateException] {
        await(result)
      }.getMessage shouldBe "Cached session state expected but not found"
    }
  }

  "GET /agents/review-authorisations" should {
    "show the review authorisations page" in {
      val result = controller.showReviewAuthorisations
    }
  }

  "GET /agents/not-enrolled" should {
    val request = FakeRequest("GET", "/agents/not-enrolled")
    val notEnrolled = controller.notEnrolled()
    val featureFlags = FeatureFlags()

    "return 403 for authorised Agent who submitted known facts of an not enrolled ITSA client" in {
      testCurrentAuthorisationRequestCache.save(CurrentAuthorisationRequest(personal, serviceITSA))
      val ninoForm =
        agentInvitationIdentifyClientFormItsa(featureFlags).fill(
          UserInputNinoAndPostcode(personal, serviceITSA, None, None))
      val result =
        notEnrolled(authorisedAsValidAgent(request.withFormUrlEncodedBody(ninoForm.data.toSeq: _*), arn.value))

      status(result) shouldBe 403
      checkHtmlResultWithBodyText(
        result,
        htmlEscapedMessage(
          "generic.title",
          htmlEscapedMessage("not-enrolled.itsa.header"),
          htmlEscapedMessage("title.suffix.agents")))
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("not-enrolled.itsa.description"))
      checkHtmlResultWithBodyText(result, htmlEscapedMessage("not-enrolled.itsa.button"))
      checkHasAgentSignOutLink(result)
      verifyAuthoriseAttempt()
      await(testCurrentAuthorisationRequestCache.fetch) shouldBe None

    }
  }

  "GET /confirm-client" should {
    val request = FakeRequest("GET", "/agents/confirm-client")
    val showConfirmClient = controller.showConfirmClient()

    "return 200 and show client trading name" in {
      testCurrentAuthorisationRequestCache.save(
        CurrentAuthorisationRequest(personal, serviceITSA, "ni", validNino.value, Some(validPostcode), fromManual))
      givenTradingName(validNino, "64 Bit")
      val result = showConfirmClient(authorisedAsValidAgent(request, arn.value))
      status(result) shouldBe 200
      checkHtmlResultWithBodyText(result, "64 Bit")
      checkHtmlResultWithBodyMsgs(result, "confirm-client.header")
      checkHtmlResultWithBodyMsgs(result, "confirm-client.yes")
      checkHtmlResultWithBodyMsgs(result, "confirm-client.no")
    }

    "return 200 and show client's name" in {
      testCurrentAuthorisationRequestCache.save(
        CurrentAuthorisationRequest(personal, serviceITSA, "ni", validNino.value, Some(validPostcode), fromManual))
      givenTradingNameMissing(validNino)
      givenCitizenDetailsAreKnownFor(validNino.value, "Anne Marri", "Son Pear")
      val result = showConfirmClient(authorisedAsValidAgent(request, arn.value))
      status(result) shouldBe 200
      checkHtmlResultWithBodyText(result, "Anne Marri Son Pear")
      checkHtmlResultWithBodyMsgs(result, "confirm-client.header")
      checkHtmlResultWithBodyMsgs(result, "confirm-client.yes")
      checkHtmlResultWithBodyMsgs(result, "confirm-client.no")
    }

    "return 200 and no client name was found" in {
      testCurrentAuthorisationRequestCache.save(
        CurrentAuthorisationRequest(personal, serviceITSA, "ni", validNino.value, Some(validPostcode), fromManual))
      givenTradingNameMissing(validNino)
      givenCitizenDetailsReturns404For(validNino.value)
      val result = showConfirmClient(authorisedAsValidAgent(request, arn.value))
      status(result) shouldBe 200
      checkHtmlResultWithBodyMsgs(result, "confirm-client.header")
      checkHtmlResultWithBodyMsgs(result, "confirm-client.yes")
      checkHtmlResultWithBodyMsgs(result, "confirm-client.no")
    }

    behaveLikeMissingCacheScenarios(showConfirmClient, request)
  }

  "POST /confirm-client" should {
    val request = FakeRequest("POST", "/agents/confirm-client")
    val submitConfirmClient = controller.submitConfirmClient()

    "redirect to show-review-authorisations when YES is selected" in {
      testCurrentAuthorisationRequestCache.save(
        CurrentAuthorisationRequest(personal, serviceITSA, "ni", validNino.value, Some(validPostcode), fromManual))
      givenInvitationCreationSucceeds(arn, mtdItId.value, invitationIdITSA, validNino.value, "ni", serviceITSA, "NI")
      givenTradingName(validNino, "64 Bit")
      givenAgentReference(arn, "ABCDEFGH", "personal")
      givenGetAllPendingInvitationsReturnsEmpty(arn, validNino.value, serviceITSA)
      val choice = agentConfirmationForm("error message").fill(Confirmation(true))
      val result =
        submitConfirmClient(authorisedAsValidAgent(request, arn.value).withFormUrlEncodedBody(choice.data.toSeq: _*))
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showReviewAuthorisations().url
      status(result) shouldBe 303
    }

    "redirect to show identify client when NO is selected" in {
      testCurrentAuthorisationRequestCache.save(
        CurrentAuthorisationRequest(personal, serviceITSA, "ni", validNino.value, Some(validPostcode), fromManual))
      givenInvitationCreationSucceeds(arn, mtdItId.value, invitationIdITSA, validNino.value, "ni", serviceITSA, "NI")
      givenTradingName(validNino, "64 Bit")
      givenAgentReference(arn, "ABCDEFGH", "personal")
      givenGetAllPendingInvitationsReturnsEmpty(arn, validNino.value, serviceITSA)
      val choice = agentConfirmationForm("error message").fill(Confirmation(false))
      val result =
        submitConfirmClient(authorisedAsValidAgent(request, arn.value).withFormUrlEncodedBody(choice.data.toSeq: _*))
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClient().url
      status(result) shouldBe 303
    }

    "redirect to select client type when client type in cache is not supported" in {
      testCurrentAuthorisationRequestCache.save(
        CurrentAuthorisationRequest(Some("foo"), serviceITSA, "ni", validNino.value, Some(validPostcode), fromManual))
      givenInvitationCreationSucceeds(arn, mtdItId.value, invitationIdITSA, validNino.value, "ni", serviceITSA, "NI")
      givenTradingName(validNino, "64 Bit")
      givenAgentReference(arn, "ABCDEFGH", "personal")
      givenGetAllPendingInvitationsReturnsEmpty(arn, validNino.value, serviceITSA)
      val choice = agentConfirmationForm("error message").fill(Confirmation(true))
      val result =
        submitConfirmClient(authorisedAsValidAgent(request, arn.value).withFormUrlEncodedBody(choice.data.toSeq: _*))
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showClientType().url
      status(result) shouldBe 303
    }

    "redirect to already-authorisation-pending when the agent has already created an invitation for this client and service" in {
      testCurrentAuthorisationRequestCache.save(
        CurrentAuthorisationRequest(personal, serviceITSA, "ni", validNino.value, Some(validPostcode), fromManual))
      givenInvitationExists(arn, mtdItId.value, invitationIdITSA, serviceITSA, "itsa", "Pending")
      givenTradingName(validNino, "64 Bit")
      givenAgentReference(arn, "ABCDEFGH", "personal")
      givenGetAllPendingInvitationsReturnsSome(arn, validNino.value, serviceITSA)

      val choice = agentConfirmationForm("error message").fill(Confirmation(true))
      val result =
        submitConfirmClient(authorisedAsValidAgent(request, arn.value).withFormUrlEncodedBody(choice.data.toSeq: _*))

      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.pendingAuthorisationExists().url
    }

    "return 200 for not selecting an option" in {
      testCurrentAuthorisationRequestCache.save(
        CurrentAuthorisationRequest(personal, serviceITSA, "ni", validNino.value, Some(validPostcode), fromManual))
      givenTradingName(validNino, "64 Bit")
      val result = submitConfirmClient(authorisedAsValidAgent(request, arn.value))
      status(result) shouldBe 200
      givenTradingName(validNino, "64 Bit")
      checkHtmlResultWithBodyMsgs(result, "error.confirm-client.required")
      checkHtmlResultWithBodyMsgs(result, "confirm-client.header")
      checkHtmlResultWithBodyMsgs(result, "confirm-client.yes")
      checkHtmlResultWithBodyMsgs(result, "confirm-client.no")
    }

    behaveLikeMissingCacheScenarios(submitConfirmClient, request)
  }

  def behaveLikeMissingCacheScenarios(action: Action[AnyContent], request: FakeRequest[AnyContentAsEmpty.type]) = {
    "return to identify-client no client identifier found in cache" in {
      testCurrentAuthorisationRequestCache.save(
        CurrentAuthorisationRequest(personal, serviceITSA, "", "", None, fromManual))
      val result = action(authorisedAsValidAgent(request, arn.value))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showIdentifyClient().url
    }

    "return to client-type for no cache" in {
      val result = action(authorisedAsValidAgent(request, arn.value))
      status(result) shouldBe 303
      redirectLocation(result).get shouldBe routes.AgentsInvitationController.showClientType().url
    }
  }
}
