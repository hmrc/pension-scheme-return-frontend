package controllers

import navigation.{FakeNavigator, Navigator}
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import views.html.RadioListView

class RadioListControllerSpec extends ControllerBaseSpec {

  def onwardRoute = Call("GET", "/foo")

  lazy val onPageLoad = routes.RadioListController.onPageLoad(srn).url
  lazy val onSubmit = routes.RadioListController.onSubmit(srn).url

  "RadioListController" should {

    "return OK and the correct view for a GET" in runningApplication { implicit app =>

      val view = injected[RadioListView]
      val request = FakeRequest(GET, onPageLoad)

      val result = route(app, request).value
      val expectedView = view(RadioListController.viewModel(srn))(request, createMessages(app))

      status(result) mustEqual OK
      contentAsString(result) mustEqual expectedView.toString
    }

    "redirect to the next page" in {

      val fakeNavigatorApplication =
        applicationBuilder()
          .overrides(
            bind[Navigator].toInstance(new FakeNavigator(onwardRoute))
          )

      running(_ => fakeNavigatorApplication) { app =>

        val request = FakeRequest(GET, onSubmit)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual onwardRoute.url
      }
    }
  }
}