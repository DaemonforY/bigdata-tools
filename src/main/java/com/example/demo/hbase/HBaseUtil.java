package com.example.demo.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Table;

/**
 * HBase 工具类（教学用：单例 Connection + 默认 localhost 连接）
 *
 * <p>连接地址按以下优先级解析，方便不同环境切换：</p>
 * <ol>
 *   <li>JVM 系统属性：{@code -Dhbase.zookeeper.quorum=...}</li>
 *   <li>环境变量：{@code HBASE_ZOOKEEPER_QUORUM}</li>
 *   <li>默认 {@code localhost}（配合 docker/hbase/docker-compose.yml 使用）</li>
 * </ol>
 */
public class HBaseUtil {
    public static final String DEFAULT_QUORUM = "localhost";
    public static final String DEFAULT_ZK_PORT = "2181";

    private static volatile Connection connection;

    public static synchronized Connection getConnection() throws Exception {
        if (connection == null || connection.isClosed()) {
            Configuration conf = HBaseConfiguration.create();
            conf.set("hbase.zookeeper.quorum", resolveQuorum());
            conf.set("hbase.zookeeper.property.clientPort", resolvePort());
            connection = ConnectionFactory.createConnection(conf);
        }
        return connection;
    }

    public static Table getTable(String tableName) throws Exception {
        return getConnection().getTable(TableName.valueOf(tableName));
    }

    private static String resolveQuorum() {
        String v = System.getProperty("hbase.zookeeper.quorum");
        if (v == null || v.isBlank()) {
            v = System.getenv("HBASE_ZOOKEEPER_QUORUM");
        }
        return (v == null || v.isBlank()) ? DEFAULT_QUORUM : v;
    }

    private static String resolvePort() {
        String v = System.getProperty("hbase.zookeeper.property.clientPort");
        if (v == null || v.isBlank()) {
            v = System.getenv("HBASE_ZOOKEEPER_CLIENT_PORT");
        }
        return (v == null || v.isBlank()) ? DEFAULT_ZK_PORT : v;
    }
}
