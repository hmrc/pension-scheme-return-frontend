package views

import forms.{RadioListFormProvider, YesNoPageFormProvider}
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.test.FakeRequest
import utils.BaseSpec
import views.html.RadioListView

class RadioListViewSpec extends BaseSpec with ScalaCheckPropertyChecks with HtmlHelper {

  runningApplication { implicit app =>

    implicit val request  = FakeRequest()

    val view = injected[RadioListView]

    "RadioListView" should {

      val requiredKey = "required"
//      val invalidKey = "invalid"

      val radioListForm = new RadioListFormProvider()(requiredKey)

      "have a title" in {

        forAll(radioListViewModelGen) { viewmodel =>

          title(view(radioListForm, viewmodel)) must startWith(viewmodel.title.toMessage)
        }
      }

      "have a heading" in {

        forAll(radioListViewModelGen) { viewmodel =>

          h1(view(radioListForm, viewmodel)) mustBe viewmodel.heading.toMessage
        }
      }

      "have radio list values" in {

        forAll(radioListViewModelGen) { viewmodel =>

          radios(view(radioListForm, viewmodel)).map(_.`val`()) mustBe List("value", "text")
        }
      }

      "have radio button labels" in {

        forAll(yesNoPageViewModelGen) { viewmodel =>

          labels(view(yesNoForm, viewmodel)) must contain allElementsOf List(messages("site.yes"), messages("site.no"))
        }
      }

      "have form" in {

        forAll(radioListViewModelGen) { viewmodel =>

          form(view(radioListForm, viewmodel)).attr("method") mustBe viewmodel.onSubmit.method
          form(view(radioListForm, viewmodel)).attr("action") mustBe viewmodel.onSubmit.url
        }
      }

      "have error summary" in {

        forAll(radioListViewModelGen) { viewmodel =>

          val invalidForm = radioListForm.bind(Map("value" -> ""))
          errorSummary(view(invalidForm, viewmodel)).text() must include(requiredKey)
        }
      }

      "have error message" in {

        forAll(radioListViewModelGen) { viewmodel =>

          val invalidForm = radioListForm.bind(Map("value" -> ""))
          errorMessage(view(invalidForm, viewmodel)).text() must include(requiredKey)
        }
      }
    }
  }
}
