#!/bin/bash

echo ""
echo "Applying migration $className;format="snake"$"

$! Generic !$
$if(directory.empty)$
DIR=../conf/app.routes
PACKAGE="controllers.nonsipp"
$else$
DIR=../conf/$directory$.routes
PACKAGE="controllers.nonsipp.$directory$"
$endif$

echo "Adding routes to conf/app.routes"

echo -en "\n\n" >> ../conf/app.routes
$if(index.empty)$
echo "GET        /:srn/$urlPath$                        \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, mode: Mode = NormalMode)" >> \$DIR
echo "POST       /:srn/$urlPath$                        \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, mode: Mode = NormalMode)" >> \$DIR

echo "GET        /:srn/change-$urlPath$                 \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, mode: Mode = CheckMode)" >> \$DIR
echo "POST       /:srn/change-$urlPath$                 \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, mode: Mode = CheckMode)" >> \$DIR
$else$
  $if(secondaryIndex.empty)$
  echo "GET        /:srn/$urlPath$/:index                 \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, index: $index$, mode: Mode = NormalMode)" >> \$DIR
  echo "POST       /:srn/$urlPath$/:index                 \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, index: $index$, mode: Mode = NormalMode)" >> \$DIR

  echo "GET        /:srn/change-$urlPath$/:index          \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, index: $index$, mode: Mode = CheckMode)" >> \$DIR
  echo "POST       /:srn/change-$urlPath$/:index          \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, index: $index$, mode: Mode = CheckMode)" >> \$DIR
  $else$
  echo "GET        /:srn/$urlPath$/:index/:secondaryIndex                 \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, index: $index$, secondaryIndex: $secondaryIndex$, mode: Mode = NormalMode)" >> \$DIR
  echo "POST       /:srn/$urlPath$/:index/:secondaryIndex                 \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, index: $index$, secondaryIndex: $secondaryIndex$, mode: Mode = NormalMode)" >> \$DIR

  echo "GET        /:srn/change-$urlPath$/:index/:secondaryIndex          \${PACKAGE}.$className$Controller.onPageLoad(srn: Srn, index: $index$, secondaryIndex: $secondaryIndex$, mode: Mode = CheckMode)" >> \$DIR
  echo "POST       /:srn/change-$urlPath$/:index/:secondaryIndex          \${PACKAGE}.$className$Controller.onSubmit(srn: Srn, index: $index$, secondaryIndex: $secondaryIndex$, mode: Mode = CheckMode)" >> \$DIR
  $endif$
$endif$
$! Generic end !$

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

$! Generic !$
DIR="$directory$"

if [ -z \$DIR ]; then
  echo "DIR empty, skipping"
else
  echo "Add to navigator"
  $if(index.empty)$
  ../.g8/scripts/updateNavigator $className;format="cap"$Page $directory$
  $else$
    $if(secondaryIndex.empty)$
    ../.g8/scripts/updateNavigator $className;format="cap"$Page $directory$ "$index$"
    $else$
    ../.g8/scripts/updateNavigator $className;format="cap"$Page $directory$ "$index$" "$secondaryIndex$"
    $endif$
  $endif$
fi
$! Generic end !$

echo "Migration $className;format="snake"$ completed"
