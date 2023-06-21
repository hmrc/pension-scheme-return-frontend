#!/bin/bash

echo ""
echo "Applying migration $className;format="snake"$"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /:srn/$urlPath$                        controllers.$className$Controller.onPageLoad(srn: Srn, mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /:srn/$urlPath$                        controllers.$className$Controller.onSubmit(srn: Srn, mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /:srn/change-$urlPath$                  controllers.$className$Controller.onPageLoad(srn: Srn, mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /:srn/change-$urlPath$                  controllers.$className$Controller.onSubmit(srn: Srn, mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "$className;format="decap"$.title = $title$" >> ../conf/messages.en
echo "$className;format="decap"$.heading = $title$" >> ../conf/messages.en
echo "$className;format="decap"$.option1 = $option1msg$" >> ../conf/messages.en
echo "$className;format="decap"$.option2key = $option2msg$" >> ../conf/messages.en
echo "$className;format="decap"$.checkYourAnswersLabel = $title$" >> ../conf/messages.en
echo "$className;format="decap"$.error.required = Select $className;format="decap"$" >> ../conf/messages.en
echo "$className;format="decap"$.change.hidden = $className$" >> ../conf/messages.en

echo "Migration $className;format="snake"$ completed"
