
# Kafka 教学大纲与案例设计

---

## 一、Kafka 基础与原理

### 1.1 Kafka 简介

#### Kafka 是什么？
Kafka 是一个**分布式、高吞吐、可扩展、持久化的消息队列与流处理平台**。最初由LinkedIn开发，后捐献给Apache基金会，广泛用于处理实时数据流、日志收集、事件驱动和数据总线等场景。

#### Kafka 的典型应用场景
- **日志收集与聚合**：将不同系统和服务的日志汇总到Kafka，统一处理和分析。
- **实时数据处理**：如网站用户行为流、金融交易流、IoT传感器流等，实时消费和处理。
- **微服务解耦**：服务之间通过Kafka异步通信，降低耦合、提升可靠性。
- **事件驱动架构**：以事件为中心的系统设计，支持复杂事件流转与异步处理。
- **数据总线/数据集成**：连接多种数据源与目标系统，实现数据同步与集成。

---

### 1.2 Kafka 架构原理

#### 核心组件

- **Broker**  
  Kafka 集群中的服务器节点，负责存储和转发消息。一个集群通常包含多个 Broker。

- **Topic**  
  消息的逻辑分类（主题），每个 Topic 可以有多个 Partition。

- **Partition**  
  Topic 的物理分区，消息在 Partition 中有序存储。Partition 是并行和高可用的基本单元。

- **Producer**  
  消息生产者，负责向指定 Topic 发送消息。

- **Consumer**  
  消息消费者，从 Topic 的 Partition 拉取消息。消费者通常属于一个 Consumer Group。

- **Consumer Group**  
  消费者组，组内每个 Partition 只会被一个消费者消费，支持水平扩展。

- **Zookeeper/KRaft**  
  Kafka 3.x 之前依赖 Zookeeper 进行元数据管理和选举，3.x 支持 KRaft（Kafka自带Raft协议）替代ZK。

#### 消息存储机制

- **顺序写入**：所有消息顺序追加到 Partition 的日志文件，极大提升磁盘写入效率。
- **磁盘持久化**：消息写入磁盘的 Segment 文件，支持高效的顺序读写和批量删除（基于时间或大小的日志清理）。
- **Segment 文件**：Partition 被分割为多个 Segment 文件，便于管理与清理。

#### 高可用与容错

- **副本机制**：每个 Partition 可以有多个副本（Replica），分布在不同 Broker 上。
- **ISR（In-Sync Replicas）**：同步副本集合，只有ISR内的副本才被认为是“最新”的。
- **Leader选举**：每个 Partition 有一个 Leader，Producer和Consumer只与Leader交互，Follower副本与Leader同步数据。
- **故障恢复**：Leader宕机后，ISR中的其他副本会被选举为新的Leader，保证消息不丢失。

---

### Kafka 核心架构图（文字版）

```
+-------------------+       +-------------------+      +-------------------+
|    Producer       |-----> |   Kafka Broker    |<-----|    Consumer       |
| (发送消息到Topic) |       |   (多个Partition) |      | (从Topic拉消息)   |
+-------------------+       +-------------------+      +-------------------+
                                      |
                                      v
                               +--------------+
                               |  Zookeeper   |
                               +--------------+
```

- Producer 将消息发送到 Topic（分区）。
- Broker 存储消息，分区有副本，Leader负责读写。
- Consumer Group 拉取消息，每个 Partition 只被组内一个Consumer消费。
- Zookeeper/KRaft 管理元数据、选举Leader、健康检查等。

---

### 数据流转过程

1. **Producer** 发送消息到指定 Topic，消息被分配到某个 Partition。
2. **Broker** 接收消息，顺序写入 Partition 的日志（Segment 文件），并同步到副本。
3. **Consumer** 从 Broker 拉取消息，按 Partition 顺序消费，消费进度（offset）可提交保存。
4. **Zookeeper/KRaft** 维护 Broker、Topic、Partition、Consumer Group 等元数据，负责 Leader 选举和故障转移。

---

### Kafka 与 RabbitMQ、传统消息队列的区别

| 特性         | Kafka                    | RabbitMQ/传统消息队列      |
|--------------|--------------------------|---------------------------|
| 存储方式     | 顺序写磁盘，分区分段     | 内存为主，磁盘为辅         |
| 吞吐量       | 极高（百万级/秒）        | 较高，但不及Kafka          |
| 消费模式     | 拉模式（Pull）           | 推模式（Push）             |
| 消息顺序     | 分区内有序               | 队列有序                   |
| 消费语义     | 至少一次、最多一次        | 一般为至少一次             |
| 适用场景     | 大数据流、日志、流计算   | 事务、通知、短消息         |
| 持久化       | 强，适合大数据场景       | 适合轻量/事务型消息        |
| 高可用       | 副本+ISR+Leader选举      | 镜像队列/主从              |
| 生态         | 大数据、流计算集成广泛   | 应用集成、微服务           |

---

### 练习题参考答案

#### 1. 用自己的话描述Kafka和RabbitMQ、传统消息队列的区别

- Kafka 适合高吞吐、分布式、海量数据流场景，消息持久化能力强，分区机制支持水平扩展；RabbitMQ/传统队列更适合低延迟、事务性场景，支持复杂的路由和确认机制，但吞吐量和持久化能力不如Kafka。

#### 2. Kafka逻辑架构图（文字描述）

