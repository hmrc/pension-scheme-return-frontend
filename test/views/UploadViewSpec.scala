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

package views

import forms.mappings.Mappings
import play.api.test.FakeRequest
import views.html.UploadView

class UploadViewSpec extends ViewSpec with Mappings {

  runningApplication { implicit app =>
    val view = injected[UploadView]

    implicit val request = FakeRequest()

    "UploadView" - {
      act.like(renderTitle(uploadViewModelGen(false))(view(_), _.title.key))
      act.like(renderHeading(uploadViewModelGen(false))(view(_), _.heading))
      act.like(renderForm(uploadViewModelGen(false))(view(_), _.onSubmit))
      act.like(renderUpload(uploadViewModelGen(false))(view(_)))
      act.like(renderErrors(uploadViewModelGen(true))(view(_), _.error.value))
      act.like(renderInset(uploadViewModelGen(true))(view(_), _.acceptedFileType).updateName(_ + " AcceptedFileType"))
      act.like(renderInset(uploadViewModelGen(true))(view(_), _.maxFileSize).updateName(_ + " MaxFileSize"))
      act.like(renderDetails(uploadViewModelGen(false))(view(_), vm => messageKey(vm.detailsContent)))
      act.like(renderSaveAndContinueButton(uploadViewModelGen(false))(view(_)))
    }
  }
}
