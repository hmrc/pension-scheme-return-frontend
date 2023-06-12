#!/bin/bash

set -e

echo ""
echo "Applying migration $className;format="snake"$"

echo "Adding routes to conf/app.routes"

echo -en "\n\n" >> ../conf/app.routes
echo "GET        /:srn/$urlPath$                        controllers.$className$Controller.onPageLoad(srn: Srn, mode: Mode = NormalMode)" >> ../conf/app.routes
echo "GET        /:srn/submit-$urlPath$                 controllers.$className$Controller.onSubmit(srn: Srn, mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /:srn/change-$urlPath$                 controllers.$className$Controller.onPageLoad(srn: Srn, mode: Mode = CheckMode)" >> ../conf/app.routes
echo "GET        /:srn/change-submit-$urlPath$          controllers.$className$Controller.onSubmit(srn: Srn, mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Migration $className;format="snake"$ completed"
