# 本地 Docker Spark（教学用）

单 Master + 单 Worker 的 Spark Standalone 集群，秒级启动，配套 Web UI 方便观察 Job / Stage / Task。

> 镜像：`apache/spark:3.5.1`（Apache 官方多架构镜像，Apple Silicon 原生 arm64）
>
> 注意：早期版本曾用 `bitnami/spark:3.5`，但 Bitnami 把旧版浮动 tag 从 Docker Hub 撤下，
> 现在拉不到。故本仓库统一切换到 Apache 官方镜像。

---

## 一、启动

```bash
# 仓库根目录执行
docker compose -f docker/spark/docker-compose.yml up -d

# 等大约 5-10 秒
docker logs spark-master | grep "Master:" | tail -5
docker logs spark-worker | grep "Worker:" | tail -5
```

| 端口  | 用途                                       |
|-------|--------------------------------------------|
| 7077  | Master RPC（提交作业入口）                 |
| 8088  | Master Web UI → <http://localhost:8088>    |
| 8081  | Worker Web UI → <http://localhost:8081>    |
| 4040  | Driver Web UI（任务运行期间才有）          |

打开 <http://localhost:8088> 应能看到 1 个 Worker 注册上来。

---

## 二、最快上手：spark-shell / pyspark

```bash
# Scala REPL
docker exec -it spark-master /opt/spark/bin/spark-shell \
  --master spark://spark-master:7077

# Python REPL
docker exec -it spark-master /opt/spark/bin/pyspark \
  --master spark://spark-master:7077
```

跑一个最小 RDD WordCount：

```scala
val rdd = sc.parallelize(Seq("hello spark", "hello flink", "hello kafka"))
rdd.flatMap(_.split(" ")).map((_,1)).reduceByKey(_+_).collect.foreach(println)
```

---

## 三、提交一个 jar 作业

仓库根目录的 `pom.xml` 已经把 `spark-core/spark-sql/spark-streaming-kafka` 加进来了。先打包：

```bash
mvn -DskipTests package
```

然后挂载 jar、提交到集群：

```bash
docker cp target/demo-0.0.1-SNAPSHOT.jar spark-master:/opt/jobs/demo.jar

docker exec spark-master /opt/spark/bin/spark-submit \
  --master spark://spark-master:7077 \
  --class com.example.demo.spark.demo.RddWordCount \
  /opt/jobs/demo.jar
```

> Spark Boot 自带的 controller 演示走的是 **本地模式**（`local[*]`）—— 学生在 Web 页面点按钮就能看到结果，不需要往集群里 submit。等学生熟练了，再切到上面的 `spark-submit` 提交方式上集群跑。

---

## 四、Web 控制器一键体验（推荐学生入门走这条）

启动 Spring Boot 主程序后访问：

| 入口                                    | 演示什么                          |
|-----------------------------------------|-----------------------------------|
| <http://localhost:8090/spark/wordcount> | RDD WordCount，最小 Spark 程序    |
| <http://localhost:8090/spark/sql>       | DataFrame + Spark SQL 学生成绩分析 |
| <http://localhost:8090/spark/streaming> | Structured Streaming 消费 Kafka   |

> 注意：Structured Streaming 演示需要先把 Kafka 起来（`docker compose -f docker/kafka/docker-compose.yml up -d`），并执行过 `bash docker/kafka/init-topics.sh` 创建好 `spark-input` topic。

---

## 五、停止 / 清理

```bash
docker compose -f docker/spark/docker-compose.yml down
docker compose -f docker/spark/docker-compose.yml down -v   # 连数据卷一起清
```

---

## 六、常见排错

| 现象 | 原因 | 解决 |
|------|------|------|
| 启动一会儿 Worker 自动退出 | 内存配额不足 | 调小 `SPARK_WORKER_MEMORY`，或在 Docker Desktop 给足 4G |
| 提交 jar 报 `ClassNotFoundException: SparkSession` | 没把 spark 依赖打进 fat-jar 或没用 `provided` | 教学环境直接用 Web 控制器（local 模式）；要 submit 上集群就用 `--packages` 或 fat-jar |
| Spark UI 8080 被 Kafka UI 占了 | 端口冲突 | 本 compose 已改用 8088（Master）和 8081（Worker） |
| 容器里运行作业，driver/executor 互相连不上 | Driver 在宿主机 / Executor 在容器，BlockManager 走容器名 | 教学场景把 Driver 也跑容器里（spark-submit 进 master 容器执行）|
