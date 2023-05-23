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

package services

import akka.stream.scaladsl.Source
import akka.util.ByteString
import connectors.UpscanConnector
import models.UploadStatus.UploadStatus
import models._
import repositories.UploadRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Clock, Instant}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UploadService @Inject()(
  upscanConnector: UpscanConnector,
  repository: UploadRepository,
  clock: Clock
)(implicit ec: ExecutionContext) {

  def initiateUpscan(callBackUrl: String, successRedirectUrl: String, failureRedirectUrl: String)(
    implicit hc: HeaderCarrier
  ): Future[UpscanInitiateResponse] =
    upscanConnector.initiate(callBackUrl, successRedirectUrl, failureRedirectUrl)

  def registerUploadRequest(key: UploadKey, fileReference: Reference): Future[Unit] =
    for {
      _ <- repository.remove(key)
      _ <- repository.insert(UploadDetails(key, fileReference, UploadStatus.InProgress, Instant.now(clock)))
    } yield ()

  def registerUploadResult(reference: Reference, uploadStatus: UploadStatus): Future[Unit] =
    repository.updateStatus(reference, uploadStatus)

  def getUploadResult(key: UploadKey): Future[Option[UploadStatus]] =
    repository.getUploadDetails(key).map(_.map(_.status))

  def stream(downloadUrl: String)(implicit hc: HeaderCarrier): Future[Source[ByteString, _]] =
    upscanConnector.download(downloadUrl).map(_.bodyAsSource)

  def saveValidatedUpload(uploadKey: UploadKey, uploadResult: Upload): Future[Unit] =
    repository.setUploadResult(uploadKey, uploadResult)
}
