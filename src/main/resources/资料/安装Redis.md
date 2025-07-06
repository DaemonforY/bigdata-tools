
Mac 启动Redis和关闭Redis：
在 macOS 上安装、启动和关闭 Redis 的方式有几种，推荐使用 **Homebrew** 安装。以下是完整流程和命令：

---

## 🍺 1. 安装 Redis（使用 Homebrew）

```bash
brew update
brew install redis
```

安装完成后，Redis 的命令行工具和服务将被安装到 `/opt/homebrew/bin/redis-server` 等路径下（M1/M2 架构）或 `/usr/local/bin/`（Intel）。

---

## 🚀 2. 启动 Redis

### ✅ 方式一：手动启动 Redis 服务器

```bash
redis-server
```

> 默认配置，会一直挂在前台运行。

### ✅ 方式二：后台启动（推荐）

```bash
brew services start redis
```

> Redis 会作为 macOS 后台服务启动（随开机自动运行）。

---

## 🛑 3. 停止 Redis

### 🔻 停止前台运行的 Redis：

如果你是用 `redis-server` 命令手动启动的，可以在终端按下 `Ctrl + C` 结束服务。

### 🔻 停止后台 Redis 服务（brew）

```bash
brew services stop redis
```

---

## 🧪 4. 测试 Redis 是否正常运行

打开新终端窗口，输入：

```bash
redis-cli
```

进入后你可以输入：

```bash
ping
```

如果输出：

```text
PONG
```

说明 Redis 已成功启动并正在运行。

---

## 📌 补充：常用命令小抄

| 操作       | 命令                          |
| -------- | --------------------------- |
| 安装 Redis | `brew install redis`        |
| 启动（后台）   | `brew services start redis` |
| 停止（后台）   | `brew services stop redis`  |
| 手动启动     | `redis-server`              |
| 连接客户端    | `redis-cli`                 |
| 测试连接     | `redis-cli ping`            |

---

需要我帮你配置 Redis 的配置文件（如端口、持久化等）吗？
