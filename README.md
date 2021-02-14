## 1. ycsb-web的项目简介

YCSB，全称为"Yahoo！Cloud Serving Benchmark"， 是雅虎开发的用来对云服务进行基础测试的工具，其内部涵盖了常见的NoSQL数据库产品，
如Cassandra、MongoDB、HBase、Redis等等。在运行YCSB的时候，可以配置不同的workload和DB，也可以指定线程数和并发数等其他参数。

原生的YCSB，其纯命令行式的使用体验对我而言存在诸多不便，特别是在对测试的结果进行收集、汇总、分析和历史比对的时候。

于是乎，心里萌发出了一个想法，把YCSB平台化，它拥有简单的界面、而且具备YCSB所有的功能，此外，它需要支持分布式。因为，单节点的机器资源，
或许无法满足针对超大型数据库系统的基准性能测试。最后，YCSB压测的结果指标被DataCenter统一收集和汇总，以图表化的形式呈现给用户，
并可以支持与历史测试数据的各个维度进行对比。

## 2. ycsb-web目前支持的功能

- 支持YCSB的所有功能，目前优先对HBase进行适配，但它理论上可以支持YCSB支持的所有数据库
- 支持多执行器并发执行YCSB的压测任务
- 支持YCSB压测任务的定时、以及单次触发
- 压测任务界面化的编辑功能
- 压测任务执行时日志实时预览
- 压测任务依赖功能，比如：load -> run workloada -> run workloadb
- 报警功能，任务运行失败及时报警  
- 压测任务调度报表展示、压测结果指标绘图（规划开发中）
- 用户管理，用户与执行器功能绑定

## 3. 系统功能使用截图

### 3.1 执行器管理

创建执行器，支持绑定多个节点的地址，已实现分布式YCSB基准性能测试。

![create-executor](https://leo-jie-pic.oss-cn-beijing.aliyuncs.com/leo_blog/2021-02-11-134802.jpg)

![show-executor](https://leo-jie-pic.oss-cn-beijing.aliyuncs.com/leo_blog/2021-02-11-%E4%BC%81%E4%B8%9A%E5%BE%AE%E4%BF%A1%E6%88%AA%E5%9B%BE_fad2c1df-454d-4155-b1f5-3346558b0462.png)


### 3.2 任务管理

创建一个YCSB的压测任务，可以设置为定时调度，也可以设置为一次执行

![create-task](https://leo-jie-pic.oss-cn-beijing.aliyuncs.com/leo_blog/2021-02-11-%E4%BC%81%E4%B8%9A%E5%BE%AE%E4%BF%A1%E6%88%AA%E5%9B%BE_b7609394-c1ab-4157-8c66-1885055cc8dd.png)

![show-task](https://leo-jie-pic.oss-cn-beijing.aliyuncs.com/leo_blog/2021-02-11-%E4%BC%81%E4%B8%9A%E5%BE%AE%E4%BF%A1%E6%88%AA%E5%9B%BE_47221cbd-b95c-405e-8426-51be36be52a3.png)

### 3.3 运行YCSB任务

我们在添加的任务操作按钮中选择执行一次。

查看任务的调度列表。

![schedule-list](https://leo-jie-pic.oss-cn-beijing.aliyuncs.com/leo_blog/2021-02-11-%E4%BC%81%E4%B8%9A%E5%BE%AE%E4%BF%A1%E6%88%AA%E5%9B%BE_784852e7-abca-4a19-a12f-d912b9a7d7e4.png)

操作按钮中选择查看执行日志，如下图，YCSB压测任务的实时日志会输出到WebConsole中

![show-log](https://leo-jie-pic.oss-cn-beijing.aliyuncs.com/leo_blog/2021-02-11-135837.jpg)

任务运行结束或被kill之后，YCSB的测试汇总数据也会输出到界面中。

![show-result](https://leo-jie-pic.oss-cn-beijing.aliyuncs.com/leo_blog/2021-02-11-140028.jpg)


### 3.4 任务运行大屏

![show-detail](https://leo-jie-pic.oss-cn-beijing.aliyuncs.com/leo_blog/2021-02-11-140113.jpg)


### 3.5 任务失败报警

![alarm](https://leo-jie-pic.oss-cn-beijing.aliyuncs.com/leo_blog/2021-02-11-140312.jpg)

### 3.6 YCSB压测指标汇总

快马加鞭开发中

## 4. 开始使用

### 4.1 ycsb-job-admin

**编译打包和运行**

```shell
cd ycsb-web
mvn clean package -DskipTests -Pprod -pl ycsb-job-admin -am

mkdir ycsb-job-admin
mv ycsb-job-admin-1.0.0.jar ycsb-job-admin/
cd ycsb-job-admin
sh ycsb-job-admin.sh start|status|stop|restart
```

**系统配置**

```shell
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

# 上述所需的配置会传递给ycsb-job-admin的application.properties配置文件
```

### 4.2 ycsb-job-executor

**编译打包和运行**

```shell
mvn clean package -DskipTests -pl ycsb-job-executor -am


```



## 5. 总结

ycsb-web的idea源于JMeter，编码的实现则基于xxl-job和YCSB整合而来，两个毫不相干的项目，被强扭在了一起，看起来倒也契合，
这强扭的瓜或许也挺甜。

## 6. 特别鸣谢

XXL-JOB、YCSB





