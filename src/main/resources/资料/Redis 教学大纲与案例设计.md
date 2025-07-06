
# Redis 教学大纲与案例设计

---

## 一、Redis 基础入门

### 1.1 Redis 简介
- Redis 是什么、应用场景
- 内存数据库、持久化、单线程模型

### 1.2 安装与启动
- Windows/Linux/Mac 下安装
- 启动/停止 Redis
- redis-cli 基本使用

#### 案例
```bash
# 启动 Redis
redis-server

# 连接 Redis
redis-cli

# 基本命令
set name Alice
get name
```

**练习**
- 在本地安装并启动 Redis，尝试 set/get 一个 key。

---

## 二、Redis 数据类型与常用命令

### 2.1 String、List、Set、Hash、ZSet
- 各类型的存储结构与典型应用场景

### 2.2 常用命令
- String: set/get/incr/decr
- List: lpush/rpush/lpop/rpop/lrange
- Set: sadd/smembers/sismember
- Hash: hset/hget/hgetall
- ZSet: zadd/zrange/zscore

#### 案例
```bash
# String
set counter 10
incr counter
get counter

# List
lpush fruits apple
lpush fruits banana
lrange fruits 0 -1

# Set
sadd tags java redis stream
smembers tags

# Hash
hset user:1 name Alice age 20
hgetall user:1

# ZSet
zadd scores 100 Tom 90 Jerry
zrange scores 0 -1 withscores
```

**练习**
- 用 Redis CLI 操作每种数据类型，体验其行为。

---

### Redis 五大数据类型详解

---

#### 1. String（字符串）

##### 存储结构与原理
- Redis 的 String 是**二进制安全字符串**，最大可存储512MB。
- 底层实现为简单动态字符串（SDS），可存储文本、数字、序列化对象、图片等任意数据。

##### 典型应用场景
- 缓存单个对象（如用户Token、配置信息）
- 计数器、分布式唯一ID
- 简单消息、验证码等

##### 常用命令与案例

```bash
SET name Alice
GET name               # Alice

INCR counter           # 自增1
DECR counter           # 自减1

SET balance 100
INCRBY balance 50      # 增加50
GET balance            # 150
```

###### Java 操作示例（Jedis）
```java
jedis.set("name", "Alice");
String name = jedis.get("name");
jedis.incr("counter");
```

---

#### 2. List（列表）

##### 存储结构与原理
- Redis 的 List 是**双向链表**，支持从两端高效插入和删除。
- 元素有序，可重复。

##### 典型应用场景
- 消息队列（生产者 lpush，消费者 rpop）
- 任务列表、时间线（如微博）

##### 常用命令与案例

```bash
LPUSH fruits apple
RPUSH fruits banana
LPUSH fruits orange
LRANGE fruits 0 -1      # [orange, apple, banana]

LPOP fruits             # orange
RPOP fruits             # banana
```

###### Java 操作示例（Jedis）
```java
jedis.lpush("queue", "task1");
String task = jedis.rpop("queue");
```

---

#### 3. Set（集合）

##### 存储结构与原理
- Redis 的 Set 是**无序且唯一的元素集合**，底层为哈希表。
- 自动去重，查找/插入/删除效率高。

##### 典型应用场景
- 标签、兴趣、去重（如共同好友、抽奖池）
- 统计独立IP、在线用户

##### 常用命令与案例

```bash
SADD tags java redis stream
SMEMBERS tags            # [java, redis, stream]

SISMEMBER tags java      # 1（存在）
SREM tags stream         # 删除stream
```

###### Java 操作示例（Jedis）
```java
jedis.sadd("tags", "java", "redis");
Set<String> tags = jedis.smembers("tags");
```

---

#### 4. Hash（哈希）

##### 存储结构与原理
- Redis 的 Hash 是**键值对映射表**，适合存储对象。
- 底层为哈希表，支持高效单字段操作。

##### 典型应用场景
- 存储用户信息、商品信息（如 user:1）

##### 常用命令与案例

```bash
HSET user:1 name Alice age 20
HGET user:1 name          # Alice
HGETALL user:1            # {name=Alice, age=20}
HDEL user:1 age           # 删除字段
```

###### Java 操作示例（Jedis）
```java
jedis.hset("user:1", "name", "Alice");
jedis.hset("user:1", "age", "20");
Map<String, String> user = jedis.hgetAll("user:1");
```

---

#### 5. ZSet（有序集合）

