package viewmodels.models

import play.api.mvc.Call
import viewmodels.DisplayMessage
import viewmodels.DisplayMessage.{BlockMessage, InlineMessage, Message}

case class PageViewModel[A](
  title: Message,
  heading: InlineMessage,
  description: Option[BlockMessage],
  inset: Option[DisplayMessage],
  page: A,
  refresh: Boolean,
  buttonText: Message,
  onSubmit: Call
) {

  def withInset(message: DisplayMessage): PageViewModel[A] =
    copy(inset = Some(message))

  def withDescription(message: Option[BlockMessage]): PageViewModel[A] =
    copy(description = message)

  def withDescription(message: BlockMessage): PageViewModel[A] =
    withDescription(Some(message))

  def refreshPage: PageViewModel[A] =
    copy(refresh = true)

  def withButtonText(message: Message): PageViewModel[A] =
    copy(buttonText = message)
}

object PageViewModel {

  def apply[A](
    title: Message,
    heading: InlineMessage,
    page: A,
    onSubmit: Call
  ): PageViewModel[A] =
    PageViewModel(
      title,
      heading,
      None,
      None,
      page,
      refresh = false,
      Message("site.saveAndContinue"),
      onSubmit
    )
}