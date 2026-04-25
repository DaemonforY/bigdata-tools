# 本地 Docker HBase（教学用）

单机 standalone 模式：HMaster + RegionServer + ZooKeeper 跑在同一个容器里，秒级启动，
重启即清空（卷可保留），适合课堂演示和 Java 客户端联调。

> 镜像：`harisekhon/hbase:latest`（HBase 2.1.3）
> 仅有 amd64 镜像；Apple Silicon 通过 `platform: linux/amd64` 走 Rosetta，可用。

---

## 一、启动

```bash
# 在仓库根目录执行
docker compose -f docker/hbase/docker-compose.yml up -d

# 查看启动状态（约 15-30 秒后 master 就绪）
docker logs -f hbase-standalone
```

启动后开放端口：

| 端口  | 用途                           |
|-------|--------------------------------|
| 2181  | ZooKeeper（Java 客户端入口）   |
| 16000 | HMaster RPC                    |
| 16010 | HMaster Web UI（浏览器）       |
| 16020 | RegionServer RPC               |
| 16030 | RegionServer Web UI            |

打开 <http://localhost:16010> 应能看到 HBase Web UI。

---

## 二、关键一步：解析 `hbase-docker`（**必做**）

HBase 客户端连 ZooKeeper 时，ZK 返回的是 RegionServer **容器内的 hostname**
（这里是 `hbase-docker`），宿主机默认无法解析它，于是出现 "无法连接到
hbase-docker:16020" 之类的错误。两个等价方案任选其一：

### 方案 A（推荐，无需 sudo）：JDK 私有 hosts 文件

仓库已经准备了 `docker/hbase/hosts.local`，并在 `pom.xml` 的 surefire 中通过
`-Djdk.net.hosts.file=...` 注入。**Maven 跑测试无需任何额外配置**。

如果是其他启动方式（比如 IDEA 直接 Run），把下面这行加到 VM Options：

```
-Djdk.net.hosts.file=docker/hbase/hosts.local
```

### 方案 B：改系统 `/etc/hosts`

```bash
# macOS / Linux
echo "127.0.0.1 hbase-docker" | sudo tee -a /etc/hosts
```

Windows 编辑 `C:\Windows\System32\drivers\etc\hosts`，加入同样一行。

> 这是 HBase 在 Docker 里的经典坑。理解后记得在课件里给学生也补一遍。

---

## 三、HBase Shell（容器内）

```bash
docker exec -it hbase-standalone hbase shell
```

随后可执行：

```bash
list                                # 查看所有表
create 'student','info','score'     # 创建表
put 'student','1001','info:name','Alice'
get 'student','1001'
scan 'student'
disable 'student'; drop 'student'
```

为方便课堂演示，已经准备了一键创建几张演示表的脚本：

```bash
docker exec -i hbase-standalone hbase shell < docker/hbase/init-tables.hbase
```

---

## 四、Java 客户端连接

`HBaseUtil` 默认连 `localhost:2181`，配合上面的 hosts 映射即可工作。
也可以临时切换：

```bash
# JVM 系统属性
mvn test -Dhbase.zookeeper.quorum=localhost -Dtest=HbasePractice

# 或环境变量
HBASE_ZOOKEEPER_QUORUM=localhost mvn spring-boot:run
```

---

## 五、停止 / 清理

```bash
# 停止（保留数据卷）
docker compose -f docker/hbase/docker-compose.yml down

# 连数据一起清掉
docker compose -f docker/hbase/docker-compose.yml down -v
```

---

## 六、常见排错

| 现象 | 原因 | 解决 |
|------|------|------|
| `RegionServerStoppedException` / 连接 hbase-docker 超时 | 没加 hosts 映射 | 见第二节 |
| 容器启动后立刻退出 | 端口 16010/2181 被占用 | `lsof -i :16010` 杀掉占用进程，或改 compose 端口映射 |
| Mac 启动很慢 | Rosetta 模拟 amd64 | 正常现象，首启 30~60s |
| `org.apache.hadoop.hbase.ipc.RpcClient ... ClassNotFoundException` | hadoop-common 与 hbase-client 版本冲突 | 保持 pom 中 hbase-client 2.4.x + hadoop-common 3.3.x |
