
# Flink 教学大纲与案例设计

> 配套代码：`src/main/java/com/example/demo/flink/`
> 配套 Docker：`docker/flink/`
> Web 入口：<http://localhost:8090/flink/wordcount> · `/flink/window` · `/flink/state` · `/flink/cep`

---

## 一、Flink 基础与原理

### 1.1 Flink 是什么？

Flink 是一个**流优先（stream-first）的分布式计算引擎**：

> 在 Flink 看来，**批处理只是一种"有界流"的特殊情况**。因此一套 API 能既写流又写批。

| 维度       | Flink                          | Spark Streaming                |
|------------|-------------------------------|---------------------------------|
| 执行模型   | True streaming（事件驱动）     | Micro-batch（攒批）              |
| 延迟       | 毫秒级                          | 秒级                             |
| 吞吐       | 高（背压 + 异步 IO）            | 高                               |
| 状态       | 原生大状态（RocksDB），TB 级     | DStream 状态有限                 |
| 事件时间   | 第一公民，watermark 完善         | 后期补齐，能用                   |
| 一致性     | Exactly-once（含端到端，2PC）   | Exactly-once（依赖幂等 sink）   |
| 典型场景   | 实时风控、实时大屏、CEP         | 离线 + 微批一体                  |

### 1.2 适用场景

- **实时监控 / 大屏**：双 11 实时 GMV、UV、Top N 商品
- **实时风控**：反欺诈、连续异常检测（CEP 看家本领）
- **实时 ETL**：Kafka → Flink → ClickHouse / Iceberg
- **机器学习实时特征**：在线特征计算 + 模型推理
- **数据湖入仓**：Flink CDC 把 MySQL 变更实时同步到 Iceberg / Hudi

---

## 二、Flink 架构与核心概念

### 2.1 整体架构

```
+-----------------+        提交 Job       +-----------------+
|     Client      | --------------------> |   JobManager    |
| (flink run / SQL|                       | (Master 节点)    |
|  Client)        |                       | - JobMaster     |
+-----------------+                       | - ResourceManager|
                                          | - Dispatcher    |
                                          +--------+--------+
                                                   |
                                                   v 调度 Task
                          +------------------------+----------------------+
                          |                                                |
                          v                                                v
                +-----------------+                            +-----------------+
                |   TaskManager   |                            |   TaskManager   |
                | (Worker 节点)    |                            | (Worker 节点)    |
                | Task Slot 1     |                            | Task Slot 1     |
                | Task Slot 2     |                            | Task Slot 2     |
                +-----------------+                            +-----------------+
```

- **JobManager**：协调，包括 JobMaster（管 Job）+ ResourceManager（管 Slot）+ Dispatcher（提供 REST）
- **TaskManager**：真正干活的 Worker，里面有若干 **Task Slot**（一个 slot 对应一组并发任务的内存份额）
- **Slot**：TM 的资源切分单位。同一个 job 的不同算子可以共享 slot（**Slot Sharing**），降低开销

### 2.2 DataStream API 与执行图

用户程序 → 4 张图（**最重要的概念之一**）：

```
StreamGraph  →  JobGraph  →  ExecutionGraph  →  Physical Graph
（程序逻辑图）   （优化后逻辑图）  （并行任务图）      （部署到 TaskManager）
```

- **StreamGraph**：算子节点，按写代码顺序连起来
- **JobGraph**：把可以"算子链"（Operator Chain）的串起来，减少线程切换和序列化
- **ExecutionGraph**：根据并行度展开成多个并行子任务（subtask）
- **Physical Graph**：实际部署在哪个 TM 哪个 Slot 上

### 2.3 时间语义（Flink 招牌特性）

| 语义           | 含义                                    | 何时用                       |
|----------------|----------------------------------------|------------------------------|
| Event Time     | 事件本身发生的时间（消息里的 ts 字段）   | 默认推荐，能正确处理乱序     |
| Processing Time| 算子机器收到事件的时间                  | 不关心精度的简单聚合          |
| Ingestion Time | 进入 Flink 的时间                       | 介于两者之间，少用            |

#### Watermark 是什么？

**Watermark = 事件时间的进度指示器**。Watermark = T 含义："系统认为不会再有事件时间 ≤ T 的数据"。

```java
WatermarkStrategy.<Event>forBoundedOutOfOrderness(Duration.ofSeconds(5))
    .withTimestampAssigner((e, ts) -> e.timestamp);
```

- `forBoundedOutOfOrderness(5s)`：允许 5 秒乱序
- 窗口：事件时间 < watermark 的窗口才会关闭、触发计算
- 迟到事件：默认丢弃，可以走 `sideOutputLateData(...)` 收到侧输出

### 2.4 窗口（Window）