```
Producer  -->  [Topic-Partition-Leader] (Kafka Broker集群) <--> Consumer Group
                       |   |   |                          |
                  [Follower Replicas]                [Zookeeper/KRaft]
```
- Producer写入Topic分区，Broker负责存储，Partition有Leader和多个Follower副本，Consumer Group按分区消费，Zookeeper/KRaft负责元数据和选举。

---

## 总结

- Kafka是现代大数据和流处理的核心中间件，具备高吞吐、高可用、水平扩展、消息持久化等特性。
- 其核心架构包括Broker、Topic、Partition、Producer、Consumer、Zookeeper/KRaft等组件。
- 与传统消息队列相比，Kafka更适合大数据、流式场景，RabbitMQ更适合事务和微服务解耦。
- 熟悉Kafka的架构和流转过程，有助于理解其在数据中台、实时计算等场景的应用价值。


---

## 二、Kafka 安装与命令行操作

### 2.1 环境准备
- 下载与启动Kafka（本地/伪分布式）
- 启动Zookeeper和Broker

### 2.2 基本命令行操作
- 创建Topic、查看Topic、删除Topic
- 发送消息、消费消息

#### 案例
```bash
# 启动Zookeeper和Kafka
bin/zookeeper-server-start.sh config/zookeeper.properties
bin/kafka-server-start.sh config/server.properties

# 创建topic
bin/kafka-topics.sh --create --topic test --bootstrap-server localhost:9092 --partitions 1 --replication-factor 1
(mac) kafka-topics --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic test

# 生产消息
bin/kafka-console-producer.sh --topic test --bootstrap-server localhost:9092
(mac) kafka-console-producer --topic test --bootstrap-server localhost:9092

# 消费消息
bin/kafka-console-consumer.sh --topic test --from-beginning --bootstrap-server localhost:9092
(mac) kafka-console-consumer --topic test --from-beginning --bootstrap-server localhost:9092
```

**练习**
- 创建一个名为 `student` 的topic，发送并消费10条消息。

---

## 三、Kafka Java API 编程

### 3.1 Producer API 原理与使用
- Producer配置参数、分区策略、消息可靠性
- 同步/异步发送消息

#### 案例
```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
KafkaProducer<String, String> producer = new KafkaProducer<>(props);
producer.send(new ProducerRecord<>("test", "hello kafka!"));
producer.close();
```

**练习**
- 编写Java代码，向`test` topic发送100条消息。

---

#### 发送100条消息的示例代码

```java
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.Callback;
import java.util.Properties;
/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/2 09:38:56
 * @Version 1.0
 */
public class KafkaProducerDemo {
    public static void main(String[] args) throws InterruptedException {
        // 1. 配置Producer参数
        KafkaProducer<String, String> producer = getStringStringKafkaProducer();

        // 3. 发送100条消息
        for (int i = 1; i <= 100; i++) {
            String key = "key" + i;
            String value = "message-" + i;
            Thread.sleep(500);

            // --- 同步发送 ---
            /*
            try {
                RecordMetadata metadata = producer.send(new ProducerRecord<>("test", key, value)).get();
                System.out.printf("Sent: %s, partition: %d, offset: %d%n", value, metadata.partition(), metadata.offset());
            } catch (Exception e) {
                e.printStackTrace();
            }
            */

            // --- 异步发送 ---
            producer.send(new ProducerRecord<>("test", key, value), new Callback() {
                @Override
                public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception == null) {
                        System.out.printf("Sent: %s, partition: %d, offset: %d%n", value, metadata.partition(), metadata.offset());
                    } else {
                        exception.printStackTrace();
                    }
                }
            });
        }

        // 4. 关闭Producer
        producer.close();
    }

    private static KafkaProducer<String, String> getStringStringKafkaProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        // 可选：提高可靠性
        props.put("acks", "all"); // 等待所有副本确认
        props.put("retries", 3);  // 失败重试次数

        // 2. 创建Producer实例
        return new KafkaProducer<>(props);
    }
}
```

---

#### 说明

- **同步发送**（注释块内）会阻塞直到消息被Broker确认，适合对可靠性要求极高的场景。
- **异步发送**（默认）不会阻塞主线程，推荐用于高吞吐业务，发送结果通过回调处理。
- 你可以根据需要切换同步或异步发送方式。

---

#### 练习扩展

1. 修改代码，尝试只用同步或只用异步方式发送，并观察输出。
2. 修改消息内容，发送带有时间戳或自定义内容的消息。
3. 改为向不同的 topic 或 partition 发送消息，体验分区策略。


---

### 3.2 Consumer API 原理与使用
- Consumer Group、自动/手动提交offset、消费模式
- 消费消息反序列化

#### 案例
下面是一个完整的 Java Kafka Consumer 练习代码，能够消费 `test` topic 的所有消息，并统计收到的消息总数：
```java
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/2 09:46:54
 * @Version 1.0
 */
public class KafkaConsumerDemo {
    public static void main(String[] args) {
        // 1. 配置Consumer参数
        KafkaConsumer<String, String> consumer = getStringStringKafkaConsumer();

        // 3. 消费消息并统计总数
        try (consumer) {
            consumer.subscribe(List.of("test"));
            int totalCount = 0;
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    totalCount++;
                    System.out.printf("offset = %d, key = %s, value = %s, total = %d%n",
                            record.offset(), record.key(), record.value(), totalCount);
                }
            }
        }
    }

    private static KafkaConsumer<String, String> getStringStringKafkaConsumer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "demo-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        // 自动提交offset（可选）
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");

        // 2. 创建Consumer实例并订阅topic
        return new KafkaConsumer<>(props);
    }
}
```


