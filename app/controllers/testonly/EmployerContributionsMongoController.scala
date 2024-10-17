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

package controllers.testonly

import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import config.RefinedTypes.{Max300, Max50}
import models.SchemeId.Srn
import cats.implicits._
import play.api.libs.json.Json
import models._
import pages.nonsipp.employercontributions._
import services.SaveService
import controllers.actions.IdentifyAndRequireData
import eu.timepit.refined._
import play.api.i18n.I18nSupport
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

import javax.inject.Inject

class EmployerContributionsMongoController @Inject()(
  saveService: SaveService,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  private val max: Max50 = refineMV(50)

  def addEmployerContributions(srn: Srn, index: Max300, num: Max50): Action[AnyContent] =
    identifyAndRequireData(srn).async { implicit request =>
      for {
        removedUserAnswers <- Future.fromTry(removeAllEmployerContributions(srn, index, request.userAnswers))
        updatedUserAnswers <- Future.fromTry(
          updateUserAnswersWithEmployerContributions(num.value, srn, index, removedUserAnswers)
        )
        _ <- saveService.save(updatedUserAnswers)
      } yield Ok(
        s"Added ${num.value} land or property disposals to UserAnswers for land or property index ${index.value}\n${Json
          .prettyPrint(updatedUserAnswers.data.decryptedValue)}"
      )
    }

  private def buildIndexes(num: Int): Try[List[Max50]] =
    (1 to num).map(i => refineV[Max50.Refined](i).leftMap(new Exception(_)).toTry).toList.sequence

  private def removeAllEmployerContributions(srn: Srn, index: Max300, userAnswers: UserAnswers): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(max.value)
      updatedUserAnswers <- indexes.foldLeft(Try(userAnswers)) {
        case (ua, disposalIndex) =>
          ua.flatMap(_.remove(EmployerNamePage(srn, index, disposalIndex)))
            .flatMap(_.remove(EmployerTypeOfBusinessPage(srn, index, disposalIndex)))
            .flatMap(_.remove(TotalEmployerContributionPage(srn, index, disposalIndex)))
            .flatMap(_.remove(EmployerCompanyCrnPage(srn, index, disposalIndex)))
            .flatMap(_.remove(PartnershipEmployerUtrPage(srn, index, disposalIndex)))
            .flatMap(_.remove(OtherEmployeeDescriptionPage(srn, index, disposalIndex)))
      }
    } yield updatedUserAnswers

  private def updateUserAnswersWithEmployerContributions(
    num: Int,
    srn: Srn,
    index: Max300,
    userAnswers: UserAnswers
  ): Try[UserAnswers] =
    for {
      indexes <- buildIndexes(num)
      name = buildRandomNameDOB().fullName
      employerName = indexes.map(secondaryIndex => EmployerNamePage(srn, index, secondaryIndex) -> name)
      employerBusinessType = indexes.map(
        secondaryIndex => EmployerTypeOfBusinessPage(srn, index, secondaryIndex) -> IdentityType.UKCompany
      )
      employerContribution = indexes.map(
        secondaryIndex => TotalEmployerContributionPage(srn, index, secondaryIndex) -> Money(10.50)
      )
      employerCrn = indexes.map(
        secondaryIndex =>
          EmployerCompanyCrnPage(srn, index, secondaryIndex) -> ConditionalYesNo.yes[String, Crn](Crn("1234567890"))
      )
      ua1 <- employerName.foldLeft(Try(userAnswers)) {
        case (ua, (page, value)) => ua.flatMap(_.set(page, value))
      }
      ua2 <- employerBusinessType.foldLeft(Try(ua1)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua3 <- employerContribution.foldLeft(Try(ua2)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
      ua4 <- employerCrn.foldLeft(Try(ua3)) { case (ua, (page, value)) => ua.flatMap(_.set(page, value)) }
    } yield ua4

}