| 窗口类型      | 例子                                      |
|---------------|-------------------------------------------|
| Tumbling 滚动 | 每 5 秒一个窗口，不重叠                  |
| Sliding 滑动  | 每 5 秒触发一次，覆盖最近 1 分钟         |
| Session 会话  | 用户 30 秒内无操作就关窗（变长）         |
| Global 全局   | 永不关闭，需自己写 trigger（少用）        |

代码模式：

```java
stream
  .keyBy(e -> e.user)
  .window(TumblingEventTimeWindows.of(Time.seconds(5)))
  .process(new MyWindowFunction());
```

### 2.5 状态（State）—— Flink 的灵魂

Flink 的状态是**算子内部的、按 key 隔离的、可以容错的**。

| 状态类型           | 用法                                       |
|--------------------|--------------------------------------------|
| `ValueState<T>`    | 每个 key 一个值（最常用）                  |
| `ListState<T>`     | 每个 key 一个列表（如最近 N 条事件）        |
| `MapState<K,V>`    | 每个 key 一个 Map（如多维度聚合）          |
| `ReducingState<T>` | 自动聚合的状态                             |
| `AggregatingState<IN,OUT>` | 自定义聚合状态                     |

**State Backend**：存到哪儿？
- **HashMapStateBackend**：堆内存，快但状态不能太大
- **EmbeddedRocksDBStateBackend**：本地 RocksDB（堆外），TB 级状态都能扛

### 2.6 容错机制：Checkpoint + Savepoint

#### Checkpoint（自动周期触发）
- JobManager 每隔 N 秒发起一次"分布式快照"
- 用 **Chandy-Lamport 算法**：在数据流里插入 Barrier，每个算子收到 barrier 时把自己的 state 拍照
- 故障恢复时，所有算子回到上一个 checkpoint 的状态，输入源也回退到对应的 offset

#### Savepoint（手动）
- 升级版的 checkpoint，专门为版本升级 / 修改并行度设计
- 升级时：`stop with savepoint` → 改代码 → `start from savepoint`

#### Exactly-Once 怎么做的？

- **Source 端**：Kafka source 把 offset 存进 checkpoint
- **算子之间**：barrier 对齐保证状态一致
- **Sink 端**：要么用幂等 sink（如 MySQL upsert），要么用 **2PC（两阶段提交）**：
  - Pre-commit：写到外部系统但不提交（如 Kafka 事务、MySQL 事务）
  - Commit：等 checkpoint 完成才正式提交

---

## 三、运行 Demo 的步骤

### 3.1 启动 Docker Flink
```bash
docker compose -f docker/flink/docker-compose.yml up -d
# Flink Web UI：http://localhost:8082
```

### 3.2 启动 Spring Boot
```bash
mvn spring-boot:run
# 然后访问：
# http://localhost:8090/flink/wordcount   —— DataStream WordCount
# http://localhost:8090/flink/window      —— Tumbling Window + Watermark
# http://localhost:8090/flink/state       —— Keyed State + Timer 会话超时
# http://localhost:8090/flink/cep         —— CEP 连续登录失败检测
```

### 3.3 命令行提交（让学生体验集群作业）
```bash
mvn -DskipTests package
docker cp target/demo-0.0.1-SNAPSHOT.jar flink-jobmanager:/opt/job.jar
docker exec flink-jobmanager flink run \
  -c com.example.demo.flink.demo.WordCountJob /opt/job.jar
# 然后到 http://localhost:8082 看 Job
```

---

## 四、案例 1：DataStream WordCount

代码：`com.example.demo.flink.demo.WordCountJob` + `FlinkWordCountController`

```java
DataStream<Tuple2<String, Integer>> counts = source
    .flatMap((line, out) -> { for (String w: line.split("\\W+")) out.collect(Tuple2.of(w,1)); })
    .returns(Types.TUPLE(Types.STRING, Types.INT))
    .keyBy(t -> t.f0)        // 按单词 hash 分区
    .sum(1);                 // 每个 key 维护一份 ValueState 累加
```

教学要点：
- `keyBy` 触发 hash 分区（**不是 shuffle 而是 redistribute**）
- `sum`/`reduce` 内部就是状态算子
- `env.execute()` 才真正提交 Job（与 Spark 的 action 类似）

---

## 五、案例 2：Tumbling Window + Watermark

代码：`FlinkWindowController` + 模板 `flink/window_demo.html`

```java
WatermarkStrategy<ClickEvent> wm = WatermarkStrategy
    .<ClickEvent>forBoundedOutOfOrderness(Duration.ofSeconds(1))
    .withTimestampAssigner((e, ts) -> e.eventTime);

source.assignTimestampsAndWatermarks(wm)
      .keyBy(e -> e.user)
      .window(TumblingEventTimeWindows.of(Time.seconds(5)))
      .process(new CountByWindow());   // 输出 (windowEnd, user, count)
```