## 说明

- **group.id**：同组内的消费者会自动负载均衡分区，实现并行消费。
- **enable.auto.commit**：自动提交offset，保证消息不会重复消费（也可手动提交）。
- **key/value.deserializer**：反序列化消息为字符串。
- **totalCount**：统计消费到的消息总数。

---

## 练习扩展

1. 修改代码，改为手动提交offset（`enable.auto.commit=false`，并在循环末尾加 `consumer.commitSync()`）。
2. 打印每个分区的消息数量。
3. 统计每秒消费的消息速率。
```java
package com.example.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.*;
/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/2 03:51:39
 * @Version 1.0
 */
public class KafkaConsumerExercise {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "demo-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("enable.auto.commit", "false"); // 关闭自动提交

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("test"));

        int totalCount = 0;
        Map<Integer, Integer> partitionCount = new HashMap<>();

        long lastPrintTime = System.currentTimeMillis();
        int countThisSecond = 0;

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                for (ConsumerRecord<String, String> record : records) {
                    totalCount++;
                    countThisSecond++;
                    int partition = record.partition();
                    partitionCount.put(partition, partitionCount.getOrDefault(partition, 0) + 1);

                    System.out.printf("partition=%d, offset=%d, key=%s, value=%s, total=%d%n",
                            partition, record.offset(), record.key(), record.value(), totalCount);
                }

                // 手动提交offset
                consumer.commitSync();

                // 每秒打印一次速率和分区统计
                long now = System.currentTimeMillis();
                if (now - lastPrintTime >= 1000) {
                    System.out.println("---- 每秒消费条数: " + countThisSecond);
                    System.out.println("---- 各分区累计消息数: " + partitionCount);
                    countThisSecond = 0;
                    lastPrintTime = now;
                }
            }
        } finally {
            consumer.close();
        }
    }
}
```
```java
package com.example.kafka;

import org.apache.kafka.clients.producer.*;
import com.google.gson.Gson;

import java.util.Properties;
import java.util.UUID;
/**
 * @Description
 * @Author miaoyongbin
 * @Date 2025/7/2 03:55:54
 * @Version 1.0
 */
public class KafkaProducerPractice {
    private static final String BOOTSTRAP_SERVERS = "localhost:9092";

    public static void main(String[] args) throws Exception {
        // 选择练习项，取消对应方法的注释即可体验
        //sendSyncMessages();
        //sendAsyncMessages();
        //sendCustomContentMessages();
        //sendToDifferentTopics();
        //sendToDifferentPartitions();
        //sendKeyAndNoKeyMessages();
        //sendBulkMessagesAndMeasure();
        //sendJsonObjectMessages();
        //sendIdempotentMessages();
    }

    // 1. 只用同步方式发送100条消息
    public static void sendSyncMessages() throws Exception {
        KafkaProducer<String, String> producer = getProducer();
        for (int i = 1; i <= 100; i++) {
            String key = "key" + i;
            String value = "message-" + i;
            RecordMetadata metadata = producer.send(new ProducerRecord<>("test", key, value)).get();
            System.out.printf("Sync Sent: %s, partition: %d, offset: %d%n", value, metadata.partition(), metadata.offset());
        }
        producer.close();
    }

    // 2. 只用异步方式发送100条消息
    public static void sendAsyncMessages() {
        KafkaProducer<String, String> producer = getProducer();
        for (int i = 1; i <= 100; i++) {
            String key = "key" + i;
            String value = "message-" + i;
            producer.send(new ProducerRecord<>("test", key, value), (metadata, exception) -> {
                if (exception == null) {
                    System.out.printf("Async Sent: %s, partition: %d, offset: %d%n", value, metadata.partition(), metadata.offset());
                } else {
                    exception.printStackTrace();
                }
            });
        }
        producer.close();
    }

    // 3. 每条消息内容包含当前时间戳和自定义业务字段
    public static void sendCustomContentMessages() {
        KafkaProducer<String, String> producer = getProducer();
        for (int i = 1; i <= 100; i++) {
            String key = "user" + i;
            String value = String.format("message-%d, ts=%d, userId=%s", i, System.currentTimeMillis(), key);
            producer.send(new ProducerRecord<>("test", key, value));
        }
        producer.close();
    }

    // 4. 发送消息到不同的topic
    public static void sendToDifferentTopics() {
        KafkaProducer<String, String> producer = getProducer();
        for (int i = 1; i <= 100; i++) {
            String topic = (i % 2 == 0) ? "testA" : "testB";
            String key = "key" + i;
            String value = "message-" + i;
            producer.send(new ProducerRecord<>(topic, key, value));
        }
        producer.close();
    }

    // 5. 指定不同partition发送消息
    public static void sendToDifferentPartitions() {
        KafkaProducer<String, String> producer = getProducer();
        for (int i = 1; i <= 100; i++) {
            int partition = i % 3; // 假设有3个分区
            String key = "key" + i;
            String value = "message-" + i;
            producer.send(new ProducerRecord<>("test", partition, key, value));
        }
        producer.close();
    }

    // 7. 部分消息指定key，部分不指定key
    public static void sendKeyAndNoKeyMessages() {
        KafkaProducer<String, String> producer = getProducer();
        for (int i = 1; i <= 100; i++) {
            String value = "message-" + i;
            if (i % 2 == 0) {
                producer.send(new ProducerRecord<>("test", null, value)); // 不指定key
            } else {
                String key = "key" + i;
                producer.send(new ProducerRecord<>("test", key, value)); // 指定key
            }
        }
        producer.close();
    }

    // 8. 发送10000条消息，测量总耗时和吞吐量
    public static void sendBulkMessagesAndMeasure() {
        KafkaProducer<String, String> producer = getProducer();
        long start = System.currentTimeMillis();
        for (int i = 1; i <= 10000; i++) {
            producer.send(new ProducerRecord<>("test", "key" + i, "message-" + i));
        }
        producer.flush();
        long end = System.currentTimeMillis();
        System.out.println("发送10000条消息耗时: " + (end - start) + " ms, 吞吐: " + (10000.0 / (end - start) * 1000) + " 条/秒");
        producer.close();
    }

    // 9. 发送Java对象转JSON的消息
    public static void sendJsonObjectMessages() {
        KafkaProducer<String, String> producer = getProducer();
        Gson gson = new Gson();
        for (int i = 1; i <= 100; i++) {
            Event event = new Event("user" + i, System.currentTimeMillis(), "login");
            String json = gson.toJson(event);
            producer.send(new ProducerRecord<>("test", "key" + i, json));
        }
        producer.close();
    }

    // 10. 幂等性Producer
    public static void sendIdempotentMessages() {
        Properties props = new Properties();
        props.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all");
        props.put("retries", 3);
        props.put("enable.idempotence", "true"); // 开启幂等性
        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        for (int i = 1; i <= 100; i++) {
            String key = UUID.randomUUID().toString();
            String value = "idempotent-message-" + i;
            producer.send(new ProducerRecord<>("test", key, value));
        }
        producer.close();
    }

    // Producer基础配置
    private static KafkaProducer<String, String> getProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("acks", "all");
        props.put("retries", 3);
        return new KafkaProducer<>(props);
    }

    // 用于发送JSON对象的Event类
    static class Event {
        String userId;
        long ts;
        String action;
        Event(String userId, long ts, String action) {
            this.userId = userId;
            this.ts = ts;
            this.action = action;
        }
    }
}

```

