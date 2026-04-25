

# HBase 教学大纲与案例设计

---

## 一、HBase 基础入门

### 1.1 HBase 简介
- 什么是 HBase？与关系型数据库、Redis、MongoDB的区别
- 典型应用场景（大数据存储、时序数据、日志、物联网等）

#### 什么是 HBase？

- **HBase** 是一个开源的、分布式的、面向列的 NoSQL 数据库，运行在 Hadoop HDFS 之上。
- 它模仿了 Google 的 Bigtable 设计，能够**存储和管理超大规模稀疏表**（数十亿行、数百万列）。
- HBase 提供了高吞吐、可扩展的随机读写能力，适合海量数据的实时检索和分析。

#### HBase 与其他数据库的区别

| 特点              | HBase                          | 关系型数据库（MySQL等） | Redis                | MongoDB             |
|-------------------|-------------------------------|------------------------|----------------------|---------------------|
| **数据模型**      | 面向列的稀疏表                 | 面向行的结构化表        | 键值、五大数据类型    | 文档（JSON/BSON）   |
| **事务支持**      | 行级原子操作，无多行事务        | 支持ACID事务            | 单命令原子           | 单文档事务          |
| **扩展性**        | 水平扩展，适合大数据            | 垂直扩展为主            | 水平扩展（集群）     | 水平扩展            |
| **存储介质**      | HDFS（磁盘）                    | 磁盘                    | 内存为主，支持持久化 | 磁盘                |
| **适用场景**      | 大数据、时序、日志、物联网      | OLTP、关系型数据         | 缓存、计数器、排行榜 | 半结构化文档数据    |
| **查询能力**      | 支持主键、范围、过滤器查询      | SQL丰富查询             | 键值/集合操作        | 富查询、聚合        |
| **一致性模型**    | 强一致性（单行原子操作）        | 强一致性                | 强一致性             | 最终一致性          |

##### 总结
- HBase 适合**超大规模、宽表、稀疏数据**，尤其是需要高并发写入和实时查询的场景。
- 不适合复杂事务和 SQL 聚合查询，通常与 Hadoop/Spark 等大数据生态集成。

---

#### 典型应用场景

1. **大数据存储**  
   - 互联网公司用户行为日志、广告点击流、搜索引擎索引等。
2. **时序数据**  
   - 监控系统、IoT设备数据、金融市场行情历史等。
3. **物联网（IoT）**  
   - 设备传感器数据、高频采集、按设备ID+时间戳组织。
4. **消息与日志系统**  
   - 大规模日志归档、消息追踪、风控审计。
5. **用户画像/标签系统**  
   - 用户多维度标签宽表存储、实时查询。

---

### 1.2 HBase 体系结构
- HMaster、RegionServer、Zookeeper
- 表、行键（RowKey）、列族（ColumnFamily）、列限定符（Qualifier）、版本（Timestamp）、单元（Cell）

#### 核心组件

1. **HMaster**
   - HBase 的主节点，负责表的管理、Region 分配、负载均衡、元数据维护。
   - 类似于 NameNode 在 HDFS 中的角色。

2. **RegionServer**
   - HBase 的工作节点，实际存储和管理数据。
   - 每个 RegionServer 管理若干个 Region（表的分区）。
   - 负责处理客户端的读写请求。

3. **ZooKeeper**
   - HBase 的协作与故障检测组件。
   - 管理集群元数据、RegionServer 状态、Master 选举等。

##### 体系结构图（文字版）

```
+-----------------+
|     Client      |
+--------+--------+
         |
         v
+--------+--------+        +-------------------+
|     ZooKeeper   |<------>|      HMaster      |
+--------+--------+        +-------------------+
         |
         v
+--------+--------+        +-------------------+
|   RegionServer  |<------>|   RegionServer    |
+-----------------+        +-------------------+
```

---

#### 数据模型（宽表模型）

- **表（Table）**：逻辑上的数据集合，类似关系型数据库的表。
- **行键（RowKey）**：唯一标识一行数据，**字典序排序**，支持范围扫描。行键设计对性能影响极大。
- **列族（ColumnFamily）**：表的物理分区，存储在一起。每个表至少有一个列族。
- **列限定符（Qualifier）**：列族下的具体列名，可以动态扩展。
- **版本（Timestamp）**：每个单元格（Cell）可以存储多个版本，默认用写入时间戳区分。
- **单元格（Cell）**：表的最小存储单元，由（RowKey, ColumnFamily, Qualifier, Timestamp）唯一标识。

##### 例子

假设有如下 HBase 表结构：

| RowKey | info:name | info:age | score:math | score:english |
|--------|-----------|----------|------------|---------------|
| 1001   | Alice     | 20       | 95         | 88            |
| 1002   | Bob       | 21       | 80         | 90            |

- `info`、`score` 是列族
- `name`、`age`、`math`、`english` 是列限定符
- 每个 Cell 可以有多个版本（时间戳）

#### 小结

