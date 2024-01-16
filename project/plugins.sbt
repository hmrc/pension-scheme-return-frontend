resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"

resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("uk.gov.hmrc"         % "sbt-auto-build"         % "3.19.0")
addSbtPlugin("uk.gov.hmrc"         % "sbt-distributables"     % "2.4.0")
addSbtPlugin("com.typesafe.play"   % "sbt-plugin"             % "2.9.1")
addSbtPlugin("org.scoverage"       % "sbt-scoverage"          % "2.0.9")
addSbtPlugin("org.scalameta"       % "sbt-scalafmt"           % "2.4.2")
addSbtPlugin("io.github.irundaia"  % "sbt-sassify"            % "1.5.2")
addSbtPlugin("net.ground5hark.sbt" % "sbt-concat"             % "0.2.0")
addSbtPlugin("com.typesafe.sbt"    % "sbt-uglify"             % "2.0.0")
addSbtPlugin("com.typesafe.sbt"    % "sbt-digest"             % "1.1.4")