##### 存储结构与原理
- Redis 的 ZSet 是**带分数的有序集合**，底层实现为跳表+哈希表。
- 每个元素有一个分数（score），自动按分数排序，支持范围查找。

##### 典型应用场景
- 排行榜（积分、活跃度、热度）
- 延时队列、带权重的任务调度

##### 常用命令与案例

```bash
ZADD scores 100 Tom 90 Jerry 80 Alice
ZRANGE scores 0 -1 WITHSCORES   # [Alice, Jerry, Tom]
ZREVRANGE scores 0 1 WITHSCORES # [Tom, Jerry]
ZSCORE scores Tom               # 100
ZINCRBY scores 10 Alice         # Alice分数+10
```

###### Java 操作示例（Jedis）
```java
jedis.zadd("ranking", 100, "Tom");
jedis.zadd("ranking", 90, "Jerry");
Set<String> top = jedis.zrevrange("ranking", 0, 2);
```

---

### 总结与对比

| 类型   | 结构/原理        | 典型场景                 | 是否有序 | 是否唯一 | 是否带分数 |
|--------|------------------|--------------------------|----------|----------|------------|
| String | 动态字符串       | 缓存、计数器、Token      | 否       | -        | -          |
| List   | 双向链表         | 队列、消息、时间线       | 是       | 可重复   | -          |
| Set    | 哈希表           | 标签、去重、共同好友     | 否       | 是       | -          |
| Hash   | 哈希表           | 用户/商品对象            | -        | 字段唯一 | -          |
| ZSet   | 跳表+哈希表      | 排行榜、延时队列         | 是       | 是       | 是         |

---

### 练习（建议CLI和Java各做一遍）

1. 用 String 实现一个访问计数器，每访问一次就自增。
2. 用 List 实现一个简单消息队列，生产者 lpush，消费者 rpop。
3. 用 Set 实现抽奖池，去重添加参与者，随机抽取一名。
4. 用 Hash 存储和查询用户 profile 信息。
5. 用 ZSet 实现积分排行榜，支持加分和查询前3名。

### 答案
好的，下面是每个练习的**CLI命令**和**Java（Jedis）代码**，你可以让学生分别在 redis-cli 和 Java 代码中实践。

---

#### 1. 用 String 实现一个访问计数器，每访问一次就自增

**CLI**
```bash
INCR visit:counter
GET visit:counter
```

**Java (Jedis)**
```java
Jedis jedis = new Jedis("localhost", 6379);
long count = jedis.incr("visit:counter");
System.out.println("当前访问次数：" + count);
jedis.close();
```

---

#### 2. 用 List 实现一个简单消息队列，生产者 lpush，消费者 rpop

**CLI**
```bash
LPUSH queue:msg "消息1"
LPUSH queue:msg "消息2"
RPOP queue:msg
```

**Java (Jedis)**
```java
Jedis jedis = new Jedis("localhost", 6379);
// 生产者
jedis.lpush("queue:msg", "消息1");
jedis.lpush("queue:msg", "消息2");
// 消费者
String msg = jedis.rpop("queue:msg");
System.out.println("消费消息：" + msg);
jedis.close();
```

---

#### 3. 用 Set 实现抽奖池，去重添加参与者，随机抽取一名

**CLI**
```bash
SADD lottery "Alice" "Bob" "Charlie"
SRANDMEMBER lottery
```

**Java (Jedis)**
```java
Jedis jedis = new Jedis("localhost", 6379);
jedis.sadd("lottery", "Alice", "Bob", "Charlie");
String winner = jedis.srandmember("lottery");
System.out.println("中奖者：" + winner);
jedis.close();
```

---

#### 4. 用 Hash 存储和查询用户 profile 信息

**CLI**
```bash
HSET user:1001 name "Alice" age "20"
HGET user:1001 name
HGETALL user:1001
```

**Java (Jedis)**
```java
Jedis jedis = new Jedis("localhost", 6379);
jedis.hset("user:1001", "name", "Alice");
jedis.hset("user:1001", "age", "20");
String name = jedis.hget("user:1001", "name");
Map<String, String> profile = jedis.hgetAll("user:1001");
System.out.println("姓名：" + name);
System.out.println("完整信息：" + profile);
jedis.close();
```

---

#### 5. 用 ZSet 实现积分排行榜，支持加分和查询前3名

**CLI**
```bash
ZADD rank 100 Tom 80 Alice 90 Bob
ZINCRBY rank 10 Alice
ZREVRANGE rank 0 2 WITHSCORES
```

