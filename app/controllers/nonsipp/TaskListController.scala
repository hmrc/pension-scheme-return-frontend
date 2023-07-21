/*
 * Copyright 2023 HM Revenue & Customs
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

/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http:www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.nonsipp

import cats.implicits.toShow
import com.google.inject.Inject
import controllers.actions._
import models.SchemeId.Srn
import models.requests.DataRequest
import models.{DateRange, NormalMode}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SchemeDateService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import utils.DateTimeUtils.localDateShow
import viewmodels.DisplayMessage.{Heading2, LinkMessage, Message, ParagraphMessage}
import viewmodels.implicits._
import viewmodels.models.TaskListStatus.{Completed, NotStarted, TaskListStatus, UnableToStart}
import viewmodels.models.{PageViewModel, TaskListItemViewModel, TaskListSectionViewModel, TaskListViewModel}
import views.html.TaskListView

import java.time.LocalDate

class TaskListController @Inject()(
  override val messagesApi: MessagesApi,
  identifyAndRequireData: IdentifyAndRequireData,
  val controllerComponents: MessagesControllerComponents,
  view: TaskListView,
  schemeDateService: SchemeDateService
) extends FrontendBaseController
    with I18nSupport {

  def onPageLoad(srn: Srn): Action[AnyContent] = identifyAndRequireData(srn) { implicit request =>
    withSchemeDate(srn) { dates =>
      val viewModel = TaskListController.viewModel(srn, request.schemeDetails.schemeName, dates.from, dates.to)
      Ok(view(viewModel))
    }
  }

  private def withSchemeDate(srn: Srn)(body: DateRange => Result)(implicit request: DataRequest[_]): Result =
    schemeDateService.schemeDate(srn) match {
      case Some(dates) => body(dates)
      case None => Redirect(controllers.routes.JourneyRecoveryController.onPageLoad())
    }
}

object TaskListController {

  def messageKey(prefix: String, suffix: String, status: TaskListStatus): String =
    status match {
      case UnableToStart | NotStarted => s"$prefix.add.$suffix"
      case _ => s"$prefix.change.$suffix"
    }

  private def schemeDetailsSection(srn: Srn, schemeName: String) = {
    val prefix = "nonsipp.tasklist.schemedetails"

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(s"$prefix.details.title", schemeName),
          controllers.nonsipp.schemedesignatory.routes.HowManyMembersController.onPageLoad(srn, NormalMode).url
        ),
        Completed
      ),
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "finances.title", NotStarted), schemeName),
          controllers.nonsipp.schemedesignatory.routes.HowMuchCashController.onPageLoad(srn, NormalMode).url
        ),
        NotStarted
      )
    )
  }

  private def membersSection(srn: Srn, schemeName: String) = {
    val prefix = "nonsipp.tasklist.members"

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "details.title", NotStarted), schemeName),
          controllers.nonsipp.memberdetails.routes.PensionSchemeMembersController.onPageLoad(srn).url
        ),
        NotStarted
      )
    )
  }

  private def memberPaymentsSection(srn: Srn) = {
    val prefix = "nonsipp.tasklist.memberpayments"

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "employercontributions.title", UnableToStart),
          controllers.nonsipp.memberpayments.routes.EmployerContributionsController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        UnableToStart
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "unallocatedcontributions.title", UnableToStart),
          controllers.nonsipp.memberpayments.routes.UnallocatedEmployerContributionsController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        UnableToStart
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "memberContributions.title", UnableToStart),
          controllers.nonsipp.membercontributions.routes.MemberContributionsController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        UnableToStart
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "transfersreceived.title", UnableToStart),
          controllers.nonsipp.receivetransfer.routes.DidSchemeReceiveTransferController.onPageLoad(srn, NormalMode).url
        ),
        UnableToStart
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "transfersout.title", UnableToStart),
          controllers.nonsipp.transferout.routes.SchemeTransferOutController.onPageLoad(srn, NormalMode).url
        ),
        UnableToStart
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "pcls.title", UnableToStart),
          controllers.nonsipp.pensioncommencementlumpsum.routes.PensionCommencementLumpSumController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        UnableToStart
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "payments.title", UnableToStart),
          controllers.nonsipp.routes.PensionPaymentsReceivedController.onPageLoad(srn, NormalMode).url
        ),
        UnableToStart
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "surrenderedbenefits.title", UnableToStart),
          controllers.nonsipp.benefitssurrendered.routes.BenefitsSurrenderedController.onPageLoad(srn, NormalMode).url
        ),
        UnableToStart
      )
    )
  }

  private def loansSection(srn: Srn, schemeName: String) = {
    val prefix = s"nonsipp.tasklist.loans"

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "loansmade.title", UnableToStart), schemeName),
          controllers.nonsipp.loansmadeoroutstanding.routes.LoansMadeOrOutstandingController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        NotStarted
      ),
      TaskListItemViewModel(
        LinkMessage(
          Message(messageKey(prefix, "moneyborrowed.title", UnableToStart), schemeName),
          controllers.nonsipp.moneyborrowed.routes.MoneyBorrowedController.onPageLoad(srn, NormalMode).url
        ),
        NotStarted
      )
    )
  }

  private def sharesSection(srn: Srn) = {
    val prefix = "nonsipp.tasklist.shares"

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "sponsoringemployer.title", UnableToStart),
          controllers.nonsipp.sharesinsponsoringemployer.routes.DidSchemeHoldSharesInSponsoringEmployerController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        NotStarted
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "connectedparty.title", UnableToStart),
          controllers.nonsipp.sharesacquiredfromconnectedparty.routes.SharesAcquiredFromConnectedPartyController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        NotStarted
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "unquotedshares.title", UnableToStart),
          controllers.nonsipp.unquotedshares.routes.UnquotedSharesController.onPageLoad(srn, NormalMode).url
        ),
        NotStarted
      ),
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "quotedshares.title", UnableToStart),
          controllers.routes.UnauthorisedController.onPageLoad().url
        ),
        NotStarted
      )
    )
  }

  private def landOrPropertySection(srn: Srn) = {
    val prefix = "nonsipp.tasklist.landorproperty"

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "title", UnableToStart),
          controllers.nonsipp.landorproperty.routes.LandOrPropertyHeldController.onPageLoad(srn, NormalMode).url
        ),
        NotStarted
      )
    )
  }

  private def bondsSection(srn: Srn) = {
    val prefix = "nonsipp.tasklist.bonds"

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "unregulatedorconnected.title", UnableToStart),
          controllers.nonsipp.unregulatedorconnectedbonds.routes.UnregulatedOrConnectedBondsHeldController
            .onPageLoad(srn, NormalMode)
            .url
        ),
        NotStarted
      )
    )
  }

  private def otherAssetsSection(srn: Srn) = {
    val prefix = "nonsipp.tasklist.otherassets"

    TaskListSectionViewModel(
      s"$prefix.title",
      TaskListItemViewModel(
        LinkMessage(
          messageKey(prefix, "title", UnableToStart),
          controllers.nonsipp.otherassetsheld.routes.OtherAssetsHeldController.onPageLoad(srn, NormalMode).url
        ),
        NotStarted
      )
    )
  }

  private val declarationSection = {
    val prefix = "nonsipp.tasklist.declaration"

    TaskListSectionViewModel(
      s"$prefix.title",
      Message(s"$prefix.incomplete"),
      LinkMessage(s"$prefix.saveandreturn", controllers.routes.UnauthorisedController.onPageLoad().url)
    )
  }

  def viewModel(
    srn: Srn,
    schemeName: String,
    startDate: LocalDate,
    endDate: LocalDate
  ): PageViewModel[TaskListViewModel] = {

    val viewmodel = TaskListViewModel(
      schemeDetailsSection(srn, schemeName),
      membersSection(srn, schemeName),
      memberPaymentsSection(srn),
      loansSection(srn, schemeName),
      sharesSection(srn),
      landOrPropertySection(srn),
      bondsSection(srn),
      otherAssetsSection(srn),
      declarationSection
    )

    val items = viewmodel.sections.toList.flatMap(_.items.fold(_ => Nil, _.toList))
    val completed = items.count(_.status == Completed)
    val total = items.length

    PageViewModel(
      Message("nonsipp.tasklist.title", startDate.show, endDate.show),
      Message("nonsipp.tasklist.heading", startDate.show, endDate.show),
      viewmodel
    ).withDescription(
      Heading2("nonsipp.tasklist.subheading") ++
        ParagraphMessage(Message("nonsipp.tasklist.description", completed, total))
    )
  }
}
