/*
 * Copyright 2025 HM Revenue & Customs
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

package utils.nonsipp.summary

import play.api.mvc.{AnyContent, Result}
import utils.ListUtils.flip
import viewmodels.models.SummaryPageEntry.{Heading, Section, Subheading}
import uk.gov.hmrc.http.HeaderCarrier
import models.Mode
import viewmodels.DisplayMessage
import viewmodels.models._
import models.requests.DataRequest
import cats.data.EitherT
import models.SchemeId.Srn

import scala.concurrent.{ExecutionContext, Future}

trait CheckAnswersUtils[I, D] {

  def summaryDataAsync(srn: Srn, index: I, mode: Mode)(using
    DataRequest[AnyContent],
    HeaderCarrier,
    ExecutionContext
  ): Future[Either[Result, D]]

  def summaryDataAsyncT(srn: Srn, index: I, mode: Mode)(using
    DataRequest[AnyContent],
    HeaderCarrier,
    ExecutionContext
  ): EitherT[Future, Result, D] = EitherT(summaryDataAsync(srn, index, mode))

  def indexes(using request: DataRequest[AnyContent]): List[I]

  def viewModel(data: D): FormPageViewModel[CheckYourAnswersViewModel]

  def allSummaryData(srn: Srn, mode: Mode)(using
    request: DataRequest[AnyContent],
    executionContext: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Either[Result, List[D]]] =
    indexes.map(i => summaryDataAsync(srn, i, mode)).flip

  def allSummaryDataT(srn: Srn, mode: Mode)(using
    request: DataRequest[AnyContent],
    executionContext: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, Result, List[D]] = EitherT(allSummaryData(srn, mode))

  def heading: Option[DisplayMessage] = None
  def subheading(data: D): Option[DisplayMessage] = None

  def allSectionEntriesT(srn: Srn, mode: Mode)(using
    request: DataRequest[AnyContent],
    executionContext: ExecutionContext,
    hc: HeaderCarrier
  ): EitherT[Future, Result, List[SummaryPageEntry]] = allSummaryDataT(srn, mode).map { data =>
    heading.map(Heading(_)).toList ++
      data.flatMap { x =>
        subheading(x).map(Subheading(_)).toList ++
          List(Section(viewModel(x).page.toSummaryViewModel(None)))
      }
  }
}