- HBase 适合大数据/宽表/高并发写入/实时查询场景。
- 体系结构核心：HMaster、RegionServer、ZooKeeper。
- 数据模型：表-行键-列族-列限定符-版本-单元格，灵活且高效。

---
### 1.3 HBase 安装与启动

本节给出**两条可走通**的本地路径，按学生机器条件二选一。**强烈推荐 Docker 方式**——
单条命令拉起，秒级清空重来，避开 Hadoop 配置坑。

#### 方式 A（推荐）：Docker 单机版

仓库已经准备好 `docker/hbase/docker-compose.yml`，启动只需：

```bash
docker compose -f docker/hbase/docker-compose.yml up -d
```

> Apple Silicon (M1/M2/M3) 自动通过 Rosetta 跑 amd64 镜像，无需手动配置。

启动后访问 <http://localhost:16010> 看到 HMaster Web UI 即成功。
**关键一步**：让本机能把 `hbase-docker` 解析到 127.0.0.1。两个方法二选一：

```bash
# 方法 A（推荐，无需 sudo）：用 JDK 私有 hosts 文件
# 仓库 pom.xml 已经在 surefire 里注入：-Djdk.net.hosts.file=docker/hbase/hosts.local
# IDEA 单跑测试时把这条加到 VM Options 即可。

# 方法 B：改系统 hosts
echo "127.0.0.1 hbase-docker" | sudo tee -a /etc/hosts
```

进入 shell：

```bash
docker exec -it hbase-standalone hbase shell
```

> 一键灌入演示数据：`docker exec -i hbase-standalone hbase shell < docker/hbase/init-tables.hbase`
> 详细说明见 `docker/hbase/README.md`。

#### 方式 B：本地伪分布式

需要先装好 JDK 8/11、Hadoop（伪分布式）和 ZooKeeper，再下载 HBase tar 包，
配置 `hbase-site.xml` 指向本地 HDFS 与 ZK，用 `start-hbase.sh` 启动。
对教学场景而言负担过重，**仅在没有 Docker 环境时使用**。

```bash
# 启动 / 停止
start-hbase.sh
stop-hbase.sh

# 进入 Shell
hbase shell
list
```

#### 必懂：Java 客户端为什么需要 hosts 映射？

ZooKeeper 里登记的 RegionServer 地址是 RegionServer 进程**自身看到的 hostname**
（容器里就是容器的 hostname）。客户端拿到这个名字后必须能解析到一个可达的 IP，
所以本机要把它指回 `127.0.0.1`。这是 HBase + Docker 教学最高频的"卡死"点，
务必给学生强调一遍。

**练习**
- 用 Docker 启动 HBase，访问 16010 Web UI。
- 进入 shell 执行 `list`、`status`，观察集群状态。
- 故意不加 hosts 映射，跑一次 Java 客户端，复现并理解错误信息。

---

## 二、HBase 数据模型与基本操作

### 2.1 数据模型原理
- 行键唯一性与设计原则
- 列族与列（宽表结构）
- 多版本数据存储

### 2.2 表操作与基本命令
- 创建表、删除表、列族管理

#### 案例（Shell）
```bash
# 创建表
create 'user', 'info', 'score'

# 插入数据
put 'user', '1001', 'info:name', 'Alice'
put 'user', '1001', 'info:age', '20'
put 'user', '1001', 'score:math', '95'

# 查询数据
get 'user', '1001'
scan 'user'
```

**练习**
- 创建一个学生表，包含info和score两个列族，并插入一条学生数据。

---

## 三、HBase 进阶操作与原理

### 3.1 多版本与时间戳
- 原理：Cell 支持多版本，默认版本号为写入时间戳
- 应用场景：数据回溯、审计

#### 案例
```bash
put 'user', '1001', 'info:name', 'Alice', 1680000000000
put 'user', '1001', 'info:name', 'Alice2', 1690000000000
get 'user', '1001', {COLUMN=>'info:name', VERSIONS=>2}
```

**练习**
- 对同一行同一列插入不同版本数据，并查询所有版本。

### 3.2 过滤器与条件查询
- RowFilter、ValueFilter、PrefixFilter等
- Scan 查询原理

#### 案例
```bash
scan 'user', {FILTER=>"PrefixFilter('100')"}
scan 'user', {FILTER=>"SingleColumnValueFilter('info','age',=,'binary:20')"}
```

**练习**
- 用PrefixFilter查找所有学号以“100”开头的学生。

---

## 四、HBase Java API 操作

### 4.1 Java 连接与基本操作
- HBase Java 客户端配置
- 表的增删查改

#### 最小代码（连接到本地 Docker HBase）

```java
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;

Configuration conf = HBaseConfiguration.create();
conf.set("hbase.zookeeper.quorum", "localhost");        // ← Docker 直连
conf.set("hbase.zookeeper.property.clientPort", "2181");

try (Connection connection = ConnectionFactory.createConnection(conf);
     Table table = connection.getTable(TableName.valueOf("user"))) {

    // 插入
    Put put = new Put("1001".getBytes());
    put.addColumn("info".getBytes(), "name".getBytes(), "Alice".getBytes());
    table.put(put);

    // 查询
    Result result = table.get(new Get("1001".getBytes()));
    byte[] value = result.getValue("info".getBytes(), "name".getBytes());
    System.out.println(new String(value));
}
```

