/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.actions

import services.PsrRetrievalService
import pages.nonsipp.memberdetails._
import pages.nonsipp.loansmadeoroutstanding.Paths.loans
import play.api.mvc.ActionTransformer
import models.UserAnswers.SensitiveJsObject
import pages.nonsipp.memberdetails.MembersDetailsPage.MembersDetailsOps
import config.RefinedTypes.Max300
import models.SchemeId.Srn
import config.Constants.UNCHANGED_SESSION_PREFIX
import transformations.Transformer
import play.api.libs.json.JsSuccess
import pages.nonsipp.memberdetails.Paths.personalDetails
import models.UserAnswers
import pages.nonsipp.loansmadeoroutstanding.{LoansMadeOrOutstandingPage, LoansRecordVersionPage}
import repositories.SessionRepository
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import models.requests.{AllowedAccessRequest, DataRequest}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

import javax.inject.{Inject, Singleton}

@Singleton
class PrePopulationDataAction @Inject()(sessionRepository: SessionRepository, psrRetrievalService: PsrRetrievalService)(
  implicit val ec: ExecutionContext
) extends Transformer {

  def apply(optLastSubmittedPsrFbInPreviousYears: Option[String]): ActionTransformer[DataRequest, DataRequest] =
    new ActionTransformer[DataRequest, DataRequest] {

      override protected def transform[A](existingDataRequest: DataRequest[A]): Future[DataRequest[A]] =
        optLastSubmittedPsrFbInPreviousYears
          .map { lastSubmittedPsrFbInPreviousYears =>
            implicit val hc: HeaderCarrier =
              HeaderCarrierConverter.fromRequestAndSession(existingDataRequest, existingDataRequest.session)
            val allowedAccessRequest = existingDataRequest.request
            for {
              baseReturn <- psrRetrievalService.getAndTransformStandardPsrDetails(
                optFbNumber = Some(lastSubmittedPsrFbInPreviousYears),
                fallBackCall = controllers.routes.OverviewController.onPageLoad(existingDataRequest.srn)
              )(
                hc = implicitly,
                ec = implicitly,
                request = DataRequest[A](allowedAccessRequest, emptyUserAnswers(allowedAccessRequest))
              )
              _ <- Future.fromTry(
                prePopulateForMemberDetailsWithPageObject(
                  baseReturn,
                  existingDataRequest.userAnswers
                )(
                  existingDataRequest.srn
                )
              )
              onlyPrePopulateMemberDetailsUA <- Future.fromTry(
                prePopulateForMemberDetailsWithJsPath(baseReturn, existingDataRequest.userAnswers)
              )
              _ <- Future.fromTry(
                prePopulateForLoan(baseReturn, onlyPrePopulateMemberDetailsUA)(existingDataRequest.srn)
              )
              _ <- sessionRepository.set(
                onlyPrePopulateMemberDetailsUA.copy(id = UNCHANGED_SESSION_PREFIX + onlyPrePopulateMemberDetailsUA.id)
              )
            } yield {
              DataRequest(
                allowedAccessRequest,
                onlyPrePopulateMemberDetailsUA
              )
            }

          }
          .getOrElse(Future.successful(existingDataRequest))
      override protected def executionContext: ExecutionContext = ec

    }

  // value of UserAnswers in DataRequest is not referenced in the psrRetrievalService
  private def emptyUserAnswers(request: AllowedAccessRequest[_]): UserAnswers =
    UserAnswers(request.getUserId + request.srn)

  // Option - 1 to work with page object
  private def prePopulateForMemberDetailsWithPageObject(baseUA: UserAnswers, currentUA: UserAnswers)(
    srn: Srn
  ): Try[UserAnswers] = {
    val memberIndexes = keysToIndex[Max300.Refined](baseUA.membersDetails(srn))
    memberIndexes.foldLeft(Try(currentUA)) {
      case (triedUa, memberIndex) =>
        for {
          ua0 <- baseUA
            .get(MemberDetailsPage(srn, memberIndex))
            .fold(triedUa)(data => triedUa.flatMap(_.setOnly(MemberDetailsPage(srn, memberIndex), data)))
          ua1 <- baseUA
            .get(DoesMemberHaveNinoPage(srn, memberIndex))
            .fold(Try(ua0))(data => ua0.setOnly(DoesMemberHaveNinoPage(srn, memberIndex), data))
          ua2 <- baseUA
            .get(MemberDetailsNinoPage(srn, memberIndex))
            .fold(Try(ua1))(data => ua1.setOnly(MemberDetailsNinoPage(srn, memberIndex), data))
          ua3 <- baseUA
            .get(NoNINOPage(srn, memberIndex))
            .fold(Try(ua2))(data => ua2.setOnly(NoNINOPage(srn, memberIndex), data))
          ua4 <- baseUA
            .get(MemberDetailsCompletedPage(srn, memberIndex))
            .fold(Try(ua3))(data => ua3.setOnly(MemberDetailsCompletedPage(srn, memberIndex), data))
          ua5 <- baseUA
            .get(MemberStatus(srn, memberIndex))
            .fold(Try(ua4))(data => ua4.setOnly(MemberStatus(srn, memberIndex), data))
        } yield {
          ua5
        }
    }

  }

  // Option - 2 to work with JsPath directly
  private def prePopulateForMemberDetailsWithJsPath(baseUA: UserAnswers, currentUA: UserAnswers): Try[UserAnswers] =
    baseUA.data.decryptedValue
      .transform(personalDetails.json.pickBranch) match {
      case JsSuccess(value, _) => Success(currentUA.copy(data = SensitiveJsObject(value)))
      case _ => Try(currentUA)
    }

  private def prePopulateForLoan(baseUA: UserAnswers, currentUA: UserAnswers)(
    srn: Srn
  ): Try[UserAnswers] =
    baseUA.data.decryptedValue
      .transform(loans.json.pickBranch)
      .flatMap(_.transform(LoansMadeOrOutstandingPage(srn).path.prune))
      .flatMap(_.transform(LoansRecordVersionPage(srn).path.prune)) match {
      case JsSuccess(value, _) => Success(currentUA.copy(data = SensitiveJsObject(value)))
      case _ => Try(currentUA)
    }

}