## 1. sendSyncMessages() —— 同步发送

```java
RecordMetadata metadata = producer.send(new ProducerRecord<>("test", key, value)).get();
```
- **ProducerRecord**：表示一条待发送的消息，包含topic、key、value等信息。
- **producer.send(record)**：异步发送消息，返回一个Future对象。
- **.get()**：调用Future的get方法，阻塞直到消息被Broker确认，获得RecordMetadata（包含分区、偏移量等）。
- **同步发送**：只有消息成功写入Kafka，方法才返回。适合对可靠性要求高的场景。

---

## 2. sendAsyncMessages() —— 异步发送

```java
producer.send(new ProducerRecord<>("test", key, value), (metadata, exception) -> {
    if (exception == null) {
        // 发送成功
    } else {
        // 发送失败
    }
});
```
- **Callback**：回调接口，消息发送后自动调用。
- **onCompletion**：发送成功时metadata不为null，失败时exception不为null。
- **异步发送**：不会阻塞主线程，适合高吞吐业务。

---

## 3. sendCustomContentMessages() —— 自定义消息内容

```java
String value = String.format("message-%d, ts=%d, userId=%s", i, System.currentTimeMillis(), key);
producer.send(new ProducerRecord<>("test", key, value));
```
- **System.currentTimeMillis()**：获取当前时间戳，常用于消息追踪或排序。
- **自定义内容**：可以携带业务ID、时间、操作类型等信息，便于后续消费端处理。

---

## 4. sendToDifferentTopics() —— 向不同Topic发送

```java
String topic = (i % 2 == 0) ? "testA" : "testB";
producer.send(new ProducerRecord<>(topic, key, value));
```
- **ProducerRecord(String topic, ...)**：可以指定不同topic，实现多业务数据分流。

---

## 5. sendToDifferentPartitions() —— 指定分区发送

```java
producer.send(new ProducerRecord<>("test", partition, key, value));
```
- **ProducerRecord(String topic, Integer partition, K key, V value)**：显式指定分区号。
- **分区策略**：可实现同一业务ID（如userId）落到同一分区，便于有序消费。

---

## 6. sendKeyAndNoKeyMessages() —— 指定/未指定key

```java
producer.send(new ProducerRecord<>("test", null, value)); // 不指定key
producer.send(new ProducerRecord<>("test", key, value));  // 指定key
```
- **不指定key**：Kafka会用轮询方式分配分区。
- **指定key**：Kafka会对key hash，保证同一key的消息落到同一分区（有序性）。

---

## 7. sendBulkMessagesAndMeasure() —— 批量发送，统计吞吐

```java
long start = System.currentTimeMillis();
for (...) {
    producer.send(...);
}
producer.flush();
long end = System.currentTimeMillis();
```
- **producer.flush()**：强制将消息批量推送到Broker，确保所有消息都已发送。
- **吞吐量统计**：用时间差和消息数计算每秒发送条数。

---

## 8. sendJsonObjectMessages() —— 发送JSON对象

