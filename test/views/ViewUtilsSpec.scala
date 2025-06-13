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

package views

import play.api.test.FakeRequest
import play.api.test.Helpers.stubMessagesApi
import forms.mappings.Mappings.text
import org.scalatest.matchers.must.Matchers
import controllers.{ControllerBaseSpec, ControllerBehaviours}
import models._
import play.api.i18n.Messages
import play.api.data.Form
import viewmodels.DisplayMessage.Message
import viewmodels.models.PaginatedViewModel

class ViewUtilsSpec extends ControllerBaseSpec with ControllerBehaviours with Matchers {

  private val onwardRoute = controllers.routes.UnauthorisedController.onPageLoad()
  implicit val messages: Messages = stubMessagesApi().preferred(FakeRequest())
  private val form = Form("value" -> text("custom.error"))
  private val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()
  private val label = Message("label")
  private val title: String = "title"
  private val postfixKey = "site.title.postfix"

  "paginatedTitle" - {
    "must contain pagination text" - {
      "when on first page" in {
        running(application) {
          val actualTitle = ViewUtils.paginatedTitle(
            Some(
              PaginatedViewModel(
                label = label,
                pagination = Pagination(
                  currentPage = 1,
                  pageSize = 2,
                  totalSize = 3,
                  _ => onwardRoute
                )
              )
            ),
            form,
            title
          )
          actualTitle must include(postfixKey)
        }
      }

      "when on second page" in {
        running(application) {
          val actualTitle = ViewUtils.paginatedTitle(
            Some(
              PaginatedViewModel(
                label = label,
                pagination = Pagination(
                  currentPage = 2,
                  pageSize = 2,
                  totalSize = 3,
                  _ => onwardRoute
                )
              )
            ),
            form,
            title
          )
          actualTitle must include(postfixKey)
        }
      }
    }

    "must not contain pagination text" - {
      "when on there is no pagination" in {
        running(application) {
          val actualTitle = ViewUtils.paginatedTitle(
            Some(
              PaginatedViewModel(
                label = label,
                pagination = Pagination(
                  currentPage = 1,
                  pageSize = 2,
                  totalSize = 2,
                  _ => onwardRoute
                )
              )
            ),
            form,
            title
          )
          (actualTitle must not).include(postfixKey)
        }
      }
    }
  }

  "paginatedTitleNoForm" - {
    "must contain pagination text" - {
      "when on first page" in {
        running(application) {
          val actualTitle = ViewUtils.paginatedTitleNoForm(
            Some(
              PaginatedViewModel(
                label = label,
                pagination = Pagination(
                  currentPage = 1,
                  pageSize = 2,
                  totalSize = 3,
                  _ => onwardRoute
                )
              )
            ),
            title
          )
          actualTitle must include(postfixKey)
        }
      }

      "when on second page" in {
        running(application) {
          val actualTitle = ViewUtils.paginatedTitleNoForm(
            Some(
              PaginatedViewModel(
                label = label,
                pagination = Pagination(
                  currentPage = 2,
                  pageSize = 2,
                  totalSize = 3,
                  _ => onwardRoute
                )
              )
            ),
            title
          )
          actualTitle must include(postfixKey)
        }
      }
    }

    "must not contain pagination text" - {
      "when on there is no pagination" in {
        running(application) {
          val actualTitle = ViewUtils.paginatedTitleNoForm(
            Some(
              PaginatedViewModel(
                label = label,
                pagination = Pagination(
                  currentPage = 1,
                  pageSize = 2,
                  totalSize = 2,
                  _ => onwardRoute
                )
              )
            ),
            title
          )
          (actualTitle must not).include(postfixKey)
        }
      }
    }
  }
}
