
# Spark 教学大纲与案例设计

> 配套代码：`src/main/java/com/example/demo/spark/`
> 配套 Docker：`docker/spark/`
> Web 入口：<http://localhost:8090/spark/wordcount> · `/spark/sql` · `/spark/streaming>

---

## 一、Spark 基础与原理

### 1.1 Spark 是什么？

Spark 是一个**统一的大数据分析引擎**，一套 API 同时支持批处理、SQL、流处理、机器学习、图计算。

| 维度       | Hadoop MapReduce        | Spark                              |
|------------|------------------------|-------------------------------------|
| 计算模型   | Map → Disk → Reduce    | DAG（有向无环图）+ 内存计算         |
| 中间结果   | 落磁盘                 | 默认在内存（不够再落盘）            |
| 性能       | 慢，适合一次性 ETL      | 快 10-100 倍（同任务）              |
| 编程       | 只能写 MR              | RDD/DataFrame/SQL/Streaming/MLlib   |

### 1.2 适用场景

- **离线 ETL / 数据清洗**：每天 / 每小时跑一遍的批任务
- **交互式分析**：Spark SQL + Hive / Iceberg / Delta Lake
- **实时计算**：Structured Streaming，秒-分钟级延迟（Flink 是亚秒级）
- **机器学习**：MLlib + Spark on K8s 跑特征工程 / 训练
- **数据湖**：Spark + Parquet/ORC/Iceberg/Hudi/Delta 是事实标准

---

## 二、Spark 架构与运行原理

### 2.1 核心组件

```
+----------------+        提交Job          +-----------------+
|   Driver       |  ---------------------> |  Cluster Manager|
| (SparkContext) |                         | (Standalone/Yarn|
+--------+-------+                         |  /K8s/Mesos)    |
         |                                 +--------+--------+
         | 调度 Task                                |
         v                                          v
+--------+-------+   +-----------------+   +-----------------+
|   Executor 1   |   |   Executor 2    |   |   Executor N    |
| (Task线程池+缓存)|   | (Task线程池+缓存) |   | (Task线程池+缓存) |
+----------------+   +-----------------+   +-----------------+
```

- **Driver**：跑用户的 main()，负责构建 DAG、调度 Stage/Task
- **Executor**：在 Worker 上启动的 JVM 进程，跑 Task，缓存数据
- **Cluster Manager**：分配资源（教学环境用 Standalone，生产常用 YARN/K8s）
- **SparkContext / SparkSession**：用户写代码的入口；2.0 后推荐用 `SparkSession`

### 2.2 RDD：弹性分布式数据集

RDD 是 Spark 最底层的抽象，核心特点：
1. **不可变**（只能转换出新 RDD，不能改原有）
2. **分区**（数据切成多个 Partition，分布在不同 Executor 上）
3. **血统（Lineage）**：每个 RDD 记得自己是怎么来的，丢了能重算
4. **懒执行**：transformation 不立即跑，遇到 action 才触发

#### Transformation vs Action

| 类型 | 例子                                           | 是否触发计算 |
|------|------------------------------------------------|--------------|
| Transformation | `map / flatMap / filter / reduceByKey / join / groupBy` | 否（构建 DAG） |
| Action         | `collect / count / take / saveAsTextFile / foreach`     | 是（提交 Job） |

#### 窄依赖 vs 宽依赖（核心面试点）

- **窄依赖**（Narrow）：每个父分区被**至多一个**子分区使用 → `map / filter / union`
  - 同一 Stage 内 pipeline 执行，**不需要 shuffle**
- **宽依赖**（Wide）：父分区被**多个**子分区使用 → `groupByKey / reduceByKey / join`
  - **会触发 shuffle**，划分新 Stage，性能瓶颈所在

> 一句话记住：**Stage 的边界就是 shuffle**。

### 2.3 DataFrame / Dataset / Spark SQL