教学要点：
1. **窗口什么时候触发？**：Watermark > windowEnd 时
2. 浏览器点 3 次 alice → 等 5 秒以内必有结果（窗口已过 watermark）
3. 看 Flink Web UI（8082）的 Job 详情，每个 task 的 Watermark 列在动

---

## 六、案例 3：Keyed State + Timer

代码：`FlinkStateController`（会话超时检测）

```java
public class SessionTimeoutFn extends KeyedProcessFunction<String, Event, Map> {
    private transient ValueState<Long> lastSeen;

    public void open(Configuration cfg) {
        lastSeen = getRuntimeContext().getState(
            new ValueStateDescriptor<>("last-seen", Types.LONG));
    }

    public void processElement(Event e, Context ctx, Collector<Map> out) {
        lastSeen.update(e.ts);
        ctx.timerService().registerProcessingTimeTimer(System.currentTimeMillis() + 10_000);
        // 输出 ACTIVE
    }

    public void onTimer(long ts, OnTimerContext ctx, Collector<Map> out) {
        if (System.currentTimeMillis() - lastSeen.value() >= 10_000) {
            // 输出 TIMEOUT，清状态
            lastSeen.clear();
        }
    }
}
```

教学要点：
1. **状态是按 key 隔离的**：alice 的 lastSeen 与 bob 的互不影响
2. **定时器有两种**：processing-time / event-time
3. **状态超时（TTL）**：生产中常用 `StateTtlConfig`，避免状态无限膨胀

---

## 七、案例 4：CEP 复杂事件处理

代码：`FlinkCepController`（连续 3 次登录失败 → 风控告警）

```java
Pattern<LoginEvent, ?> pattern = Pattern.<LoginEvent>begin("first")
    .where(e -> "FAIL".equals(e.result))
    .next("second").where(e -> "FAIL".equals(e.result))
    .next("third").where(e -> "FAIL".equals(e.result))
    .within(Time.seconds(30));

CEP.pattern(source.keyBy(e -> e.user), pattern)
   .select(events -> { /* 输出告警 */ });
```

教学要点：
- `next` 要求**严格连续**（中间不能有别的事件），`followedBy` 允许中间有不匹配的
- `within(30s)` 是窗口约束
- CEP 底层是 NFA，性能好

> 改进：可以用 `.times(3).consecutive().within(30s)` 写得更紧凑。

---

## 八、Flink 调优速查

| 问题                   | 处理思路                                           |
|------------------------|---------------------------------------------------|
| 反压（Backpressure）   | 看 Web UI 哪一级 BUSY 红了，优化下游或加并行度    |
| Checkpoint 失败 / 慢   | 缩小 state 大小、用 RocksDB、开增量 checkpoint     |
| 数据倾斜               | 二阶段聚合（先 hash key 加盐 → 再去盐聚合）       |
| 状态过大               | 启用 State TTL；分区 key 设计避免热 key          |
| Watermark 不推进       | 检查空分区（idle source），用 `withIdleness(Duration)` |
| Kafka 消费跟不上        | 加并行度（≤ Kafka partition 数）；调 `fetch.max.bytes` |
| 频繁 Full GC           | 用 G1，状态搬到 RocksDB（堆外）                    |

---

## 九、Flink 高频面试题

### Q1：Flink 与 Spark Streaming 最本质的区别？

- **执行模型不同**：Flink 是真流（事件驱动，一条来就处理一条），Spark Streaming 是 micro-batch（攒成小批）
- **状态原生**：Flink 把状态当一等公民（ValueState/ListState/MapState），有 RocksDB 后端能扛 TB 状态
- **事件时间成熟**：Flink 一开始就为流而生，Watermark / 乱序处理 / 迟到数据全套支持
- **延迟 / 吞吐**：Flink 毫秒级，Spark Streaming 秒级（Continuous mode 实验性）

### Q2：Watermark 是什么？怎么生成？

Watermark 是事件时间的进度标记。Watermark = T 表示**系统认为不会再来事件时间 ≤ T 的数据**。

生成策略（`WatermarkStrategy`）：
- `forMonotonousTimestamps()`：事件时间单调递增（很少见）
- `forBoundedOutOfOrderness(5s)`：允许 5 秒乱序，最常用
- 自定义：实现 `WatermarkGenerator`

进窗口 / 触发器 / 定时器都靠 watermark 推进。

### Q3：Flink 的状态有哪几种？分别什么场景？

按可见性分两类：
- **Keyed State**：按 key 隔离，最常用（ValueState/ListState/MapState/ReducingState/AggregatingState）
- **Operator State**：算子级（不分 key），如 Kafka Source 存 offset

按存储分：
- **HashMapStateBackend**：堆内存，吞吐高，状态不能太大
- **EmbeddedRocksDBStateBackend**：本地 LSM 树，TB 级状态，吞吐略低，**生产首选**

