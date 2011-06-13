sbt-version-info-plugin
=======================

This sbt 0.10 plugin generates a version.txt file in the root of your classpath.
This is typically used by the
[guardian-management](https://github.com/guardian/guardian-management) manifest reporter,
which displays the content of this file.

To use:

1. Work out what released version you want to use by going to <https://github.com/guardian/guardian.github.com/tree/master/maven/repo-releases/com/gu/sbt-version-info-plugin>

2. Add the sbt-version-info-plugin to your sbt build, by creating project/plugins/build.sbt that looks like:

        resolvers ++= Seq(
            "Guardian Github Releases" at "http://guardian.github.com/maven/repo-releases",
            "Guardian Github Snapshots" at "http://guardian.github.com/maven/repo-snapshots"
            )

        libraryDependencies += "com.gu" %% "sbt-version-info-plugin" % "2.0-SNAPSHOT"


4. The VersionInfo trait relies on a couple of system properties to obtain the build number etc from TeamCity.
   To pass these in you want a sbt start script that looks a bit like this (which by convention we call sbt-tc):

       #!/bin/bash

       cat /dev/null | java -Xmx1G -XX:MaxPermSize=250m \
        -Dsbt.log.noformat=true \
        -Dbuild.number="$BUILD_NUMBER" \
        -Dbuild.vcs.number="$BUILD_VCS_NUMBER" \
        -jar sbt-launch-0.10.0.jar "$@"



    




