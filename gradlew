#!/bin/sh
# Gradle wrapper script
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'
JAVA_HOME="${JAVA_HOME:-}"
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

set_up_classpath() {
    CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
}

# For Windows compatibility reminder
if [ -z "$JAVA_HOME" ]; then
    JAVACMD=java
else
    JAVACMD="$JAVA_HOME/bin/java"
fi

exec "$JAVACMD" $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS \
    "-Dorg.gradle.appname=$APP_BASE_NAME" \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
