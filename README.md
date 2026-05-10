# bigdata-tools

一个面向教学的大数据中间件实战项目：用 Spring Boot 把 **Redis / HBase / Kafka / Spark / Flink** 串成一条线，
每个组件都给学生提供：

1. **本地 Docker 一键起服务**（`docker/<组件>/docker-compose.yml`）
2. **可点可玩的 Web Demo**（`/redis`、`/hbase/student/...`、`/kafka/...`、`/spark/...`、`/flink/...`）
3. **教学大纲与高频面试题**（`src/main/resources/资料/`）

启动后打开 <http://localhost:8090> 是总入口。

---

## 一、项目里有什么

| 模块  | Docker 目录          | Java 代码                                        | 教学资料                              |
|-------|----------------------|--------------------------------------------------|---------------------------------------|
| Redis | （直接装宿主机或自建） | `com.example.demo.redis`                          | `资料/Redis 教学大纲与案例设计.md`    |
| HBase | `docker/hbase/`      | `com.example.demo.hbase`                          | `资料/HBase 教学大纲与案例设计.md`    |
| Kafka | `docker/kafka/`      | `com.example.demo.kafka`                          | `资料/Kafka 教学大纲与案例设计.md`    |
| Spark | `docker/spark/`      | `com.example.demo.spark`                          | `资料/Spark 教学大纲与案例设计.md`    |
| Flink | `docker/flink/`      | `com.example.demo.flink`                          | `资料/Flink 教学大纲与案例设计.md`    |

> 综合项目（订单异步处理、风控、实时排行榜等）在 `com.example.demo.projects` 里，访问入口 `/order/demo`、`/risk/demo`、`/score/demo` 等。

---

## 二、快速上手

### 1) 起需要的中间件（按需）

```bash
# Redis：默认 application.properties 用 localhost:6380，按需自起容器
# HBase
docker compose -f docker/hbase/docker-compose.yml up -d
# Kafka（教学时与 Spark/Flink Streaming demo 都依赖它）
docker compose -f docker/kafka/docker-compose.yml up -d
bash docker/kafka/init-topics.sh
# Spark Standalone（学集群提交时再起；只跑 Web demo 可不起）
docker compose -f docker/spark/docker-compose.yml up -d
# Flink Session 集群（学集群提交时再起；只跑 Web demo 可不起）
docker compose -f docker/flink/docker-compose.yml up -d
```

| 服务         | UI                                    |
|--------------|---------------------------------------|
| Kafka UI     | <http://localhost:8085>                |
| HBase Master | <http://localhost:16010>               |
| Spark Master | <http://localhost:8088>                |
| Spark Worker | <http://localhost:8081>                |
| Flink Web    | <http://localhost:8082>                |

### 2) 启动 Spring Boot 主程序

```bash
mvn spring-boot:run
```

打开 <http://localhost:8090>，选感兴趣的模块进入即可。

> Spring Boot 启动时会创建一个进程内的 `SparkSession (local[*])`，给 Spark Web demo 用，
> 不需要 Spark Docker 集群。**只有想体验 spark-submit 提交集群作业**时才起 `docker/spark/`。
>
> Flink Web demo 用的是 mini-cluster，同样不依赖 Flink Docker 集群。

---

## 三、教学路线建议

```
入门
 │
 ├─ Redis：缓存、队列、限流、排行榜（基础）
 ├─ HBase：宽表、版本、Rowkey 设计（NoSQL 思维）
 ├─ Kafka：分区、消费组、Streams（消息中间件）
 │
中级
 │
 ├─ Spark：RDD → DataFrame → Spark SQL → Structured Streaming（批 + 流）
 │
高级
 │
 └─ Flink：DataStream → Window/Watermark → State/Timer → CEP（实时）
```

每个组件都先看 `资料/` 下的教学大纲与面试题，再跑 Web demo，最后照《xxx 教学大纲与案例设计.md》末尾的"推荐学习路径"周计划自己复刻。

---

## 四、目录速览

```
bigdata-tools/
├── docker/
│   ├── hbase/         # 单机 HBase（standalone）
│   ├── kafka/         # 单节点 KRaft Kafka + Kafka UI
│   ├── spark/         # 单 Master + 单 Worker
│   └── flink/         # 单 JobManager + 单 TaskManager
├── src/main/java/com/example/demo/
│   ├── redis/         # Redis 各种小例子
│   ├── hbase/         # HBase 学生信息管理
│   ├── kafka/         # Kafka producer/consumer/streams
│   ├── spark/         # Spark RDD / SQL / Structured Streaming
│   ├── flink/         # Flink DataStream / Window / State / CEP
│   └── projects/      # 综合项目（订单/风控/排行榜...）
└── src/main/resources/
    ├── templates/     # 各模块 Thymeleaf 页面
    └── 资料/           # 教学大纲、案例设计、面试题
```

---

## 五、常见环境

- JDK 17（Java 17 跑 Spark 已经在 `pom.xml` 中加好了 `--add-opens` JVM 参数）
- Maven 3.6+
- Docker Desktop（Apple Silicon 直接用，多数镜像支持 arm64 原生）

如果遇到端口被占（`8085 / 8088 / 8081 / 8082` 等），按各组件 README 里的"常见排错"调整。