**Java (Jedis)**
```java
Jedis jedis = new Jedis("localhost", 6379);
jedis.zadd("rank", 100, "Tom");
jedis.zadd("rank", 80, "Alice");
jedis.zadd("rank", 90, "Bob");
// Alice加10分
jedis.zincrby("rank", 10, "Alice");
// 查询前三名
Set<String> top3 = jedis.zrevrange("rank", 0, 2);
System.out.println("排行榜前3名：" + top3);
jedis.close();
```

---

## 三、Redis 进阶特性

### 3.1 过期与持久化
- key 过期（expire/ttl）
- RDB/AOF 持久化机制

#### 过期（expire/ttl）

##### 原理
- Redis 可以为每个 key 设置过期时间，到期后自动删除。
- 常用命令：`EXPIRE key seconds`，`TTL key`。

##### CLI 案例
```bash
SET temp "will expire"
EXPIRE temp 10
TTL temp          # 查看剩余秒数
```

##### Java (Jedis) 案例
```java
Jedis jedis = new Jedis("localhost", 6379);
jedis.set("temp", "will expire");
jedis.expire("temp", 10);
System.out.println("TTL: " + jedis.ttl("temp"));
jedis.close();
```

##### 练习
- 设置一个 key 10 秒后自动删除，并用 `TTL` 验证它的剩余时间和到期后自动消失。

---

#### 持久化机制（RDB/AOF）

##### 原理
- RDB：定期快照保存内存数据到磁盘，适合灾难恢复。
- AOF：每次写命令都追加到日志文件，支持更高的数据可靠性。

##### CLI 案例
```bash
# 手动触发RDB快照
SAVE
# 查看AOF配置
CONFIG GET appendonly
```

##### 练习
- 修改 redis.conf，体验 RDB 和 AOF 持久化的不同。
- 停止 Redis，删除内存数据，重启后观察数据恢复情况。

---
### 3.2 发布/订阅
- pub/sub 机制
- 典型应用：消息通知、实时推送

##### 原理
- Redis 支持消息发布/订阅，适合通知、推送、实时消息等场景。
- 发布者 `PUBLISH`，订阅者 `SUBSCRIBE`。

##### CLI 案例
```bash
# 终端1
SUBSCRIBE news

# 终端2
PUBLISH news "hello redis"
```

##### Java (Jedis) 案例
```java
// 订阅端
Jedis jedis = new Jedis("localhost", 6379);
jedis.subscribe(new JedisPubSub() {
    @Override
    public void onMessage(String channel, String message) {
        System.out.println("收到消息: " + message);
    }
}, "news");

// 发布端
Jedis jedisPub = new Jedis("localhost", 6379);
jedisPub.publish("news", "hello redis");
```

##### 练习
- 用两个 redis-cli，一个订阅 news，一个发布 news，实现简单的消息广播。

---

### 3.3 事务与 Lua 脚本
- multi/exec/discard/watch
- eval 执行 Lua 脚本

#### 事务（multi/exec/discard/watch）

##### 原理
- Redis 事务通过 `MULTI` 开启，`EXEC` 执行，`DISCARD` 放弃，`WATCH` 监控乐观锁。

##### CLI 案例
```bash
MULTI
INCR counter
INCR counter
EXEC
```

##### Java (Jedis) 案例
```java
Jedis jedis = new Jedis("localhost", 6379);
Transaction tx = jedis.multi();
tx.incr("counter");
tx.incr("counter");
tx.exec();
jedis.close();
```

---

#### Lua 脚本（EVAL）

##### 原理
- 用 `EVAL` 命令执行原子性复杂逻辑，适合扣库存、分布式锁等场景。

##### CLI 案例
```bash
EVAL "return redis.call('incr', KEYS[1])" 1 counter
```

##### Java (Jedis) 案例
```java
Jedis jedis = new Jedis("localhost", 6379);
Object result = jedis.eval("return redis.call('incr', KEYS[1])", 1, "counter");
System.out.println(result);
jedis.close();
```

#### 案例
```bash
# 设置过期
set temp "will expire"
expire temp 10

# 发布订阅
subscribe news
publish news "hello redis"

# 事务
multi
incr counter
incr counter
exec
```

**练习**
- 设置一个 key 10 秒后自动删除，并验证。
- 使用 pub/sub 实现一个简单的消息广播。

---

### 练习总结