```java
Gson gson = new Gson();
Event event = new Event("user" + i, System.currentTimeMillis(), "login");
String json = gson.toJson(event);
producer.send(new ProducerRecord<>("test", "key" + i, json));
```
- **Gson.toJson(obj)**：将Java对象序列化为JSON字符串。
- **发送结构化数据**：方便消费端反序列化为对象处理。

---

## 9. sendIdempotentMessages() —— 幂等性Producer

```java
props.put("enable.idempotence", "true");
KafkaProducer<String, String> producer = new KafkaProducer<>(props);
```
- **enable.idempotence=true**：开启幂等性，避免因重试导致的消息重复（Exactly Once语义）。
- **适合金融、订单等要求严格不重复的业务场景**。

---

## 补充：Producer基础配置API

```java
props.put("bootstrap.servers", "localhost:9092");
props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
props.put("acks", "all"); // 等待所有副本确认
props.put("retries", 3);  // 失败重试次数
```
- **bootstrap.servers**：Kafka集群地址，多个用逗号分隔。
- **key.serializer/value.serializer**：消息key/value的序列化方式。
- **acks**：消息可靠性级别，"0"（不等确认）、"1"（Leader确认）、"all"（所有副本确认）。
- **retries**：发送失败自动重试次数。

---

## 四、Kafka 进阶特性与案例

## 4.1 分区与副本机制

### 原理讲解

- **分区（Partition）**：每个Topic可分为多个分区，消息分布在不同分区上，允许并行写入和多Consumer并行消费，实现负载均衡和高吞吐。
- **副本（Replica）**：每个分区可有多个副本（1个Leader，N-1个Follower），Leader负责读写，Follower负责同步，Leader挂了自动切换，提升高可用。

### 案例：创建多分区多副本Topic

**命令行**
```bash
# 创建名为test-advanced，3分区2副本的topic
bin/kafka-topics.sh --create --topic test-advanced --bootstrap-server localhost:9092 --partitions 3 --replication-factor 2
```

**观察生产和消费效果**
- 向该topic发送消息，消息会均匀分布在3个分区。
- 启动多个consumer在同一group下消费，自动分配分区，实现并行消费。
- 关闭/重启consumer，观察group rebalance（分区再分配）现象。

**练习**
- 用命令行创建3分区topic，启动3个consumer（不同进程或不同group.id），观察每个consumer收到的分区和消息数量，尝试kill掉一个consumer再观察分区分配变化。

---

## 4.2 Offset 管理与消费语义

### 原理讲解

- **Offset**：Kafka记录每个Consumer在每个分区消费到的位置（offset）。
- **自动提交**：Consumer自动定期提交offset（默认5秒），简单但可能丢失/重复消费。
- **手动提交**：业务处理后再提交offset，可靠性更高。
- **消费语义**：
  - At most once：最多一次，可能丢失。
  - At least once：最少一次，可能重复。
  - Exactly once：恰好一次，需要幂等Producer+事务。

### 案例：手动提交offset

```java
Properties props = new Properties();
props.put("bootstrap.servers", "localhost:9092");
props.put("group.id", "manual-commit-group");
props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
props.put("enable.auto.commit", "false"); // 关闭自动提交

KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
consumer.subscribe(Arrays.asList("test-advanced"));

while (true) {
    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
    for (ConsumerRecord<String, String> record : records) {
        System.out.printf("partition=%d, offset=%d, value=%s%n", record.partition(), record.offset(), record.value());
        // 模拟异常场景
        if ("error".equals(record.value())) {
            throw new RuntimeException("模拟异常");
        }
    }
    // 手动提交offset
    consumer.commitSync();
}
```

**练习**
- 修改consumer为手动提交offset，故意抛出异常，重启后观察消息是否会重复消费。
- 分析为何手动提交可以实现“at least once”语义。

---

## 4.3 Kafka Stream API 简介

### 原理讲解

- **流式处理 vs 批处理**：流式处理每到一条消息就处理，批处理是定期处理一批。
- **Kafka Streams**：Kafka官方的流处理库，支持实时聚合、过滤、连接等操作，API类似Java 8 Stream。

### 案例：实时词频统计（WordCount）

```java
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import java.util.Arrays;
import java.util.Properties;

public class WordCountStream {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> textLines = builder.stream("input-topic");
        KTable<String, Long> wordCounts = textLines
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                .groupBy((key, word) -> word)
                .count();

        wordCounts.toStream().to("output-topic", Produced.with(Serdes.String(), Serdes.Long()));
        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
    }
}
```

**练习**
- 用Kafka Streams实现实时单词计数：输入topic为“input-topic”，输出topic为“output-topic”，用kafka-console-producer/consumer测试效果。


## 深度讲解

---

### 一、Kafka 分区分配详解

#### 原理
- 一个Topic可以有多个分区（partition），每条消息根据分区策略分配到某一个分区。
- 分区的好处：高并发写入、高并发消费、水平扩展、负载均衡。
- Producer端分区策略：
  - 指定partition参数，直接写入指定分区。
  - 指定key，Kafka对key做hash分配到分区（同key始终到同一分区）。
  - 不指定key和partition，轮询分配到分区。

#### Consumer Group与分区分配
- 同一Consumer Group内的多个consumer会自动均分分区，每个分区同一时刻只会被一个consumer消费。
- Consumer数量小于分区数时，有的consumer一个消费多个分区；多于分区数时，有的consumer空闲。
- 新consumer加入或离开group，会触发**rebalance**，分区会重新分配。

#### 案例

