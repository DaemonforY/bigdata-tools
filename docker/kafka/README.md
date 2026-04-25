# 本地 Docker Kafka（教学用）

单节点 Kafka，运行在 **KRaft 模式**（不再需要 ZooKeeper），秒级启动，
随附 Web 端 Kafka UI 方便看 topic / partition / consumer group。

> 镜像：`apache/kafka:3.9.0`（多架构，Apple Silicon 原生 arm64 运行，无 Rosetta）
> UI： `provectuslabs/kafka-ui:latest`

---

## 一、启动

```bash
# 在仓库根目录执行
docker compose -f docker/kafka/docker-compose.yml up -d

# 等大约 10-15 秒，确认 broker 起来
docker logs kafka-broker | grep "Kafka Server started"
```

打开 <http://localhost:8085>（Kafka UI）应能看到本地集群。

| 端口  | 用途                              |
|-------|-----------------------------------|
| 9092  | Broker（PLAINTEXT）：Java 客户端入口 |
| 9093  | KRaft Controller（仅容器内部）      |
| 8085  | Kafka UI                          |

---

## 二、创建演示 topic

```bash
bash docker/kafka/init-topics.sh
```

会一次性创建：

| Topic           | 分区数 | 用途                                |
|-----------------|--------|-------------------------------------|
| `test`          | 3      | 通用演示                            |
| `test1`         | 1      | 单分区 → 演示分区内严格顺序消费      |
| `test3`         | 3      | 多分区 → 演示分区策略 / 并行消费      |
| `input-topic`   | 1      | Kafka Streams WordCount 输入        |
| `output-topic`  | 1      | Kafka Streams WordCount 聚合结果    |

也可以用 shell 单独建：

```bash
docker exec kafka-broker /opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server localhost:9092 --create \
  --topic mytopic --partitions 3 --replication-factor 1
```

---

## 三、命令行 Producer / Consumer

```bash
# 控制台 Producer（每行一条消息）
docker exec -it kafka-broker /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 --topic test

# 控制台 Consumer（从头开始消费）
docker exec -it kafka-broker /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic test --from-beginning

# 查看 consumer group 状态
docker exec kafka-broker /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 --describe --group demo-group
```

---

## 四、Java 客户端

`KafkaProducerDemo` / `KafkaConsumerDemo` / `KafkaConsumerExercise`
默认连 `localhost:9092`，可通过 JVM 系统属性切换：

```bash
mvn exec:java -Dexec.mainClass=com.example.demo.kafka.demo.KafkaProducerDemo
mvn exec:java -Dexec.mainClass=com.example.demo.kafka.demo.KafkaConsumerDemo
mvn exec:java -Dexec.mainClass=com.example.demo.kafka.demo.KafkaConsumerDemo \
  -Dkafka.bootstrap.servers=other-host:9092 -Dkafka.topic=mytopic
```

Spring Boot 走 `application.properties` 中的
`spring.kafka.bootstrap-servers=localhost:9092` —— **无需任何额外 JVM 参数**
（与 HBase 不同，Kafka advertise 的就是 `localhost`，不需要 hosts 映射）。

---

## 五、停止 / 清理

```bash
# 保留数据卷停止
docker compose -f docker/kafka/docker-compose.yml down

# 连数据一起清掉
docker compose -f docker/kafka/docker-compose.yml down -v
```

---

## 六、常见排错

| 现象 | 原因 | 解决 |
|------|------|------|
| `advertised.listeners cannot use the nonroutable meta-address 0.0.0.0` | listener 配错 | 不要让 `KAFKA_ADVERTISED_LISTENERS` 出现 `0.0.0.0`，必须是客户端能解析到的具体名字 |
| 客户端 `Connection to node -1 could not be established` | broker 没起 / 端口被占 | `docker logs kafka-broker`；`lsof -i :9092` |
| Kafka UI 显示集群离线 | UI 走 docker 内部网络访问不到 broker | 当前 compose 用 `host.docker.internal:9092`，需要 Docker Desktop |
| 重启容器后启动失败 (NoResource) | 数据卷里的 cluster-id 与 env 不一致 | `docker compose down -v` 把卷一起清掉再起 |
| Apple Silicon 上启动慢 | 无此问题 | apache/kafka 镜像支持原生 arm64，无需 Rosetta |