1. **设置一个 key 10 秒后自动删除，并验证。**
   - CLI：`SET temp "value"` + `EXPIRE temp 10` + `TTL temp` + 10秒后 `GET temp`
   - Java：见上面 Jedis 代码

2. **用 pub/sub 实现一个简单的消息广播。**
   - CLI：一个窗口 `SUBSCRIBE news`，另一个窗口 `PUBLISH news "hello redis"`
   - Java：见上面 Jedis 代码

---

## 四、Java 操作 Redis（Jedis/lettuce/Spring Data Redis）

### 4.1 Jedis 基本用法
- 连接 Redis
- String、List、Hash 操作

### 4.1 Jedis 基本用法

#### 1. 连接 Redis
- Jedis 是 Redis 官方推荐的 Java 客户端之一，操作简单，适合入门和小型项目。

#### 2. String、List、Hash 操作案例

```java
import redis.clients.jedis.Jedis;

public class JedisDemo {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost", 6379);

        // String
        jedis.set("name", "Alice");
        String value = jedis.get("name");
        System.out.println("name: " + value);

        // List
        jedis.lpush("queue", "task1");
        jedis.lpush("queue", "task2");
        String task = jedis.rpop("queue");
        System.out.println("task: " + task);

        // Hash
        jedis.hset("user:1", "name", "Bob");
        jedis.hset("user:1", "age", "22");
        System.out.println("user:1 name: " + jedis.hget("user:1", "name"));
        System.out.println("user:1 info: " + jedis.hgetAll("user:1"));

        jedis.close();
    }
}
```


**原理说明**：
- Jedis 通过 TCP 连接 Redis，所有命令均为同步阻塞。
- String、List、Hash 操作与 CLI 命令一一对应。

---

### 4.2 Spring Data Redis 集成
- 配置 application.properties
- 使用 RedisTemplate 操作各种类型

#### 1. 配置 application.properties

```properties
spring.redis.host=localhost
spring.redis.port=6379
```

#### 2. 使用 RedisTemplate 操作各种类型

```java
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
    @Autowired
    private StringRedisTemplate redisTemplate;

    public void demo() {
        // String
        redisTemplate.opsForValue().set("hello", "world");
        String v = redisTemplate.opsForValue().get("hello");
        System.out.println("hello: " + v);

        // List
        redisTemplate.opsForList().leftPush("queue", "task1");
        redisTemplate.opsForList().leftPush("queue", "task2");
        String task = redisTemplate.opsForList().rightPop("queue");
        System.out.println("task: " + task);

        // Hash
        redisTemplate.opsForHash().put("user:1", "name", "Bob");
        Object name = redisTemplate.opsForHash().get("user:1", "name");
        System.out.println("user:1 name: " + name);
    }
}
```
**原理说明**：
- Spring Data Redis 自动管理连接池，支持多种数据类型的高层API。
- 推荐用 `StringRedisTemplate` 操作字符串，`RedisTemplate` 操作对象。

---

### 练习题

#### 练习1：用 Java/Jedis 实现一个简单的计数器

**需求**：每调用一次方法，计数器加1，并输出当前次数。

```java
public class JedisCounterDemo {
    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost", 6379);
        long count = jedis.incr("visit:counter");
        System.out.println("当前访问次数：" + count);
        jedis.close();
    }
}
```

---

#### 练习2：用 Spring Boot 集成 Redis，实现用户登录次数统计

**需求**：每次用户登录时，统计用户的登录总次数。

**Controller 示例**：
```java
@RestController
public class LoginController {
    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/login")
    public String login(@RequestParam String username) {
        Long count = redisTemplate.opsForValue().increment("login:" + username);
        return username + " 登录次数：" + count;
    }
}
```
**测试**：多次调用 `/login?username=alice`，返回次数递增。

---

## 五、Redis 应用场景与最佳实践

---

### 5.1 常见应用场景

#### 1. 缓存
- **原理**：将热点数据（如用户信息、商品详情）存储在 Redis，减少数据库压力。
- **代码示例**：
    ```java
    // 查询缓存
    String value = jedis.get("user:1001");
    if (value == null) {
        // 查询数据库并写入缓存
        value = db.queryUser("1001");
        jedis.set("user:1001", value, "NX", "EX", 3600);
    }
    ```

#### 2. 分布式锁
- **原理**：利用 Redis 的 setnx（SET key value NX EX）实现互斥锁。
- **代码示例**：
    ```java
    String result = jedis.set("lock:order", "1", "NX", "EX", 10);
    if ("OK".equals(result)) {
        // 获得锁
        // ...业务逻辑...
        jedis.del("lock:order");
    }
    ```

