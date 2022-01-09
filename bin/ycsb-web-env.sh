#!/bin/bash

# 设置ycsb admin所需的一些环境变量
# ycsb web admin web服务的端口号
export YCSB_ADMIN_SERVER_PORT="8888"
# ycsb web admin数据库配置
export YCSB_ADMIN_DB_HOST="dev"
export YCSB_ADMIN_DB_PORT="3306"
export YCSB_ADMIN_DB_NAME="ycsb_web"
export YCSB_ADMIN_DB_USER="dev"
export YCSB_ADMIN_DB_PASSWORD="is@DEV"
# ycsb web admin的报警邮箱配置
export YCSB_ADMIN_MAIL_HOST="smtp.aliyun.com"
export YCSB_ADMIN_MAIL_PORT="465"
export YCSB_ADMIN_MAIL_USER="weixiao.me@aliyun.com"
export YCSB_ADMIN_MAIL_FROM="weixiao.me@aliyun.com"
export YCSB_ADMIN_MAIL_PASSWD="your email password"

# 设置ycsb executor所需的一些环境变量
# ycsb executor web服务端口号
export YCSB_EXECUTOR_SERVER_PORT=8889
# ycsb admin 主页地址，用于executor与admin进行RPC通信
export YCSB_ADMIN_ADDR="http://127.0.0.1:8888/ycsb-job-admin"
# ycsb executor 名称
export YCSB_EXECUTOR_NAME="ycsb-job-executor"
# ycsb executor RPC远程地址
export YCSB_EXECUTOR_ADDR="http://127.0.0.1:9999/"
# ycsb executor RPC服务的IP和端口号
export YCSB_EXECUTOR_IP="127.0.0.1"
export YCSB_EXECUTOR_PORT="9999"

echo "the env of ycsb-web-admin and ycsb-web-executor seated up successfully."