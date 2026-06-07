# 设计 Spec · Spark L5–L6 教学 Deck（三套逻辑的唯一共同输入）

## 这是什么
一份贯通「第5课 Spark SQL 实战」+「第6课 性能调优」的**讲师投屏 deck**。
- **用途**：老师上课投屏讲解（不是学生自学——自学版是已有的两个交互 HTML）。
- **观众距离**：10 米外投影，字必须够大。
- **完整 deck 约 16-22 页**，但本轮三套逻辑**各只做 2 页 showcase**（封面 + 一个核心知识点页），定 grammar 后再批量推。

## 输出格式与尺寸（三套逻辑必须统一）
- **每页独立 HTML，画布锁定 1920 × 1080px**。
- 多文件架构：`slides/NN-name.html` + `deck_index.html` 拼接（本轮先不做拼接器，只做单页）。
- 字体走 Google Fonts `<link>`，每页自带。
- 截图用 `npx playwright screenshot file://绝对路径 out.png --viewport-size=1920,1080`。

## 本轮 showcase 必做的 2 页（每套逻辑都做这两页，同内容只换设计）
### 页 A · 封面
- 课程标题：**Spark 实战 · 第5–6课**
- 主标题：**Spark SQL 实战 × 性能调优**
- 副标题英文：`Spark SQL in Practice · Performance Tuning`
- 一行 kicker：`大数据工具课 · L5–L6`
- 底部署名位：可放 `bigdata-tools · 课程材料`（占位，老师可改）

### 页 B · 核心知识点页：「数据倾斜」（L6 最有张力的点）
这一页要把**数据倾斜**讲清楚，必须包含以下真实内容（全部来自 L6 原材料，不许编造）：
- 标题：**数据倾斜 Data Skew**
- 一句话本质：`为什么 99 个 Task 都完成了，第 100 个还在跑？`
- **关键对比数据（这是这页的视觉主角）**：`99 tasks: 2s` ｜ `1 task: 47min`
- 成因：GroupBy / Join 后，某个 key 的数据量远大于其他 key，一个 Task 处理了 90% 数据，其他 Task 早就空闲。
- 典型场景：电商日志里"爬虫账号"产生百万条记录；Join 时 NULL 值被当成同一个 key 聚合。
- 解决方案两种武器（可作为这页的收尾指引，点到为止）：**加盐打散**（GroupBy 倾斜）+ **广播 Join**（Join 倾斜）。
- 可选视觉：Task duration 横条图——8 个 Task，7 个 1-2s（绿/青），第 8 个 47min（红）拉满。

## 内容来源（全部已抽取，禁止编造新事实）
### L5 · Spark SQL 实战
- DataFrame API vs Spark SQL：两种写法，经 Catalyst 优化后**性能完全相同**。
- 5 个核心算子：DataFrame 基础 / GroupBy（触发 Shuffle）/ Join（inner/left/right/full + broadcast）/ 窗口函数（保留所有行 vs GroupBy 压缩成一行）/ Spark SQL（注册临时视图）。
- 电商实战：Hive SQL → Spark SQL 迁移（GMV 统计 / 复购率 / 销售排名 TOP3）。
- 用户行为漏斗：曝光→点击→加购→下单→支付，10000→700，整体转化率 7%。
### L6 · 性能调优
- 数据倾斜原理（99 Task 2s vs 1 Task 47min）+ 定位（Spark UI → Stages → Tasks 看 max vs median）。
- 加盐打散：给倾斜 key 加随机后缀打散成 N 份 → 局部聚合 → 去盐 → 全局再聚合。
- 广播 Join：小表 broadcast 到每个节点，大表零 Shuffle 本地 join。
- 调优参数：`spark.sql.shuffle.partitions`(默认200,最常调) / `executor.memory` / `executor.cores` / `autoBroadcastJoinThreshold`(10MB) / **AQE `spark.sql.adaptive.enabled`**(3.x 默认开,自动处理倾斜)。

## 情感基调与气质
- 技术、专业、清晰、有节奏感。投屏 10 秒能抓住一个记忆点。
- 不要 AI slop：不要无意义 emoji 当图标、不要紫渐变、不要圆角卡片+左 border accent 堆砌。
- 代码片段要有语法高亮的质感（这是技术 deck 的灵魂细节）。

## 已知约束
- 中文为主，英文副标题/标签点缀。
- 数字用 tabular-nums 对齐。
- 每页底部可留 footer：左 section 名，右页码占位。
- 字体只用开源（Inter/Geist/Space Grotesk/JetBrains Mono/Fraunces/Newsreader 等），不写付费字体。

## 图片需求
- **无需真图**——这是代码/概念型教学 deck，视觉主角是排版、数据对比、代码高亮、流程图。
- 装饰用纯 CSS 几何 / 内联 SVG（横条图、流程箭头、节点图），不配 stock 灵感图。
