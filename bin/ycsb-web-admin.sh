#!/bin/bash

AppName=ycsb-job-admin-1.0.0.jar

export SERVER_PORT="8888"
export DB_HOST="127.0.0.1"
export DB_PORT="3306"
export DB_NAME="ycsb_web"
export DB_USER="leo"
export DB_PASSWORD="Yyf5211314!"
export MAIL_HOST="smtp.aliyun.com"
export MAIL_PORT="465"
export MAIL_USER="weixiao.me@aliyun.com"
export MAIL_FROM="weixiao.me@aliyun.com"
export MAIL_PASSWD="your email password"

#JVM参数
JVM_OPTS="-Dname=$AppName  -Duser.timezone=Asia/Shanghai -Xms512M -Xmx512M -XX:PermSize=256M -XX:MaxPermSize=512M -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDateStamps  -XX:+PrintGCDetails -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC -XX:+UseParallelOldGC"
APP_HOME=`pwd`
LOG_PATH=$APP_HOME/logs/ycsb-job-admin
# 设置日志输出目录
export LOGGING_PATH=$LOG_PATH

if [ ! -d "$LOG_PATH" ]; then
  mkdir -p $LOG_PATH
fi

if [ "$1" = "" ];
then
    echo -e "\033[0;31m 未输入操作名 \033[0m  \033[0;34m {start|stop|restart|status} \033[0m"
    exit 1
fi

if [ "$AppName" = "" ];
then
    echo -e "\033[0;31m 未输入应用名 \033[0m"
    exit 1
fi

function start()
{
    PID=`ps -ef |grep java|grep $AppName|grep -v grep|awk '{print $2}'`

	if [ x"$PID" != x"" ]; then
	    echo "$AppName is running..."
	    echo "Please visit the address http://127.0.0.1:$SERVER_PORT/ycsb-job-admin"
	else
		nohup java -jar  $JVM_OPTS $AppName > /dev/null 2>&1 &
		echo "Start $AppName success ..."
		echo "Please visit the address http://127.0.0.1:$SERVER_PORT/ycsb-job-admin"
	fi
}

function stop()
{
    echo "Stop $AppName"

	PID=""
	query(){
		PID=`ps -ef |grep java|grep $AppName|grep -v grep|awk '{print $2}'`
	}

	query
	if [ x"$PID" != x"" ]; then
		kill -TERM $PID
		echo "$AppName (pid:$PID) exiting..."
		while [ x"$PID" != x"" ]
		do
			sleep 1
			query
		done
		echo "$AppName exited."
	else
		echo "$AppName already stopped."
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
    PID=`ps -ef |grep java|grep $AppName|grep -v grep|wc -l`
    if [ $PID != 0 ];then
        echo "$AppName is running..."
        echo "Please visit the address http://127.0.0.1:$SERVER_PORT/ycsb-job-admin"
    else
        echo "$AppName is not running..."
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