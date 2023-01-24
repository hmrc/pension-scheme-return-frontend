package controllers

import models.NormalMode
import navigation.{FakeNavigator, Navigator}
import play.api.Application
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.bind
import play.api.mvc.{Call, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import viewmodels.models.ContentPageViewModel
import views.html.ContentPageView

class StartPageControllerSpec extends ControllerBaseSpec {

  def onwardRoute = Call("GET", "/foo")

  lazy val onPageLoad = routes.StartPageController.onPageLoad(NormalMode)
  lazy val onSubmit = routes.StartPageController.onSubmit(NormalMode)

  def view[A](viewModel: ContentPageViewModel)(implicit app: Application, request: Request[A]) =
    injected[ContentPageView](app)

  "StartPageController" should {

    "return OK and the correct view for a GET" in runningApp { implicit app =>

        val request = FakeRequest(GET, onPageLoad)

        val result = route(app, request).value

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(StartPageController.viewModel(NormalMode))(app, request).toString
      }
    }

    "redirect to the next page" in {

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )
          .build()

      running(application) {
        val request =
          FakeRequest(GET, onSubmit)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }
}