- **DataFrame** = RDD + Schema，列式访问，借助 **Catalyst 优化器** 把 SQL/DSL 翻译成最优物理计划
- **Dataset** = DataFrame + 强类型（Java/Scala 才有），Python 没有
- **Spark SQL** = 同一份数据可以用 SQL 查、也可以用 DataFrame DSL 查，最终走的是同一套 Catalyst 优化

为什么 DataFrame 比 RDD 快？
- 列式存储 → 只读需要的列
- Catalyst 优化器：谓词下推、列裁剪、Join 重排序
- Tungsten 执行引擎：堆外内存 + 全阶段代码生成（whole-stage codegen）

### 2.4 Spark Streaming vs Structured Streaming

| 维度        | Spark Streaming（旧） | Structured Streaming（新，主推） |
|-------------|----------------------|----------------------------------|
| API         | DStream（基于 RDD）   | Dataset/DataFrame                |
| 模型        | Micro-batch           | Micro-batch（默认）/ Continuous |
| 一致性      | At-least-once         | Exactly-once（端到端，配合幂等 sink） |
| 事件时间    | 弱                    | 原生支持，watermark 机制         |

---

## 三、运行 Demo 的步骤

### 3.1 启动 Docker Spark
```bash
docker compose -f docker/spark/docker-compose.yml up -d
# 看 Master UI：http://localhost:8088
```

### 3.2 启动 Spring Boot
```bash
mvn spring-boot:run
# 然后访问：
# http://localhost:8090/spark/wordcount   —— RDD WordCount
# http://localhost:8090/spark/sql         —— DataFrame + Spark SQL
# http://localhost:8090/spark/streaming   —— Structured Streaming 消费 Kafka
```

### 3.3 命令行提交（让学生体验集群作业）
```bash
mvn -DskipTests package
docker cp target/demo-0.0.1-SNAPSHOT.jar spark-master:/opt/jobs/demo.jar
docker exec spark-master /opt/spark/bin/spark-submit \
  --master spark://spark-master:7077 \
  --class com.example.demo.spark.demo.RddWordCount \
  /opt/jobs/demo.jar
```

---

## 四、案例 1：RDD WordCount 与懒执行

代码：`com.example.demo.spark.demo.RddWordCount` + `SparkWordCountController`

教学要点：
1. `flatMap → mapToPair → reduceByKey → collect` 是 Spark 最经典的"4 步走"
2. **`reduceByKey` 比 `groupByKey` 快**：前者在 shuffle 前会做本地 combine
3. 在 Web 控制器里反复点"跑一下"按钮，看 Spark UI（4041/4040）的 Job/Stage 增长

```java
JavaPairRDD<String, Integer> counts = linesRdd
    .flatMap(line -> Arrays.asList(line.toLowerCase().split("\\W+")).iterator())
    .filter(w -> !w.isBlank())
    .mapToPair(w -> new Tuple2<>(w, 1))
    .reduceByKey(Integer::sum);   // 本地预聚合 + shuffle

counts.collect().forEach(...);    // action：触发 DAG 提交
```

---

## 五、案例 2：DataFrame + Spark SQL 学生成绩多维分析

代码：`com.example.demo.spark.web.SparkSqlController` + 模板 `spark/sql_demo.html`

预设了几条递进式 SQL，覆盖：
- 全表扫描 / `GROUP BY` 平均分
- 班级 × 科目交叉聚合
- 窗口函数 `RANK() OVER (PARTITION BY subject ORDER BY score DESC)`
- 行转列 `MAX(CASE WHEN ...)` 经典面试题

每次执行都会展示 `result.queryExecution().simpleString()` 的物理计划，让学生看：
- `HashAggregate`：聚合算子
- `Exchange`：shuffle 触发点
- `SortMergeJoin / BroadcastHashJoin`：Join 策略

---

## 六、案例 3：Structured Streaming 消费 Kafka