#### 3. 排行榜（ZSet）
- **原理**：用 ZSet 的分数维护积分、热度等，自动排序，支持TopN查询。
- **代码示例**：
    ```java
    jedis.zincrby("score:rank", 10, "Alice");
    Set<String> top3 = jedis.zrevrange("score:rank", 0, 2);
    ```

#### 4. 限流与防刷
- **原理**：用计数器、过期时间等限制单位时间内的访问次数。
- **代码示例**：
    ```java
    String key = "req:ip:" + ip;
    long count = jedis.incr(key);
    if (count == 1) jedis.expire(key, 60);
    if (count > 5) {
        // 拦截
    }
    ```

---

### 5.2 缓存穿透/击穿/雪崩问题

#### 1. 缓存穿透
- **问题**：请求的 key 无论缓存还是数据库都没有，导致每次都查数据库。
- **解决**：对空结果也缓存（如存 "null"），或用布隆过滤器。

#### 2. 缓存击穿
- **问题**：某个热点 key 突然过期，大量请求同时落到数据库。
- **解决**：互斥锁、提前预热、随机过期时间。

#### 3. 缓存雪崩
- **问题**：大量 key 同时过期，导致数据库压力激增。
- **解决**：过期时间加随机、分批失效、降级限流。

---

### 5.3 Redis 性能优化与集群

- **主从复制**：读写分离，提高可用性。
- **哨兵模式**：自动故障切换。
- **分片集群**：水平扩展，提升容量和吞吐。
- **慢查询日志**：定位慢命令。
- **内存淘汰策略**：合理设置 maxmemory-policy。
- **持久化优化**：合理选择 RDB/AOF，避免频繁大快照。

---

## 案例

### 1. 用 Redis 实现分布式锁

```java
String lockKey = "lock:resource";
String lockValue = UUID.randomUUID().toString();
String result = jedis.set(lockKey, lockValue, "NX", "EX", 10);
if ("OK".equals(result)) {
    try {
        // 获得锁，执行业务
    } finally {
        // 释放锁时要确保是自己的锁
        if (lockValue.equals(jedis.get(lockKey))) {
            jedis.del(lockKey);
        }
    }
}
```

---

### 2. 用 ZSet 实现排行榜

```java
// 加分
jedis.zincrby("game:rank", 50, "Tom");
// 查询前N名
Set<String> topN = jedis.zrevrange("game:rank", 0, 2);
// 查询分数
Double score = jedis.zscore("game:rank", "Tom");
```

---

## 练习

### 1. 用 Redis 实现用户访问次数的限流，每分钟最多访问5次

**思路**：用 key 记录用户/接口的访问次数，每次访问自增，首次访问设置60秒过期，超过5次则拒绝。

**CLI 示例**
```bash
INCR req:user:1001
EXPIRE req:user:1001 60
```

**Java 示例（Jedis）**
```java
String key = "req:user:" + userId;
long count = jedis.incr(key);
if (count == 1) jedis.expire(key, 60);
if (count > 5) {
    System.out.println("访问过于频繁！");
} else {
    System.out.println("允许访问");
}
```

---

### 2. 设计一个简单的排行榜功能，支持加分和查询前N名

**CLI 示例**
```bash
ZINCRBY rank 10 Alice
ZINCRBY rank 20 Bob
ZREVRANGE rank 0 2 WITHSCORES
```

**Java 示例（Jedis）**
```java
jedis.zincrby("rank", 10, "Alice");
jedis.zincrby("rank", 20, "Bob");
Set<String> top3 = jedis.zrevrange("rank", 0, 2);
System.out.println("Top3: " + top3);
```


---

# 练习题（覆盖基础与进阶）

1. 用 Redis CLI 实现一个简单的消息队列，生产者 lpush，消费者 rpop。
2. 用 Java 实现一个点赞计数器，支持点赞和查询总数。
3. 用 Spring Data Redis 实现一个用户 session 缓存。
4. 用 Redis 的 Hash 存储用户信息，实现增查改。
5. 用 ZSet 实现一个积分排行榜，并查询Top3。
6. 用 Lua 脚本实现原子性扣减库存。
7. 用 Redis 发布/订阅实现聊天室的消息广播。
8. 用 Redis 实现接口防刷（限流），每个IP每分钟至多访问10次。
