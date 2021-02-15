#!/bin/bash

# 设置ycsb admin所需的一些环境变量
export YCSB_ADMIN_SERVER_PORT="8888"
export YCSB_ADMIN_DB_HOST="127.0.0.1"
export YCSB_ADMIN_DB_PORT="3306"
export YCSB_ADMIN_DB_NAME="ycsb_web"
export YCSB_ADMIN_DB_USER="leo"
export YCSB_ADMIN_DB_PASSWORD="Yyf5211314!"
export YCSB_ADMIN_MAIL_HOST="smtp.aliyun.com"
export YCSB_ADMIN_MAIL_PORT="465"
export YCSB_ADMIN_MAIL_USER="weixiao.me@aliyun.com"
export YCSB_ADMIN_MAIL_FROM="weixiao.me@aliyun.com"
export YCSB_ADMIN_MAIL_PASSWD="your email password"

# 设置ycsb executor所需的一些环境变量
export YCSB_EXECUTOR_SERVER_PORT=8889
export YCSB_ADMIN_ADDR="http://127.0.0.1:8888/ycsb-job-admin"
export YCSB_EXECUTOR_NAME="ycsb-job-executor"
export YCSB_EXECUTOR_ADDR="http://127.0.0.1:9999/"
export YCSB_EXECUTOR_IP="127.0.0.1"
export YCSB_EXECUTOR_PORT="9999"

echo "the env of ycsb-web-admin and ycsb-web-executor seated up successfully."