#!/bin/bash

set -e

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

$! Generic !$
DIR="$directory$"

if [ -z \$DIR ]; then
  echo "DIR empty, skipping"
else
  echo "Add to navigator"
  $if(index.empty)$
  ../.g8/scripts/updateNavigator $className;format="cap"$CompletedPage $directory$
  $else$
    $if(secondaryIndex.empty)$
    ../.g8/scripts/updateNavigator $className;format="cap"$CompletedPage $directory$ "$index$"
    $else$
    ../.g8/scripts/updateNavigator $className;format="cap"$CompletedPage $directory$ "$index$" "$secondaryIndex$"
    $endif$
  $endif$
fi
$! Generic end !$

echo "Migration $className;format="snake"$ completed"
