#!/bin/bash

$! Generic !$
echo ""
echo "Applying migration $className;format="snake"$"

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
$if(!hint.empty)$
echo "$className;format="decap"$.hint = $hint$" >> ../conf/messages.en
$endif$
echo "$className;format="decap"$.error.required.day = $errorMissingDay$" >> ../conf/messages.en
echo "$className;format="decap"$.error.required.month = $errorMissingMonth$" >> ../conf/messages.en
echo "$className;format="decap"$.error.required.year = $errorMissingYear$" >> ../conf/messages.en
echo "$className;format="decap"$.error.required.two = $errorMissingTwo$" >> ../conf/messages.en
echo "$className;format="decap"$.error.required.all = $errorMissingAll$" >> ../conf/messages.en
echo "$className;format="decap"$.error.date.before = $errorBefore$" >> ../conf/messages.en
echo "$className;format="decap"$.error.date.after = $errorAfter$" >> ../conf/messages.en
echo "$className;format="decap"$.error.invalid.date = $errorInvalidDate$" >> ../conf/messages.en
echo "$className;format="decap"$.error.invalid.chars = $errorInvalidChars$" >> ../conf/messages.en


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

echo "Migration $className;format="snake"$ completed"
$! Generic end !$