代码：`com.example.demo.spark.web.SparkStreamingController` + 模板 `spark/streaming_demo.html`

```java
spark.readStream().format("kafka")
     .option("kafka.bootstrap.servers", "localhost:9092")
     .option("subscribe", "spark-input")
     .load()
     .selectExpr("CAST(value AS STRING) as line")
     .selectExpr("explode(split(lower(line), '\\\\W+')) as word")
     .filter("length(word) > 0")
     .groupBy("word").count()
     .writeStream().outputMode("complete")
     .trigger(Trigger.ProcessingTime("500 milliseconds"))
     .foreachBatch((batch, batchId) -> { ... ws.send(...) })
     .start();
```

教学要点：
1. **流批一体**：同一套 DataFrame API，把 `read` 换成 `readStream` 就成了流
2. **Output Mode**：
   - `append`：只输出新行，适合无聚合
   - `update`：输出有变化的行
   - `complete`：每个 batch 输出全量，适合 grouped aggregation
3. **`foreachBatch`**：把每个 micro-batch 当成普通 DataFrame，可以走 JDBC sink、推 WebSocket，自由度极高
4. **Watermark**：处理迟到数据，本案例为简化没用

---

## 七、Spark 调优速查

| 问题                          | 处理思路                                           |
|-------------------------------|---------------------------------------------------|
| 任务很慢，stage 卡在 shuffle  | 检查数据倾斜：先 `salt key`，或开启 AQE            |
| OOM（OutOfMemory）            | 调小 `spark.sql.shuffle.partitions`，或加 executor 内存 |
| Task 数太多 / 太少            | 设置 `spark.sql.shuffle.partitions`（默认 200）   |
| 频繁 GC                       | `spark.executor.memoryOverhead` 调高，或换 G1 GC  |
| 小文件太多                    | `coalesce(N)` 或 `repartition(N)` 后再写出       |
| Join 慢                       | 小表广播：`spark.sql.autoBroadcastJoinThreshold`，或手动 `broadcast()` |
| Skew Join                     | AQE 的 `spark.sql.adaptive.skewJoin.enabled=true` |

> **AQE（Adaptive Query Execution）** 是 3.0+ 的杀手锏：运行期根据真实数据量自动调分区数、自动转 broadcast、自动拆 skew partition，**默认开启**。

---

## 八、Spark 高频面试题

### Q1：RDD vs DataFrame vs Dataset 区别？

| 维度        | RDD        | DataFrame        | Dataset          |
|-------------|-----------|------------------|------------------|
| 数据结构    | 任意对象  | Row + Schema     | 强类型对象 + Schema |
| 优化器      | 无         | Catalyst          | Catalyst          |
| 序列化      | Java/Kryo  | Tungsten 列式    | Tungsten 列式    |
| 类型安全    | 是（编译期）| 否（运行期）      | 是（编译期）      |
| 推荐使用    | 极少数特殊场景 | Python/SQL 主流 | Java/Scala 主流  |

### Q2：宽依赖、窄依赖，与 Stage 划分关系？

- 窄依赖：父分区 → 子分区 是 1:1 或 N:1（同一 partition 内）
- 宽依赖：父分区 → 子分区 是 N:N（要 shuffle）
- **DAGScheduler 看到宽依赖就切 Stage**：每个 Stage 内只有窄依赖可以 pipeline 执行

### Q3：`reduceByKey` 和 `groupByKey` 区别？

`reduceByKey` 在 shuffle 前在本地先做一次 reduce（**combiner**），传输的数据量大幅减少；
`groupByKey` 把所有相同 key 的数据全部 shuffle 过去再聚合，容易 OOM。
**能用 `reduceByKey` / `aggregateByKey` 就不要用 `groupByKey`**。

### Q4：Spark 的内存管理？

