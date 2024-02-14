#!/bin/bash

set -e

$! Generic !$
echo ""
echo "Applying migration $className;format="cap"$"

../.g8/scripts/updateRoutes "$className;format="cap"$" "$urlPath$" $if(!directory.empty)$"$directory$"$endif$ $if(!index.empty)$"$index$"$endif$ $if(!secondaryIndex.empty)$"$secondaryIndex$"$endif$
$! Generic end !$

echo "Adding messages to conf.messages"

echo -en "\n\n" >> ../conf/messages.en
echo "$className;format="decap"$.title = $title$" >> ../conf/messages.en
echo "$className;format="decap"$.heading = $heading$" >> ../conf/messages.en
$if(!hint.empty)$
echo "$className;format="decap"$.hint = $hint$" >> ../conf/messages.en
$endif$
echo "$className;format="decap"$.error.required = $errorRequired$" >> ../conf/messages.en
echo "$className;format="decap"$.yes.conditional = $yesHeading$"  >> ../conf/messages.en
echo "$className;format="decap"$.yes.conditional.error.required = $yesErrorRequired$"  >> ../conf/messages.en
echo "$className;format="decap"$.yes.conditional.error.invalid = $yesErrorInvalid$"  >> ../conf/messages.en
echo "$className;format="decap"$.yes.conditional.error.length = $yesErrorLength$"  >> ../conf/messages.en
echo "$className;format="decap"$.no.conditional = $noHeading$"  >> ../conf/messages.en
echo "$className;format="decap"$.no.conditional.error.required = $noErrorRequired$"  >> ../conf/messages.en
echo "$className;format="decap"$.no.conditional.error.invalid = $noErrorInvalid$"  >> ../conf/messages.en
echo "$className;format="decap"$.no.conditional.error.length = $noErrorLength$"  >> ../conf/messages.en

$! Generic !$

if [ -z "$directory$" ]; then
  echo "\$directory\$ empty, skipping"
else
  ../.g8/scripts/updateNavigator "$className;format="cap"$Page" "$directory$" $if(!index.empty)$"$index$"$endif$ $if(!secondaryIndex.empty)$"$secondaryIndex$"$endif$
fi

echo "Migration $className;format="cap"$ completed"
$! Generic end !$
