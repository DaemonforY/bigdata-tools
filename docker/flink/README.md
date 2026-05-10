# 本地 Docker Flink（教学用）

单 JobManager + 单 TaskManager 的 Flink Session 集群，秒级启动，配套 Web UI 方便观察 Job / Task / Checkpoint / Watermark。

> 镜像：`flink:1.18-scala_2.12-java17`（多架构，Apple Silicon 原生 arm64 运行）

---

## 一、启动

```bash
# 仓库根目录执行
docker compose -f docker/flink/docker-compose.yml up -d

# 等大约 8-12 秒
docker logs flink-jobmanager | grep "Recover all persisted job graphs" -A1 | tail -2
```

| 端口  | 用途                                       |
|-------|--------------------------------------------|
| 8082  | JobManager Web UI → <http://localhost:8082> |
| 6123  | JobManager RPC（提交作业入口）             |

打开 <http://localhost:8082>，应能看到 1 个 TaskManager 注册上来，4 个 Task Slot。

---

## 二、最快上手：内置 SocketWindowWordCount

```bash
# 1) 在宿主机起一个 nc 监听 9999
nc -lk 9999          # mac 自带 nc；Linux 上一般是 ncat

# 2) 在另一个终端窗口提交 Flink 自带的 SocketWordCount（注意 host 用 host.docker.internal）
docker exec flink-jobmanager flink run \
  /opt/flink/examples/streaming/SocketWindowWordCount.jar \
  --hostname host.docker.internal --port 9999

# 3) 回到 nc 窗口随便敲几行英文，回车
hello flink hello world

# 4) 看输出
docker exec flink-taskmanager bash -c \
  "tail -f /opt/flink/log/flink-*-taskexecutor-*.out"
```

Web UI 上能看到一个 RUNNING 的 Job。

---

## 三、提交项目内的 Job

仓库根目录的 `pom.xml` 已经把 `flink-streaming-java/flink-clients/flink-connector-kafka` 加进来了。先打包：

```bash
mvn -DskipTests package
```

提交到集群：

```bash
docker cp target/demo-0.0.1-SNAPSHOT.jar flink-jobmanager:/opt/job.jar

# 例：跑一个 DataStream WordCount
docker exec flink-jobmanager flink run \
  -c com.example.demo.flink.demo.WordCountJob \
  /opt/job.jar

# 例：跑 Kafka -> Flink 的窗口聚合
docker exec flink-jobmanager flink run \
  -c com.example.demo.flink.demo.KafkaWindowJob \
  /opt/job.jar
```

> Spring Boot 项目里的 Web 控制器演示用的是 **MiniCluster 本地模式**（`StreamExecutionEnvironment.createLocalEnvironment()` 或默认的 `getExecutionEnvironment()` 在 Driver 进程里执行）—— 学生点页面按钮就能看到效果，不用 submit 上集群。

---

## 四、SQL Client

```bash
docker exec -it flink-jobmanager ./bin/sql-client.sh
```

```sql
-- 一行入门
SELECT 'hello flink sql';

-- 模拟 Kafka 表（教学用：datagen 生成数据，免依赖）
CREATE TABLE orders (
  user_id INT,
  amount DOUBLE,
  ts TIMESTAMP_LTZ(3)
) WITH ('connector'='datagen','rows-per-second'='5');

SELECT user_id, SUM(amount) FROM orders GROUP BY user_id;
```

---

## 五、Web 控制器一键体验（推荐学生入门走这条）

启动 Spring Boot 主程序后访问：

| 入口                                    | 演示什么                              |
|-----------------------------------------|---------------------------------------|
| <http://localhost:8090/flink/wordcount> | DataStream WordCount，最小 Flink 程序 |
| <http://localhost:8090/flink/window>    | 滚动窗口 + Watermark 实时统计          |
| <http://localhost:8090/flink/state>     | KeyedProcessFunction + 状态超时       |
| <http://localhost:8090/flink/cep>       | CEP 连续登录失败检测                   |

---

## 六、停止 / 清理

```bash
docker compose -f docker/flink/docker-compose.yml down
docker compose -f docker/flink/docker-compose.yml down -v   # 连 checkpoint 一起清
```

---

## 七、常见排错

| 现象 | 原因 | 解决 |
|------|------|------|
| TaskManager 起来后又退出 | JobManager 还没就绪导致首次连接失败 | `restart: unless-stopped` 已配，等 10 秒后会自动重连 |
| Job 提交报 `Could not connect to BlobServer` | 容器里 hostname 没注册到宿主机 | 用 `docker exec flink-jobmanager flink run` 在容器内提交，避开网络问题 |
| Web UI 8081 被 Kafka HBase 占用 | 端口冲突 | 本 compose 已改用 8082 → 容器内 8081 |
| Watermark 一直不推进 | 单分区 + 没数据来 / 时间字段错 | 多看 Web UI 中 Job 详情里的 Watermark 列；演示 demo 都用 `WatermarkStrategy.forBoundedOutOfOrderness` |
