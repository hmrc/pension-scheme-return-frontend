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

package controllers

import pages.nonsipp.employercontributions._
import pages.nonsipp.memberdetails.{MemberDetailsNinoPage, MemberDetailsPage, NoNINOPage}
import play.api.mvc.{Call, Result, Results}
import config.RefinedTypes._
import pages.nonsipp.membersurrenderedbenefits.{
  SurrenderedBenefitsAmountPage,
  WhenDidMemberSurrenderBenefitsPage,
  WhyDidMemberSurrenderBenefitsPage
}
import models._
import cats.Applicative
import viewmodels.models.Flag
import models.requests.DataRequest
import eu.timepit.refined.api.{Refined, Validate}
import pages.nonsipp.membercontributions.TotalMemberContributionPage
import pages.nonsipp.memberreceivedpcls.PensionCommencementLumpSumAmountPage
import queries.{Gettable, Removable, Settable}
import cats.data.{EitherT, NonEmptyList}
import models.SchemeId.Srn
import cats.implicits.toTraverseOps
import pages.nonsipp.receivetransfer._
import cats.syntax.applicative._
import models.requests.psr._
import pages.nonsipp.memberpensionpayments.TotalAmountPensionPaymentsPage
import cats.syntax.either._
import eu.timepit.refined.refineV
import play.api.Logger
import play.api.libs.json.{Reads, Writes}
import utils.nonsipp.PrePopulationUtils
import models.backend.responses.{IndividualDetails, PsrVersionsResponse}
import pages.nonsipp.membertransferout._
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import java.time.LocalDate
import java.time.format.DateTimeFormatter

trait PsrControllerHelpers extends Results {

  def requiredPageLogger: Logger = Logger("RequiredPage")

  def requiredPage[A: Reads](page: Gettable[A])(implicit request: DataRequest[?]): Either[Result, A] =
    request.userAnswers.get(page) match {
      case Some(value) => Right(value)
      case None =>
        // Check if it's a case class and extract param names and values
        val params: String = page match {
          case p: Product =>
            p.productElementNames
              .zip(p.productIterator)
              .map { case (name, value) =>
                s"$name: ${value.toString}"
              }
              .mkString(", ")
          case _ => "No parameters available"
        }
        requiredPageLogger.error(s"Required page ${page.getClass.getSimpleName}($params) missing")
        Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

  def loggedInUserNameOrRedirect(implicit request: DataRequest[?]): Either[Result, String] =
    request.minimalDetails.individualDetails match {
      case Some(individual) => Right(individual.fullName)
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => Right(orgName)
          case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
        }
    }

  implicit class EitherOps[A](maybe: Either[String, A]) {
    def getOrRecoverJourney(implicit logger: Logger): Either[Result, A] =
      maybe.leftMap { errMsg =>
        logger.warn(errMsg)
        Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
      }
  }

  implicit class OptionOps[A](maybe: Option[A]) {
    def getOrRecoverJourney[F[_]: Applicative](f: A => F[Result]): F[Result] = maybe match {
      case Some(value) => f(value)
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()).pure
    }

    def getOrRecoverJourney(f: A => Result): Result = maybe match {
      case Some(value) => f(value)
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }

    def getOrRecoverJourney: Either[Result, A] = maybe match {
      case Some(value) => Right(value)
      case None => Left(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()))
    }

    def getOrRecoverJourney[F[_]: Applicative]: F[Either[Result, A]] = maybe match {
      case Some(value) => value.asRight[Result].pure
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad()).asLeft[A].pure
    }

    def getOrRecoverJourneyT(implicit ec: ExecutionContext): EitherT[Future, Result, A] = maybe match {
      case Some(value) => EitherT.right(Future.successful(value))
      case None => EitherT.left(Future.successful(Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())))
    }

    def getOrRedirectToTaskList(srn: Srn): Either[Result, A] = maybe match {
      case Some(value) => Right(value)
      case None => Left(Redirect(controllers.nonsipp.routes.TaskListController.onPageLoad(srn)))
    }
  }

  def refineIndex[A](index: Int)(implicit ev: Validate[Int, A]): Option[Refined[Int, A]] =
    refineV[A](index + 1).toOption

  // Used to specifically refine an index retrieved from user answers
  // These indexes will be strings and 0 based so we need to add 1 before refining
  def refineStringIndex[A](indexAsString: String)(implicit ev: Validate[Int, A]): Option[Refined[Int, A]] =
    indexAsString.toIntOption.flatMap(refineIndex[A])

  def recoverJourneyWhen(
    bool: Boolean,
    call: Call = controllers.routes.JourneyRecoveryController.onPageLoad()
  ): Either[Result, Unit] =
    if (bool) Left(Redirect(call)) else Right(())

  implicit class TaxOrAccountingPeriodOps(o: Option[Either[DateRange, NonEmptyList[(DateRange, Max3)]]]) {
    def merge: Option[DateRange] = o.map(_.map(_.toList.map(_._1).min).merge)
  }
}

