#!/bin/bash

set -e

echo ""
echo "Applying migration $className;format="snake"$"

echo "Adding routes to conf/app.routes"

echo -en "\n\n" >> ../conf/app.routes
$if(index.empty)$
echo "GET        /:srn/$urlPath$                        controllers.$className$Controller.onPageLoad(srn: Srn, mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /:srn/$urlPath$                        controllers.$className$Controller.onSubmit(srn: Srn, mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /:srn/change-$urlPath$                 controllers.$className$Controller.onPageLoad(srn: Srn, mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /:srn/change-$urlPath$                 controllers.$className$Controller.onSubmit(srn: Srn, mode: Mode = CheckMode)" >> ../conf/app.routes
$else$
echo "GET        /:srn/$urlPath$/:index                 controllers.$className$Controller.onPageLoad(srn: Srn, index: $index$, mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /:srn/$urlPath$/:index                 controllers.$className$Controller.onSubmit(srn: Srn, index: $index$, mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /:srn/change-$urlPath$/:index          controllers.$className$Controller.onPageLoad(srn: Srn, index: $index$, mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /:srn/change-$urlPath$/:index          controllers.$className$Controller.onSubmit(srn: Srn, index: $index$, mode: Mode = CheckMode)" >> ../conf/app.routes
$endif$

echo "Adding messages to conf.messages"

echo -en "\n\n" >> ../conf/messages.en
echo "$className;format="decap"$.title = $title$" >> ../conf/messages.en
echo "$className;format="decap"$.heading = $heading$" >> ../conf/messages.en
echo "$className;format="decap"$.error.required = $errorRequired$"  >> ../conf/messages.en
echo "$className;format="decap"$.error.invalid = $errorInvalid$"  >> ../conf/messages.en
echo "$className;format="decap"$.error.tooLong = $errorTooLong$"  >> ../conf/messages.en

echo "Add to navigator"

echo "case $className;format="cap"$Page(srn) => controllers.routes.UnauthorisedController.onPageLoad()"

echo "Migration $className;format="snake"$ completed"