##### 创建多分区Topic
```bash
bin/kafka-topics.sh --create --topic demo-partition --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

##### Producer发送消息到不同分区
```java
// 指定key，观察同key落到同一分区
for (int i = 0; i < 10; i++) {
    String key = "user" + (i % 3);
    String value = "msg" + i;
    producer.send(new ProducerRecord<>("demo-partition", key, value));
}
```

##### 多个Consumer消费同一个Topic
```java
// 启动两个Consumer实例，使用相同group.id
// 观察每个Consumer实际消费的分区
```
**练习**：
- 创建3分区topic，启动2个consumer，分别打印每个consumer实际消费的分区（record.partition()）。
- 杀掉其中一个consumer，观察剩下的consumer是否接管所有分区。

---

### 二、Offset管理进阶

#### 原理
- Offset是每个consumer group在每个分区消费到的位置。
- 自动提交（enable.auto.commit=true）：每隔一段时间自动提交offset，简单但可能重复消费或丢失。
- 手动提交（enable.auto.commit=false）：业务处理成功后手动提交offset，更安全。
- commitSync()：同步提交，保证提交成功。
- commitAsync()：异步提交，性能高但可能丢失。

#### 代码案例

##### 自动提交
```java
props.put("enable.auto.commit", "true");
props.put("auto.commit.interval.ms", "1000");
```

##### 手动提交
```java
props.put("enable.auto.commit", "false");
KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
// ...
consumer.commitSync(); // 处理完一批后同步提交
```

##### 按分区提交
```java
for (TopicPartition partition : records.partitions()) {
    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
    for (ConsumerRecord<String, String> record : partitionRecords) {
        // 处理消息
    }
    long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
    consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
}
```

#### 练习
- 修改consumer代码，模拟业务处理异常时不提交offset，重启后观察消息是否会被重复消费。
- 用commitAsync()提交offset，模拟网络异常，观察提交丢失的可能。

---

### 三、Kafka Streams 复杂案例

#### 1. 实时订单金额汇总（按用户/时间窗口聚合）

##### 需求
- 订单消息格式：`{"userId": "u1", "amount": 100, "ts": 1710000000000}`
- 按用户统计近1分钟的订单总金额，实时输出到另一个topic。

##### 代码示例
```java
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import com.google.gson.*;

import java.time.Duration;
import java.util.Properties;

public class OrderAmountWindowedSum {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-sum-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> orders = builder.stream("order-topic");

        Gson gson = new Gson();

        KTable<Windowed<String>, Double> windowedSum = orders
            .map((k, v) -> {
                JsonObject obj = gson.fromJson(v, JsonObject.class);
                String userId = obj.get("userId").getAsString();
                double amount = obj.get("amount").getAsDouble();
                return KeyValue.pair(userId, amount);
            })
            .groupByKey(Grouped.with(Serdes.String(), Serdes.Double()))
            .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofMinutes(1)))
            .reduce(Double::sum);

        windowedSum.toStream().foreach((windowedKey, sum) -> {
            System.out.printf("User %s, Window %s~%s, Total Amount: %.2f%n",
                windowedKey.key(),
                windowedKey.window().startTime(),
                windowedKey.window().endTime(),
                sum);
        });

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
    }
}
```

#### 2. 实时黑名单过滤

- 有一个黑名单topic，动态维护被禁用用户。
- 实时过滤订单流，黑名单用户的订单不输出。

##### 代码片段
```java
KTable<String, String> blacklist = builder.table("blacklist-topic");
KStream<String, String> orders = builder.stream("order-topic");

orders
    .leftJoin(blacklist, (order, flag) -> flag == null ? order : null)
    .filter((k, v) -> v != null)
    .to("filtered-order-topic");