> ⚠️ 在跑这段代码前请确认：(1) 已 `docker compose up`；(2) `/etc/hosts` 已加
> `127.0.0.1 hbase-docker`。否则 `Connection` 会卡死在解析 RegionServer 地址。

#### 仓库内的端到端示例

| 文件 | 用途 |
|------|------|
| `src/test/java/com/example/demo/HbasePractice.java` | JUnit 测试：插入、批量插入、Get |
| `src/main/java/com/example/demo/hbase/HBaseUtil.java` | 工具类：单例 Connection，按系统属性/环境变量切换 ZK 地址 |
| `src/main/java/com/example/demo/hbase/StudentController.java` | Spring Boot Controller：列表/详情/添加/前缀查询/多版本 |
| `src/main/resources/templates/hbase/*.html` | Thymeleaf 页面 |

启动 Spring Boot 后访问 <http://localhost:8090/hbase/student/list> 即可联动 HBase。

**练习**
- 用 Java 代码实现：插入一条数据、查询一条数据（在 `HbasePractice` 里加一个新方法）。
- 在 Spring Boot 页面新增"按年龄删除学生"功能，理解 `Delete` API。

---

## 五、HBase 高级特性与最佳实践

### 5.1 RowKey 设计与热点问题

**核心原则**：HBase 按 RowKey **字典序**全局排序，连续相邻的 RowKey 会落到同一
Region。设计不当会导致**热点 Region**（写入压在某一台 RegionServer 上）和
**扫描效率低下**（要查的数据散在全集群）。

#### 反模式 vs 正确做法（必看对比）

| 场景             | 反模式（热点）        | 正确做法                                    |
|------------------|------------------------|---------------------------------------------|
| 用自增 ID        | `1, 2, 3, 4...`        | 反转：`...4, 3, 2, 1`，或前置 hash 前缀     |
| 用时间戳         | `20260425103000`       | `Long.MAX - ts`（倒序）或 `bucket_ts`       |
| 用手机号         | `13800000001`（前缀同）| `MD5(phone)[0..3] + phone`，散列前缀打散    |
| 多维查询         | 单列查 → 全表 scan     | RowKey 拼接：`userId_yyyyMMdd_orderId`      |

#### 三种常见打散方法

1. **加盐（Salting）**：前置一个 hash 前缀（`hashCode(rowkey) % N`），N 个桶。
   缺点：丢失原始有序性，不能直接前缀范围扫。
2. **反转（Reverse）**：把单调字段倒过来，如手机号 `13800000001 → 10000000831`。
   保留原始信息，又打散了写入热点。
3. **预分区（Pre-splitting）**：建表时指定 split keys，避免初期所有写入都
   挤在一个默认 Region 里。

```bash
# 建表时按 16 个桶预分区
create 'log', {NAME => 'cf'}, SPLITS => ['1','2','3','4','5','6','7','8','9','a','b','c','d','e','f']
```

### 5.2 批量操作与 Scan 优化
- 批量 `table.put(List<Put>)` 比循环单 put 快 10-100 倍
- `Scan.setCaching(int)` 调大扫描缓存（默认 1，建议 100~1000）
- `Scan.setBatch(int)` 控制每次 RPC 返回的列数，列族很宽时尤其重要
- 只取需要的列：`scan.addColumn(cf, col)`，避免全列族扫描
- 过滤器链 `FilterList` 组合 `RowFilter + SingleColumnValueFilter`

### 5.3 HBase 性能优化与集群
- Region 分裂与合并（Compaction：minor/major）
- BloomFilter（按 RowKey 还是 Row+Col 决定 false positive 率）
- 压缩算法：SNAPPY 通用首选，ZSTD 压缩比更高
- TTL：自动过期，适合日志/会话类数据
- HBase 与 Hadoop/Spark 集成：通过 `TableInputFormat` 做批处理

#### 案例
- 用 Java 批量插入 1000 条数据并扫描输出（已在 `HbasePractice#prac2` 中演示）

**练习**
- 给定订单表，写出三种 RowKey 方案：纯订单号、`userId_orderId`、
  `MD5(userId)[0..3]_userId_orderId`，分别说明擅长 / 不擅长哪种查询。
- 用 Java 批量插入 1000 条并用 `setCaching(500)` 扫描，对比有无 caching 的耗时。

---

# 综合练习题

1. 用HBase Shell创建一个订单表（order），包含buyer和item两个列族，插入三条订单数据，并用scan命令查询。
2. 用Java实现：批量插入100条商品数据到商品表（product），并查询某个商品详情。
3. 用Shell或Java实现：查询某个用户的所有分数信息（score列族）。
4. 用Java实现：对同一用户的同一列写入多个版本，并查询所有版本。
5. 结合HBase过滤器，实现按条件（如分数大于80）查询学生。

