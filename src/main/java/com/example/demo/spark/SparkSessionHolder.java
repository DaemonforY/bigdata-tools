package com.example.demo.spark;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 整个应用共用一个本地 SparkSession（local[*]，教学环境够用）。
 *
 * 之所以做成 Bean：
 *   1) SparkSession 创建很重（要起 Driver、UI、BlockManager），
 *      每个 Web 请求都新建会卡顿且端口冲突；
 *   2) 关闭时 Spring 会自动调用 close()，避免残留进程。
 *
 * 注意：教学场景把 driver 跑在 Spring Boot 进程内即可；
 * 真正上集群时改成 spark-submit，本类就不再使用。
 */
@Configuration
public class SparkSessionHolder {

    @Bean(destroyMethod = "close")
    public SparkSession sparkSession() {
        SparkConf conf = new SparkConf()
                .setAppName("bigdata-tools-teach")
                .setMaster("local[*]")
                // 教学：让 Spark UI 用 4041，避免和 Spring Boot 8090 / Flink Web 8082 冲突
                .set("spark.ui.port", "4041")
                // 关闭一些花哨日志，控制台清爽点
                .set("spark.sql.shuffle.partitions", "4")
                .set("spark.driver.host", "localhost")
                .set("spark.driver.bindAddress", "127.0.0.1");
        return SparkSession.builder().config(conf).getOrCreate();
    }
}
