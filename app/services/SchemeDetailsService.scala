package services

import connectors.SchemeDetailsConnector
import models.{MinimalSchemeDetails, PensionSchemeId}
import models.SchemeId.Srn
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SchemeDetailsService @Inject()(schemeDetailsConnector: SchemeDetailsConnector) {


  def getMinimalSchemeDetails(id: PensionSchemeId, srn: Srn)(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Option[MinimalSchemeDetails]] = {

    id.fold(
      schemeDetailsConnector.listSchemeDetails(_),
      schemeDetailsConnector.listSchemeDetails(_)
    ).map { allDetails =>
      allDetails.schemeDetails.find(_.srn == srn.value)
    }
  }
}