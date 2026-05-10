#!/usr/bin/env bash
# 创建课堂演示用 topic
# 用法：bash docker/kafka/init-topics.sh

set -e
BROKER="localhost:9092"
KAFKA_BIN="/opt/kafka/bin"

create_topic() {
  local name=$1
  local partitions=${2:-3}
  docker exec kafka-broker $KAFKA_BIN/kafka-topics.sh \
    --bootstrap-server $BROKER \
    --create --if-not-exists \
    --topic "$name" \
    --partitions "$partitions" \
    --replication-factor 1
}

echo "==> 创建 test (3 partitions)"
create_topic test 3

echo "==> 创建 test1 (1 partition, 单分区演示有序消费)"
create_topic test1 1

echo "==> 创建 test3 (3 partitions, 多分区演示)"
create_topic test3 3

echo "==> 创建 input-topic / output-topic (Streams WordCount 用)"
create_topic input-topic 1
create_topic output-topic 1

echo "==> 创建 spark-input (Spark Structured Streaming 用)"
create_topic spark-input 1

echo "==> 创建 flink-input / flink-events (Flink demo 用)"
create_topic flink-input 1
create_topic flink-events 3

echo
echo "==> 当前 topic 列表："
docker exec kafka-broker $KAFKA_BIN/kafka-topics.sh \
  --bootstrap-server $BROKER --list
