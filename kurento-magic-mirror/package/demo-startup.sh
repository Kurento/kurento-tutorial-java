#!/bin/bash
# description: my-spring-boot-api start stop restart
# processname: my-spring-boot-api
# chkconfig: 234 20 80

echo "Service [$APP_NAME] - [$1]"

APP_HOME=/var/lib/kurento
APP_NAME=${project.artifactId}
APP_VERSION=${project.version}
APP=$APP_NAME-$APP_VERSION
APP_PORT=${demo.port}
CONSOLE_LOG=/var/log/kurento-media-server/$APP_NAME.log

JAVA_OPTS="-Dserver.port=$APP_PORT"
JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"

# Setup the JVM
if [ "x$JAVA" = "x" ]; then
    if [ "x$JAVA_HOME" != "x" ]; then
        JAVA="$JAVA_HOME/bin/java"
    else
        JAVA="java"
    fi
fi

# Check log directory
[ -d $(dirname $CONSOLE_LOG) ] || mkdir -p $(dirname $CONSOLE_LOG)

echo "========================================================================="
echo ""
echo "  $APP Bootstrap Environment"
echo ""
echo "  APP_HOME: $APP_HOME"
echo ""
echo "  JAVA: $JAVA $JAVA_OPTS"
echo ""
echo "========================================================================="
echo ""

function start {
    if pkill -0 -f $APP_NAME.jar > /dev/null 2>&1
    then
        echo "Service [$APP_NAME] is already running. Ignoring startup request."
    fi
    echo "Starting application $APP_NAME in port $APP_PORT..."
    nohup $JAVA $JAVA_OPTS -jar $APP_HOME/$APP_NAME.jar \
        < /dev/null > $CONSOLE_LOG 2>&1 &
}

function stop {
    if ! pkill -0 -f $APP_NAME.jar > /dev/null 2>&1
    then
        echo "Service [$APP_NAME] is not running. Ignoring shutdown request."
    fi

    pkill -f $APP_NAME.jar > /dev/null 2>&1
    echo "Service [$APP_NAME] stopped"
}

case $1 in
start)
    start
;;
stop)
    stop
;;
restart)
    stop
    start
;;
esac
exit 0