abstract class PSRController
    extends FrontendBaseController
    with I18nSupport
    with PrePopulationUtils
    with PsrControllerHelpers {

  private val logger = Logger("PSRController")

  implicit def requestToUserAnswers(implicit req: DataRequest[?]): UserAnswers = req.userAnswers

  def formatDateForApi(date: LocalDate): String =
    date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

  def getSubmitter(response: PsrVersionsResponse): String = {
    val emptySubmitterName = ""
    response.reportSubmitterDetails.fold(emptySubmitterName)(submitter =>
      submitter.individualDetails match {
        case Some(individualDetails: IndividualDetails) =>
          individualDetails.firstName + " " + individualDetails.lastName
        case None =>
          submitter.organisationOrPartnershipDetails match {
            case Some(orgDetails) => orgDetails.organisationOrPartnershipName
            case None => emptySubmitterName
          }
      }
    )
  }

  def loggedInUserNameOrBlank(implicit request: DataRequest[?]): String =
    request.minimalDetails.individualDetails match {
      case Some(individual) => individual.fullName
      case None =>
        request.minimalDetails.organisationName match {
          case Some(orgName) => orgName
          case None => ""
        }
    }

  implicit class FutureEitherOps[A](f: Future[Either[A, A]])(implicit ec: ExecutionContext) {
    def merge: Future[A] = f.map(_.merge)
  }

  implicit class FutureOps[A](f: Future[A]) {
    def liftF(implicit ec: ExecutionContext): EitherT[Future, Result, A] = EitherT.liftF(f)
  }

  implicit class ListIndexOps[A](l: List[A]) {
    def zipWithRefinedIndex[I](using Validate[Int, I]): Either[Result, List[(Refined[Int, I], A)]] =
      l.zipWithIndex.traverse { case (a, index) => refineIndex(index).map(_ -> a).getOrRecoverJourney }
  }

  implicit class UserAnswersOps(userAnswers: UserAnswers) {
    def exists[A: Reads](page: Gettable[A])(f: A => Boolean)(implicit request: DataRequest[?]): Boolean =
      request.userAnswers.get(page).fold(true)(f)

    def buildMemberDetails(srn: Srn, index: Max300): Option[MemberPersonalDetails] =
      userAnswers
        .get(MemberDetailsPage(srn, index))
        .map(memberDetails =>
          MemberPersonalDetails(
            firstName = memberDetails.firstName,
            lastName = memberDetails.lastName,
            nino = userAnswers.get(MemberDetailsNinoPage(srn, index)).map(_.value),
            reasonNoNINO = userAnswers.get(NoNINOPage(srn, index)),
            dateOfBirth = memberDetails.dob
          )
        )

    def buildEmployerContributions(
      srn: Srn,
      index: Max300
    ): Option[List[EmployerContributions]] = {
      val secondaryIndexes =
        userAnswers
          .map(EmployerContributionsCompleted.all(index))
          .keys
          .toList
          .flatMap(_.toIntOption.flatMap(i => refineV[Max50.Refined](i + 1).leftMap(new Exception(_)).toOption))

      secondaryIndexes.traverse(secondaryIndex =>
        for {
          employerName <- userAnswers.get(EmployerNamePage(srn, index, secondaryIndex))
          identityType <- userAnswers.get(EmployerTypeOfBusinessPage(srn, index, secondaryIndex))
          employerType <- identityType match {
            case IdentityType.Individual => None
            case IdentityType.UKCompany =>
              userAnswers
                .get(EmployerCompanyCrnPage(srn, index, secondaryIndex))
                .map(v => EmployerType.UKCompany(v.value.map(_.value)))
            case IdentityType.UKPartnership =>
              userAnswers
                .get(PartnershipEmployerUtrPage(srn, index, secondaryIndex))
                .map(v => EmployerType.UKPartnership(v.value.map(_.value)))
            case IdentityType.Other =>
              userAnswers
                .get(OtherEmployeeDescriptionPage(srn, index, secondaryIndex))
                .map(EmployerType.Other.apply)
          }
          total <- userAnswers.get(TotalEmployerContributionPage(srn, index, secondaryIndex))
        } yield EmployerContributions(
          employerName,
          employerType,
          totalTransferValue = total.value
        )
      )
    }

    def buildTransfersIn(
      srn: Srn,
      index: Max300
    ): Option[List[TransfersIn]] = {
      val secondaryIndexes =
        userAnswers
          .map(TransfersInSectionCompleted.all(index))
          .keys
          .toList
          .flatMap(_.toIntOption.flatMap(i => refineV[Max5.Refined](i + 1).leftMap(new Exception(_)).toOption))

      secondaryIndexes.traverse(secondaryIndex =>
        for {
          schemeName <- userAnswers.get(TransferringSchemeNamePage(srn, index, secondaryIndex))
          dateOfTransfer <- userAnswers.get(WhenWasTransferReceivedPage(srn, index, secondaryIndex))
          transferValue <- userAnswers.get(TotalValueTransferPage(srn, index, secondaryIndex))
          transferIncludedAsset <- userAnswers.get(DidTransferIncludeAssetPage(srn, index, secondaryIndex))
          transferSchemeType <- userAnswers.get(TransferringSchemeTypePage(srn, index, secondaryIndex))
        } yield TransfersIn(
          schemeName = schemeName,
          dateOfTransfer = dateOfTransfer,
          transferSchemeType = transferSchemeType,
          transferValue = transferValue.value,
          transferIncludedAsset = transferIncludedAsset
        )
      )
    }

    def buildTransfersOut(
      srn: Srn,
      index: Max300
    ): Option[List[TransfersOut]] = {
      val secondaryIndexes =
        userAnswers
          .map(TransfersOutSectionCompleted.all(index))
          .keys
          .toList
          .flatMap(_.toIntOption.flatMap(i => refineV[Max5.Refined](i + 1).leftMap(new Exception(_)).toOption))

      secondaryIndexes.traverse(secondaryIndex =>
        for {
          schemeName <- userAnswers.get(ReceivingSchemeNamePage(srn, index, secondaryIndex))
          dateOfTransfer <- userAnswers.get(WhenWasTransferMadePage(srn, index, secondaryIndex))
          transferSchemeType <- userAnswers.get(ReceivingSchemeTypePage(srn, index, secondaryIndex))
        } yield TransfersOut(
          schemeName = schemeName,
          dateOfTransfer = dateOfTransfer,
          transferSchemeType = transferSchemeType
        )
      )
    }

    def buildMemberContributions(srn: Srn, index: Max300): Option[Double] =
      userAnswers.get(TotalMemberContributionPage(srn, index)).map(_.value)

    def buildPCLS(srn: Srn, index: Max300): Option[PensionCommencementLumpSum] =
      userAnswers.get(PensionCommencementLumpSumAmountPage(srn, index))

    def buildMemberPensionPayments(srn: Srn, index: Max300): Option[Double] =
      userAnswers.get(TotalAmountPensionPaymentsPage(srn, index)).map(_.value)

    def buildSurrenderedBenefits(srn: Srn, index: Max300): Option[SurrenderedBenefits] =
      for {
        totalSurrendered <- userAnswers.get(SurrenderedBenefitsAmountPage(srn, index))
        dateOfSurrender <- userAnswers.get(WhenDidMemberSurrenderBenefitsPage(srn, index))
        surrenderReason <- userAnswers.get(WhyDidMemberSurrenderBenefitsPage(srn, index))
      } yield SurrenderedBenefits(
        totalSurrendered = totalSurrendered.value,
        dateOfSurrender = dateOfSurrender,
        surrenderReason = surrenderReason
      )

    def changed[A](f: UserAnswers => Option[A])(implicit request: DataRequest[?]): Boolean =
      request.pureUserAnswers match {
        case Some(pure) =>
          (for {
            initial <- f(pure)
            current <- f(userAnswers)
          } yield initial != current).getOrElse(true)
        case None => false
      }

    def changedList[A](f: UserAnswers => Option[List[A]])(implicit request: DataRequest[?]): Boolean =
      request.pureUserAnswers match {
        case Some(pure) =>
          (for {
            initial <- f(pure)
            current <- f(userAnswers)
          } yield !(initial.length == current.length && initial.forall(current.contains))).getOrElse(true)
        case None => false
      }
  }

  implicit class UserAnswersTryOps(userAnswers: Try[UserAnswers]) {
    def set[A: Writes](page: Settable[A], value: A): Try[UserAnswers] = userAnswers.flatMap(_.set(page, value))
    def set(page: Settable[Flag]): Try[UserAnswers] = userAnswers.flatMap(_.set(page, Flag))
    def setWhen[A: Writes](bool: Boolean)(page: Settable[A], value: A): Try[UserAnswers] =
      userAnswers.flatMap(_.setWhen(bool)(page, value))
    def compose(c: List[UserAnswers.Compose]): Try[UserAnswers] = userAnswers.flatMap(_.compose(c))
    def remove[A](page: Removable[A]): Try[UserAnswers] = userAnswers.flatMap(_.removeOnlyMultiplePages(List(page)))

    def remove(pages: List[Removable[?]]): Try[UserAnswers] = userAnswers.flatMap(_.removeOnlyMultiplePages(pages))

    def softRemove[A: Reads: Writes](page: Gettable[A] & Settable[A] & Removable[A]): Try[UserAnswers] =
      userAnswers.flatMap(_.softRemove(page))

    def removeWhen(bool: Boolean)(page: Removable[?]*): Try[UserAnswers] =
      userAnswers.flatMap(_.removeOnlyWhen(bool)(page*))

    def removeWhen(bool: UserAnswers => Boolean)(page: Removable[?]*): Try[UserAnswers] =
      userAnswers.flatMap(ua => ua.removeOnlyWhen(bool(ua))(page*))

    def when(
      get: UserAnswers => Option[Boolean]
    )(ifTrue: UserAnswers => Try[UserAnswers], ifFalse: UserAnswers => Try[UserAnswers]): Try[UserAnswers] =
      userAnswers.flatMap(ua => ua.when(get)(ifTrue, ifFalse))
  }
}
