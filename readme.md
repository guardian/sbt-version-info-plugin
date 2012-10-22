sbt-version-info-plugin
=======================

This sbt plugin generates a version.txt file in the root of your classpath.
This is typically used by the
[guardian-management](https://github.com/guardian/guardian-management) manifest reporter,
which displays the content of this file.

To use:

1. Work out what released version you want to use by looking at the tags list for this repository.

2. Add the sbt-version-info-plugin to your sbt build, by creating project/project/plugins.scala that looks like:

        import sbt._

        object Plugins extends Build {
          lazy val plugins = Project("plugins", file("."))
            .dependsOn(
              uri("git://github.com/guardian/sbt-version-info-plugin.git#2.1")
            )
        }

3. The VersionInfo trait relies on a couple of system properties to obtain the build number etc from the CI server.
   To pass these in create an SBT start script that for Teamcity looks a bit like this (which by convention we call 
   sbt-tc):

       #!/bin/bash

       cat /dev/null | java -Xmx1G -XX:MaxPermSize=250m \
        -Dsbt.log.noformat=true \
        -Dbuild.number="$BUILD_NUMBER" \
        -Dbuild.vcs.number="$BUILD_VCS_NUMBER" \
        -jar sbt-launch.jar "$@"
   
   You can find an example for Hudson/Jenkins at https://github.com/philwills/sbt-version-info-minimal/blob/master/sbt