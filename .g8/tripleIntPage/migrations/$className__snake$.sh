#!/bin/bash

set -e

echo ""
echo "Applying migration $className;format="snake"$"

echo "Adding routes to conf/app.routes"

echo -en "\n\n" >> ../conf/app.routes
echo "GET        /:srn/$urlPath$                        controllers.$className$Controller.onPageLoad(srn: Srn, mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /:srn/$urlPath$                        controllers.$className$Controller.onSubmit(srn: Srn, mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /:srn/change-$urlPath$                 controllers.$className$Controller.onPageLoad(srn: Srn, mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /:srn/change-$urlPath$                 controllers.$className$Controller.onSubmit(srn: Srn, mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo -en "\n\n" >> ../conf/messages.en
echo "$className;format="decap"$.title = $title$" >> ../conf/messages.en
echo "$className;format="decap"$.heading = $heading$" >> ../conf/messages.en
echo "$className;format="decap"$.field1 = $field1Label$"  >> ../conf/messages.en
echo "$className;format="decap"$.field1.error.required = $field1ErrorRequired$"  >> ../conf/messages.en
echo "$className;format="decap"$.field1.error.wholeNumber = $field1ErrorWholeNumber$"  >> ../conf/messages.en
echo "$className;format="decap"$.field1.error.nonNumber = $field1ErrorNonNumber$"  >> ../conf/messages.en
echo "$className;format="decap"$.field1.error.max = $field1MaxError$"  >> ../conf/messages.en
echo "$className;format="decap"$.field2 = $field2Label$"  >> ../conf/messages.en
echo "$className;format="decap"$.field2.error.required = $field2ErrorRequired$"  >> ../conf/messages.en
echo "$className;format="decap"$.field2.error.wholeNumber = $field2ErrorWholeNumber$"  >> ../conf/messages.en
echo "$className;format="decap"$.field2.error.nonNumber = $field2ErrorNonNumber$"  >> ../conf/messages.en
echo "$className;format="decap"$.field2.error.max = $field2MaxError$"  >> ../conf/messages.en
echo "$className;format="decap"$.field3 = $field3Label$"  >> ../conf/messages.en
echo "$className;format="decap"$.field3.error.required = $field3ErrorRequired$"  >> ../conf/messages.en
echo "$className;format="decap"$.field3.error.wholeNumber = $field3ErrorWholeNumber$"  >> ../conf/messages.en
echo "$className;format="decap"$.field3.error.nonNumber = $field3ErrorNonNumber$"  >> ../conf/messages.en
echo "$className;format="decap"$.field3.error.max = $field3MaxError$"  >> ../conf/messages.en


echo "Add to navigator"
# prepend before final pattern in navigator match statement
sed -i.bu \$'/[ ]*case _[ ]*=> _ => routes.IndexController.onPageLoad/i\\\\\n    case page @ $className;format="cap"$Page(srn) => {\
      case _ => routes.UnauthorisedController.onPageLoad\
    }\\\\\n' ../app/navigation/Navigator.scala
rm ../app/navigation/Navigator.scala.bu

echo "Migration $className;format="snake"$ completed"