```

#### 练习
1. 用Kafka Streams实现每分钟统计每个用户的订单数量和总金额。
2. 实现实时黑名单过滤：黑名单topic变更后，订单流自动过滤相关用户。
3. 用窗口聚合实现实时活跃用户数统计（UV）。


---

## 五、Kafka 常见应用场景与最佳实践

### 5.1 典型应用
- 日志收集、用户行为埋点、订单流水、实时监控、异步解耦

### 5.2 最佳实践
- Producer/Consumer参数调优
- Topic命名规范
- 消费端幂等性和容错
- 监控与告警

---

## 5.1 典型应用

### 1. 日志收集

**原理与价值**：
- 各服务/应用将日志（访问、错误、行为等）写入Kafka的日志Topic。
- 下游可用Kafka Connect、Flume、Logstash等消费日志，写入HDFS、ElasticSearch、数据库等。
- 支持高吞吐、低延迟、可扩展的日志收集与分析。

**设计要点**：
- 日志格式标准化（如JSON）。
- Topic可按业务/环境区分（如app-log-prod）。
- 支持多消费者并行处理不同分析任务。

---

### 2. 用户行为埋点

**原理与价值**：
- 前端/后端埋点数据（如页面浏览、点击、购买等）实时写入Kafka。
- 支持实时用户画像、推荐、风控等业务。
- 解耦数据采集与分析，便于扩展。

**设计要点**：
- 每条消息包含userId、eventType、timestamp等。
- 可用Kafka Streams等实现实时聚合、统计。

---

### 3. 订单流水

**原理与价值**：
- 电商、金融等业务的订单创建、支付、退款等事件写入Kafka。
- 消费端异步处理订单（如发货、通知、对账），保证业务解耦和高可用。
- 支持消息持久化和回溯。

**设计要点**：
- 同一订单的消息建议用orderId为key，保证顺序性。
- 多个下游系统可独立消费，实现异步解耦。

---

### 4. 实时监控

**原理与价值**：
- 系统指标、业务事件、告警等实时写入Kafka。
- 下游监控平台（如Prometheus、ELK、Grafana等）消费数据做实时展示和告警。
- 支持大规模多源数据的实时监控和聚合。

**设计要点**：
- 监控数据建议结构化（如JSON）。
- Topic可按监控类型/业务线分组。

---

### 5. 异步解耦

**原理与价值**：
- 各服务通过Kafka异步通信，生产者不直接依赖消费者，提升系统弹性和可扩展性。
- 典型如订单服务写消息到Kafka，发货服务异步消费处理。

**设计要点**：
- Topic设计要明确业务边界。
- 消费端处理需保证幂等性，防止消息重复导致脏数据。

---

## 5.2 最佳实践

### 1. Producer/Consumer参数调优

- **Producer**
  - `acks=all`：保证消息写入所有副本后才确认，提升可靠性。
  - `batch.size`、`linger.ms`：调大批量发送和延迟，提升吞吐。
  - `compression.type`: 使用 `lz4` 或 `snappy` 压缩提升网络效率。
  - `retries`：设置合理重试次数，防止网络抖动导致数据丢失。

- **Consumer**
  - `max.poll.records`：控制每次poll拉取消息数，防止单次拉取过多处理超时。
  - `session.timeout.ms`、`heartbeat.interval.ms`：合理设置，防止consumer频繁rebalance。
  - `enable.auto.commit=false`：手动提交offset，保证业务处理成功后再提交。

---

### 2. Topic命名规范

- 建议：`业务-模块-环境` 或 `domain.event.environment`
  - 例：`order-created-prod`、`user-action-log-dev`
- 便于权限管理、监控、运维。
- 避免生产/测试混用topic，防止数据串扰。

---

### 3. 消费端幂等性和容错

- **幂等性**：业务操作需保证多次消费同一消息不会导致重复写库、扣款等（如用唯一约束、状态表、去重表等）。
- **容错**：捕获并处理异常，避免consumer挂死导致分区无法消费。
- **事务**：可用Kafka事务API实现端到端exactly-once（但业务端也要幂等）。

---

### 4. 监控与告警

- **Lag监控**：监控consumer的Lag（未消费消息数），Lag过大需及时告警处理。
- **Broker健康**：监控Broker状态、分区副本同步（ISR）、磁盘使用等。
- **消息堆积**：监控topic的消息堆积，及时扩容或优化消费逻辑。
- **常用工具**：Kafka Manager、Burrow、Prometheus、Grafana等。

---

## 小结

- Kafka适用于高吞吐、实时、解耦、可扩展的消息场景。
- 生产、消费端参数需按业务调优，topic命名要规范。
- 消费端务必保证幂等，系统需有完善的监控和告警机制。


**练习**
- 设计一个基于Kafka的用户行为日志收集方案
- 总结Kafka在实际开发中遇到的常见问题与解决方法

---

# 综合案例与练习

1. 用Kafka实现一个“生产者-消费者”模型，生产端每秒发送一条带有时间戳的消息，消费端实时打印。
```java
package com.example.kafka.simple;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;
/**
 * @Description 1. 生产者-消费者模型（每秒一条消息，消费端实时打印）
 * Producer
 * @Author miaoyongbin
 * @Date 2025/7/2 10:07:42
 * @Version 1.0
 */
public class SimpleProducer {
    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        for (int i = 1; i <= 20; i++) {
            String msg = "msg-" + i + ", ts=" + System.currentTimeMillis();
            producer.send(new ProducerRecord<>("demo-topic", msg), (metadata, exception) -> {
                if (exception == null) {
                    System.out.printf("Sent: %s, partition: %d, offset: %d%n", msg, metadata.partition(), metadata.offset());
                } else {
                    exception.printStackTrace();
                }
            });
            Thread.sleep(1000); // 每秒一条
        }
        producer.close();
    }
}
```
```java
package com.example.kafka.simple;

import org.apache.kafka.clients.consumer.*;
import java.time.Duration;
import java.util.*;
/**
 * @Description 简单消费
 * @Author miaoyongbin
 * @Date 2025/7/2 10:08:22
 * @Version 1.0
 */
public class SimpleConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "simple-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("demo-topic"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                System.out.println("Received: " + record.value());
            }
        }
    }
}
```
2. 设计并实现一个订单消息异步处理系统：下单消息写入Kafka，消费者异步处理并打印。
```java
package com.example.kafka.order;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;
/**
 * @Description 2. 订单消息异步处理系统
 * 订单生产者
 * @Author miaoyongbin
 * @Date 2025/7/2 10:09:09
 * @Version 1.0
 */
public class OrderProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        for (int i = 1; i <= 10; i++) {
            String order = String.format("{\"orderId\":\"O%d\",\"amount\":%.2f,\"ts\":%d}",
                    i, Math.random() * 100, System.currentTimeMillis());
            producer.send(new ProducerRecord<>("order-topic", order));
            System.out.println("下单消息: " + order);
        }
        producer.close();
    }
}
```
```java
package com.example.kafka.order;

