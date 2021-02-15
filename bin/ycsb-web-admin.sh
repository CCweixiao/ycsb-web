#!/bin/bash

SCRIPT_DIR=$(dirname "$0" 2>/dev/null)
[ -z "$YCSB_WEB_HOME" ] && YCSB_WEB_HOME=$(cd "$SCRIPT_DIR/.." || exit; pwd)

echo "the home dir of ycsb web application is $YCSB_WEB_HOME"

CLASSPATH=
YCSB_APP_NAME="YcsbWebJobAdminApplication"
YCSB_WEB_ADMIN_CLASS="com.leo.ycsb.job.admin.YcsbWebJobAdminApplication"
YCSB_APP_ARGS=" --spring.config.location=file://$YCSB_WEB_HOME/conf/ycsb-job-admin.properties --logging.config=$YCSB_WEB_HOME/conf/logback-spring.xml --spring.web.resources.static-locations=file://$YCSB_WEB_HOME/ycsb-webapps/static/ --spring.freemarker.templateLoaderPath=file://$YCSB_WEB_HOME/ycsb-webapps/templates/"

# 设置系统运行所需的一些环境变量
if [ -r "$YCSB_WEB_HOME/bin/ycsb-web-env.sh" ]; then
  . "$YCSB_WEB_HOME/bin/ycsb-web-env.sh"
fi

# Attempt to find the available JAVA, if JAVA_HOME not set
if [ -z "$JAVA_HOME" ]; then
  JAVA_PATH=$(which java 2>/dev/null)
  if [ "x$JAVA_PATH" != "x" ]; then
    JAVA_HOME=$(dirname "$(dirname "$JAVA_PATH" 2>/dev/null)")
  fi
fi

# If JAVA_HOME still not set, error
if [ -z "$JAVA_HOME" ]; then
  echo "[ERROR] Java executable not found. Exiting."
  exit 1;
else
  echo "JAVA_HOME in current OS is $JAVA_HOME"
fi


# Add conf dir to classpath
if [ -z "$CLASSPATH" ] ; then
  CLASSPATH="$YCSB_WEB_HOME/conf"
else
  CLASSPATH="$CLASSPATH:$YCSB_WEB_HOME/conf"
fi

# Core libraries
for f in "$YCSB_WEB_HOME"/lib/*.jar ; do
  if [ -r "$f" ] ; then
    CLASSPATH="$CLASSPATH:$f"
  fi
done

for f in "$YCSB_WEB_HOME"/lib/admin/*.jar ; do
  if [ -r "$f" ] ; then
    CLASSPATH="$CLASSPATH:$f"
  fi
done

#JVM参数
JVM_OPTS="-Dname=$YCSB_APP_NAME -Duser.timezone=Asia/Shanghai -Xms512M -Xmx512M -XX:PermSize=256M -XX:MaxPermSize=512M -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDateStamps  -XX:+PrintGCDetails -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC -XX:+UseParallelOldGC"
LOG_PATH=$YCSB_WEB_HOME/logs/ycsb-job-admin
# 设置日志输出目录和日志文件名
export YCSB_ADMIN_LOG_PATH=$LOG_PATH
export YCSB_ADMIN_LOG_NAME=ycsb-job-admin.log

if [ ! -d "$LOG_PATH" ]; then
  mkdir -p $LOG_PATH
fi

if [ "$1" = "" ];
then
    echo -e "\033[0;31m 未输入操作名 \033[0m  \033[0;34m {start|stop|restart|status} \033[0m"
    exit 1
fi

if [ "$YCSB_APP_NAME" = "" ];
then
    echo -e "\033[0;31m 未输入应用名 \033[0m"
    exit 1
fi

function start()
{
    PID=`ps -ef |grep java|grep $YCSB_APP_NAME|grep -v grep|awk '{print $2}'`

	if [ x"$PID" != x"" ]; then
	    echo "$YCSB_APP_NAME is running..."
	    echo "Please visit the address http://127.0.0.1:$YCSB_ADMIN_SERVER_PORT/ycsb-job-admin"
	else
		echo "$JAVA_OPTS -classpath "$CLASSPATH" $YCSB_WEB_ADMIN_CLASS $YCSB_APP_ARGS"
		nohup "$JAVA_HOME/bin/java" $JAVA_OPTS -classpath "$CLASSPATH" $YCSB_WEB_ADMIN_CLASS $YCSB_APP_ARGS > /dev/null 2>&1 &
		echo "Start $YCSB_APP_NAME success ..."
		echo "Please visit the address http://127.0.0.1:$YCSB_ADMIN_SERVER_PORT/ycsb-job-admin"
	fi
}

function stop()
{
    echo "Stop $YCSB_APP_NAME"

	PID=""
	query(){
		PID=`ps -ef |grep java|grep $YCSB_APP_NAME|grep -v grep|awk '{print $2}'`
	}

	query
	if [ x"$PID" != x"" ]; then
		kill -TERM $PID
		echo "$YCSB_APP_NAME (pid:$PID) exiting..."
		while [ x"$PID" != x"" ]
		do
			sleep 1
			query
		done
		echo "$YCSB_APP_NAME exited."
	else
		echo "$YCSB_APP_NAME already stopped."
	fi
}

function restart()
{
    stop
    sleep 2
    start
}

function status()
{
    PID=`ps -ef |grep java|grep $YCSB_APP_NAME|grep -v grep|wc -l`
    if [ $PID != 0 ];then
        echo "$YCSB_APP_NAME is running..."
        echo "Please visit the address http://127.0.0.1:$YCSB_ADMIN_SERVER_PORT/ycsb-job-admin"
    else
        echo "$YCSB_APP_NAME is not running..."
    fi
}

case $1 in
    start)
    start;;
    stop)
    stop;;
    restart)
    restart;;
    status)
    status;;
    *)

esac