### Q4：Checkpoint 怎么做的？Exactly-Once 怎么实现？

**Checkpoint** = 分布式快照：
1. JobManager 触发，向所有 source 注入 barrier（带 checkpoint id）
2. 算子收到所有上游 barrier 后，把 state 异步刷到外部存储（HDFS/S3）
3. 都成功 → 标记 checkpoint 完成；任一失败 → 整个回滚到上一个完成的 checkpoint

**Exactly-once 三道防线**：
- Source：Kafka offset 进 checkpoint（重启回放）
- 算子内部：barrier 对齐保证状态一致
- Sink：幂等（upsert）或 2PC（pre-commit + commit on checkpoint complete）

### Q5：为什么 Flink 同一个 Job 的不同算子可以共享 slot？

**Slot Sharing**：同一 job 的不同算子的子任务可以放进同一个 slot，目的：
1. **资源利用率高**：一些算子是 IO 密集，另一些是 CPU 密集，搭配跑满 slot
2. **简化资源规划**：用户只需关心"最大并行度"，不用算每个算子要多少 slot
3. **数据交换更便宜**：同 slot 的算子走 in-memory channel，不用网络

### Q6：window 三个核心：Trigger / Evictor / Allowed Lateness？

- **Trigger**：窗口什么时候关闭并触发计算（默认 EventTimeTrigger，watermark 越过 windowEnd 触发）
- **Evictor**：触发前先剔除一些元素（很少自定义）
- **Allowed Lateness**：迟到数据允许"开窗回算"的容忍期；超过这个再迟到只能进侧输出

### Q7：keyBy 之后会发生什么？

`keyBy` 是按 hash 把数据**重分发到下游算子的并行子任务**：
- 同一个 key 的所有事件 → 同一个子任务
- 状态按 key 隔离访问（`ValueState` 等都是 keyed state）
- 物理层面：**redistribute**（不是 sort-based shuffle，比 Spark 轻量）

### Q8：Flink 处理迟到数据的策略？

3 道防线：
1. **Watermark 容忍乱序**（`forBoundedOutOfOrderness`）
2. **Allowed Lateness**：窗口已关，但允许再迟到 N 秒触发增量计算
3. **Side Output**：再迟到就走侧输出 tag，单独处理 / 报警

### Q9：CEP 与传统的 SQL/状态机相比有什么优势？

- 用**声明式 Pattern API**：`begin().next().where().within()` 直接读得懂业务规则
- 底层 NFA：高效跟踪潜在匹配前缀，比手写 KeyedProcessFunction 状态机优雅且不容易写错
- 支持复杂模式：`oneOrMore() / times(N) / consecutive() / followedBy / notFollowedBy`

### Q10：Flink SQL / Table API 是干什么的？

- 在 DataStream 之上的**关系型 API**：写 SQL 就能做流处理 / 批处理 / 流批一体
- 配合 **Catalog**（Hive / Iceberg / Paimon）就成了实时数仓的核心引擎
- **CDC 入仓**：`CREATE TABLE ... WITH ('connector'='mysql-cdc')` 一句话把 MySQL 变更流接进来

### Q11：Flink 的反压怎么排查？

1. Web UI 看每个 task 的 BUSY / IDLE 百分比，红的是瓶颈点
2. 看 Backpressure 标签页（采样 100 个线程 stack）
3. 常见原因：sink 写得慢、外部 RPC 慢、单 key 倾斜
4. 处理：加并行度（≤ source partition 数）、异步 IO（`AsyncDataStream`）、key 加盐再去盐

### Q12：什么时候用 Spark，什么时候用 Flink？

- **离线 ETL / 数仓 / Spark SQL 生态** → Spark
- **实时低延迟 / 实时风控 / CEP / 大状态** → Flink
- **流批一体 + 数据湖** → 两者都行（Flink 偏流，Spark 偏批）

---

## 十、推荐学习路径

1. **第 1 周**：跑通 `WordCountJob` 与 Web demo，熟悉 DataStream API 三件套（map/keyBy/sum）
2. **第 2 周**：跑 Window demo，体会 EventTime + Watermark + Tumbling Window
3. **第 3 周**：跑 Keyed State + Timer demo，理解 ValueState 与定时器；尝试改成 ListState / MapState
4. **第 4 周**：跑 CEP demo，把规则改成"5 分钟内 5 次以上下单 + 1 次退款" 这类业务规则
5. **第 5 周**：连 Kafka，写一个 Kafka → Flink 窗口聚合 → MySQL/Redis 的端到端 Job
6. **第 6 周**：用 Flink SQL 重写第 5 周的 Job，体会"流批一体 + 声明式" 的威力

> 面试前**必看**：Watermark 原理、Checkpoint 算法、Exactly-Once 实现、State Backend 选型、反压排查——这 5 个是 Flink 岗位重灾区。
