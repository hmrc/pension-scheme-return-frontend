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
echo -en "\n" >> ../conf/messages.en
echo "$className;format="decap"$.title = $title$" >> ../conf/messages.en
echo "$className;format="decap"$.heading = $heading$" >> ../conf/messages.en
echo "$className;format="decap"$.error.required = $errorRequired$"  >> ../conf/messages.en
echo "$className;format="decap"$.error.length = $errorLength$"  >> ../conf/messages.en
echo "$className;format="decap"$.error.invalid = $errorInvalid$"  >> ../conf/messages.en

echo "Add to navigator"
# prepend before final pattern in navigator match statement
sed -i.bu \$'/[ ]*case _[ ]*=> _ => routes.IndexController.onPageLoad/i\\\\\n    case $className;format="cap"$Page(srn) => _ => routes.UnauthorisedController.onPageLoad\\\\\n' ../app/navigation/Navigator.scala
rm ../app/navigation/Navigator.scala.bu

echo "Migration $className;format="snake"$ completed"
