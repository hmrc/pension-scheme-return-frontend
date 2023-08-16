#!/bin/bash

echo ""
echo "Applying migration $className;format="snake"$"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
$if(index.empty)$
echo "GET        /:srn/$urlPath$                        controllers.$className$Controller.onPageLoad(srn: Srn, mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /:srn/$urlPath$                        controllers.$className$Controller.onSubmit(srn: Srn, mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /:srn/change-$urlPath$                 controllers.$className$Controller.onPageLoad(srn: Srn, mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /:srn/change-$urlPath$                 controllers.$className$Controller.onSubmit(srn: Srn, mode: Mode = CheckMode)" >> ../conf/app.routes
$else$
echo "GET        /:srn/$urlPath$                        controllers.$className$Controller.onPageLoad(srn: Srn, index: $index$ = 1, mode: Mode = NormalMode)" >> ../conf/app.routes
echo "GET        /:srn/$urlPath$/:index                 controllers.$className$Controller.onPageLoad(srn: Srn, index: $index$, mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /:srn/$urlPath$                        controllers.$className$Controller.onSubmit(srn: Srn, index: $index$ = 1, mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /:srn/$urlPath$/:index                 controllers.$className$Controller.onSubmit(srn: Srn, index: $index$, mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /:srn/change-$urlPath$                 controllers.$className$Controller.onPageLoad(srn: Srn, index: $index$ = 1, mode: Mode = CheckMode)" >> ../conf/app.routes
echo "GET        /:srn/change-$urlPath$/:index          controllers.$className$Controller.onPageLoad(srn: Srn, index: $index$, mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /:srn/change-$urlPath$                 controllers.$className$Controller.onSubmit(srn: Srn, index: $index$ = 1, mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /:srn/change-$urlPath$/:index          controllers.$className$Controller.onSubmit(srn: Srn, index: $index$, mode: Mode = CheckMode)" >> ../conf/app.routes
$endif$

echo "Adding messages to conf.messages"

echo "" >> ../conf/messages.en
echo "$className;format="decap"$.title = $title$" >> ../conf/messages.en
echo "$className;format="decap"$.heading = $heading$" >> ../conf/messages.en
echo "$className;format="decap"$.field1.label = $field1Label$" >> ../conf/messages.en
echo "$className;format="decap"$.field1.error.required = $field1ErrorRequired$" >> ../conf/messages.en
echo "$className;format="decap"$.field2.label = $field2Label$" >> ../conf/messages.en
echo "$className;format="decap"$.field2.error.required = $field2ErrorRequired$" >> ../conf/messages.en
echo "$className;format="decap"$.field3.label = $field3Label$" >> ../conf/messages.en
echo "$className;format="decap"$.field3.error.required = $field3ErrorRequired$" >> ../conf/messages.en

echo "Migration $className;format="snake"$ completed"