3.0+ 是**统一内存管理**：Storage（缓存）和 Execution（shuffle/sort）共享一块内存：
- 总内存 = `spark.executor.memory` × `spark.memory.fraction`（默认 0.6）
- Storage 默认占 50%（`spark.memory.storageFraction`）
- Execution 不够时可以**抢占 Storage 缓存**（被 evict 的数据按 LRU 淘汰）

### Q5：Spark 怎么实现容错？

- **RDD Lineage**：丢了某个分区，靠血统从父 RDD 重算
- **Checkpoint**：把 RDD 物化到可靠存储（HDFS / S3），切断血统避免无限回溯
- **WAL（Write Ahead Log）**：Streaming 接收到的数据先写日志，崩了能重放
- **Executor 失败**：Driver 把 Task 调度到其他 Executor 上重跑

### Q6：Spark Streaming 与 Flink 比？

| 维度       | Spark Streaming      | Flink                   |
|------------|----------------------|--------------------------|
| 模型       | Micro-batch          | True streaming           |
| 延迟       | 秒级                 | 亚秒级（毫秒）           |
| 状态       | 简单                 | RocksDB + 大状态         |
| 事件时间   | 支持但晚加入          | 一开始就有，更成熟       |
| 一致性     | Exactly-once（限制多）| Exactly-once（默认）     |
| 生态       | 与 Spark 批生态完美整合 | 流原生强，批生态弱      |

### Q7：什么是 Catalyst 优化器？

Spark SQL 的查询优化器，分 4 阶段：
1. **Analysis**：解析 SQL → 解决表名、列名
2. **Logical Optimization**：基于规则的优化（谓词下推、列裁剪、常量折叠）
3. **Physical Planning**：选物理算子（如 BroadcastHashJoin vs SortMergeJoin）
4. **Code Generation**：Tungsten 把整个 stage 编译成一个 Java 函数（whole-stage codegen）

### Q8：Spark on YARN 中 Client 与 Cluster 模式区别？

- **Client 模式**：Driver 跑在提交机上 → 适合 spark-shell / 调试
- **Cluster 模式**：Driver 也跑到 YARN 容器里 → 适合生产，提交机断网不影响

### Q9：什么是 Shuffle？为什么慢？

Shuffle 是 stage 之间数据重新分区的过程：
1. Map 端：每个 task 把数据按目标分区写到本地磁盘（**Sort-based Shuffle**，3.0 后默认）
2. Reduce 端：通过网络拉取所有 map 任务输出，然后做后续聚合

慢的原因：磁盘 IO + 网络 + 序列化反序列化。所以**减少 shuffle 数据量是 Spark 调优第一性原理**：能 broadcast 就不 shuffle，能 reduceByKey 就不 groupByKey，能开 AQE 就开 AQE。

### Q10：为什么生产 Spark 几乎都开 AQE？

AQE = Adaptive Query Execution（3.0+ 默认开）。它在运行时根据实际数据量做：
- **动态合并 shuffle 分区**：原本设了 200 个分区，实际数据少就合并到 50 个
- **动态切换 Join 策略**：原本是 SortMergeJoin，发现一边变小后切到 BroadcastJoin
- **动态优化倾斜 Join**：把 skew partition 拆成多个小 partition 并行跑

老的 Spark 2.x 调参靠经验，AQE 帮你"运行期再决定"，省心又快。

---

## 九、推荐学习路径

1. **第 1 周**：跑通 `RddWordCount` Web demo，看 Spark UI，理解 Job/Stage/Task
2. **第 2 周**：写 10 条递进的 Spark SQL（GROUP BY → 窗口 → 行转列），看物理计划
3. **第 3 周**：起 Kafka，跑 Structured Streaming demo；了解 watermark / outputMode
4. **第 4 周**：项目实战：把 `项目/案例四_实时排行榜与历史分析.md` 用 Spark 跑一遍离线分析

> 面试前**必看**：宽窄依赖、Catalyst、AQE、Shuffle 原理、Streaming Exactly-Once 怎么做的——这 5 个题点击率最高。
