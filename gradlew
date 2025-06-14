#!/bin/sh

##############################################################################
# Copyright 2015-2021 the original authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##############################################################################

# Gradle start up script for POSIX generated systems.

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
app_path=$0

# Need this for daisy-chained symlinks.
while
    APP_HOME=${app_path%"${app_path##*/}"}  # leaves a trailing /; empty if no leading path
    [ -h "$app_path" ]
do
    ls=$( ls -ld "$app_path" )
    link=${ls#*' -> '}
    case $link in             #(
      /*)   app_path=$link ;; #(
      *)    app_path=$APP_HOME$link ;;
    esac
done

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ]; then
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ]; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME"
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
fi

# Increase the maximum file descriptors if we can.
MAX_FD="maximum"

# Use the maximum available, or set MAX_FD != -1 to use that value.
case "$MAX_FD" in #(
  max*)
    MAX_FD=$( ulimit -H -n ) ||
        warn "Could not query maximum file descriptor limit"
    ;;
esac
case $MAX_FD in #(
  '' | soft) : ;; #(
  *)
    ulimit -n "$MAX_FD" ||
        warn "Could not set maximum file descriptor limit to $MAX_FD"
    ;;
esac

# Collect all arguments for the java command, stacking in reverse order:
#   * DEFAULT_JVM_OPTS, JAVA_OPTS, and GRADLE_OPTS environment variables
#   * settings from gradle.properties

# For Darwin, add options to specify how the application appears in the dock
if [ "$(uname)" = "Darwin" ]; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=Gradle\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# Setup the command line
exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" \
    org.gradle.wrapper.GradleWrapperMain "$@"