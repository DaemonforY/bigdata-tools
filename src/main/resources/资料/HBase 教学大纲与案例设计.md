

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
- 本地伪分布式模式
- 启动/停止 HBase 服务
- HBase Shell 基本命令

#### 案例
```bash
# 启动HBase
start-hbase.sh

# 进入Shell
hbase shell

# 查看所有表
list
```

**练习**
- 启动本地HBase，进入shell，查看表列表。

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

#### 案例
```java
// 依赖：hbase-client
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.conf.Configuration;

Configuration conf = HBaseConfiguration.create();
conf.set("hbase.zookeeper.quorum", "localhost");
try (Connection connection = ConnectionFactory.createConnection(conf)) {
    Table table = connection.getTable(TableName.valueOf("user"));
    // 插入数据
    Put put = new Put("1001".getBytes());
    put.addColumn("info".getBytes(), "name".getBytes(), "Alice".getBytes());
    table.put(put);

    // 查询数据
    Get get = new Get("1001".getBytes());
    Result result = table.get(get);
    byte[] value = result.getValue("info".getBytes(), "name".getBytes());
    System.out.println(new String(value));
}
```

**练习**
- 用Java代码实现：创建表、插入一条数据、查询一条数据。

---

## 五、HBase 高级特性与最佳实践

### 5.1 RowKey 设计与热点问题
- 行键散列、前缀反转、盐值分区
- 防止热点和数据倾斜

### 5.2 批量操作与Scan优化
- 批量Put、批量Get、Scan分页
- 过滤器链、只查必要列族

### 5.3 HBase 性能优化与集群
- Region 分裂与合并
- BloomFilter、压缩、TTL、预分区
- HBase 与 Hadoop 集成

#### 案例
- 用Java批量插入1000条数据并扫描输出

**练习**
- 设计一个合理的RowKey方案，实现按学号前缀高效查询学生信息。
- 用Java批量插入并Scan查询所有数据。

---

# 综合练习题

1. 用HBase Shell创建一个订单表（order），包含buyer和item两个列族，插入三条订单数据，并用scan命令查询。
2. 用Java实现：批量插入100条商品数据到商品表（product），并查询某个商品详情。
3. 用Shell或Java实现：查询某个用户的所有分数信息（score列族）。
4. 用Java实现：对同一用户的同一列写入多个版本，并查询所有版本。
5. 结合HBase过滤器，实现按条件（如分数大于80）查询学生。