import org.apache.kafka.clients.consumer.*;
import java.time.Duration;
import java.util.*;
/**
 * @Description 订单消费者（异步处理）
 * @Author miaoyongbin
 * @Date 2025/7/2 10:09:32
 * @Version 1.0
 */

public class OrderConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "order-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("order-topic"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                System.out.println("处理订单: " + record.value());
            }
        }
    }
}
```
3. 用Kafka Streams实现实时统计某topic中每个单词出现的次数，并输出到另一个topic。
```java
package com.example.kafka.wordcount;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;
/**
 * @Description 3. Kafka Streams 实时单词计数
 * Producer（生成英文句子）
 * @Author miaoyongbin
 * @Date 2025/7/2 10:10:36
 * @Version 1.0
 */
public class WordProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        String[] sentences = {
                "Kafka Streams is powerful",
                "Kafka is a distributed streaming platform",
                "Streams API is easy to use",
                "Hello Kafka"
        };

        for (String sentence : sentences) {
            producer.send(new ProducerRecord<>("input-topic", sentence));
            System.out.println("发送句子: " + sentence);
        }
        producer.close();
    }
}
```
```java
package com.example.kafka.wordcount;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import java.util.Arrays;
import java.util.Properties;
/**
 * @Description Kafka Streams 实时统计
 * @Author miaoyongbin
 * @Date 2025/7/2 10:11:18
 * @Version 1.0
 */
public class WordCountStream {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-app");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> textLines = builder.stream("input-topic");
        KTable<String, Long> wordCounts = textLines
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                .groupBy((key, word) -> word)
                .count();

        wordCounts.toStream().to("output-topic", Produced.with(Serdes.String(), Serdes.Long()));

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();
    }
}
```
```java
package com.example.kafka.wordcount;

import org.apache.kafka.clients.consumer.*;
import java.time.Duration;
import java.util.*;
/**
 * @Description Consumer（统计结果）
 * @Author miaoyongbin
 * @Date 2025/7/2 10:12:02
 * @Version 1.0
 */
public class WordCountConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "wordcount-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.LongDeserializer");

        KafkaConsumer<String, Long> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("output-topic"));

        while (true) {
            ConsumerRecords<String, Long> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, Long> record : records) {
                System.out.printf("单词: %s, 次数: %d%n", record.key(), record.value());
            }
        }
    }
}
```
4. 设计一个“延迟消息”场景，用Kafka实现消息延迟消费。
```java
package com.example.kafka.delay;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;
/**
 * @Description  4. Kafka实现延迟消息（简单版）
 * Producer（带延迟时间戳）
 * @Author miaoyongbin
 * @Date 2025/7/2 10:12:55
 * @Version 1.0
 */
public class DelayProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        long deliverTs = System.currentTimeMillis() + 5000; // 5秒后
        String msg = "{\"orderId\":\"O1234\",\"deliverTs\":" + deliverTs + "}";
        producer.send(new ProducerRecord<>("delay-topic", msg));
        System.out.println("发送延迟消息: " + msg);
        producer.close();
    }
}
```
```java
package com.example.kafka.delay;

import org.apache.kafka.clients.consumer.*;
import org.json.JSONObject;
import java.time.Duration;
import java.util.*;
/**
 * @Description Consumer（判断时间到再处理）
 * @Author miaoyongbin
 * @Date 2025/7/2 10:13:28
 * @Version 1.0
 */
public class DelayConsumer {
    public static void main(String[] args) throws InterruptedException {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "delay-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("delay-topic"));

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                JSONObject obj = new JSONObject(record.value());
                long deliverTs = obj.getLong("deliverTs");
                long now = System.currentTimeMillis();
                if (now < deliverTs) {
                    Thread.sleep(deliverTs - now);
                }
                System.out.println("延迟消费: " + obj.getString("orderId") + ", 实际消费时间: " + System.currentTimeMillis());
            }
        }
    }
}
```
5. 编写代码测试分区与Consumer Group的负载均衡和Rebalance机制。
```java
package com.example.kafka.partition;

import org.apache.kafka.clients.producer.*;
import java.util.Properties;
/**
 * @Description
 *  5. 分区与Consumer Group负载均衡和Rebalance机制测试
 *  Producer（生成伪数据）
 * @Author miaoyongbin
 * @Date 2025/7/2 10:14:34
 * @Version 1.0
 */
public class PartitionProducer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);

        for (int i = 1; i <= 30; i++) {
            String key = "user" + (i % 3); // 3种key
            String value = "msg" + i;
            producer.send(new ProducerRecord<>("rebalance-topic", key, value));
        }
        producer.close();
    }
}
```
```java
package com.example.kafka.partition;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import java.time.Duration;
import java.util.*;
/**
 * @Description Consumer（打印自己消费的分区）
 * 你可以开多个终端运行PartitionConsumer，观察控制台分区分配的变化，kill掉某个consumer再看分区如何被重新分配。
 * @Author miaoyongbin
 * @Date 2025/7/2 10:15:16
 * @Version 1.0
 */
public class PartitionConsumer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("group.id", "rebalance-group");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("rebalance-topic"), new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                System.out.println("Assigned partitions: " + partitions);
            }
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                System.out.println("Revoked partitions: " + partitions);
            }
        });

        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
            for (ConsumerRecord<String, String> record : records) {
                System.out.printf("Consumer %s, partition %d, value: %s%n",
                        Thread.currentThread().getName(), record.partition(), record.value());
            }
        }
    }
}
```