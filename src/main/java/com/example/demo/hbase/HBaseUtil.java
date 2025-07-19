package com.example.demo.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
/**
 * @Description HBase 工具类（简化连接与操作）
 * @Author miaoyongbin
 * @Date 2025/7/2 06:53:32
 * @Version 1.0
 */
public class HBaseUtil {
    private static Connection connection;

    public static synchronized Connection getConnection() throws Exception {
        if (connection == null || connection.isClosed()) {
            Configuration conf = HBaseConfiguration.create();
            conf.set("hbase.zookeeper.quorum", "39.99.241.140");
            connection = ConnectionFactory.createConnection(conf);
        }
        return connection;
    }

    public static Table getTable(String tableName) throws Exception {
        return getConnection().getTable(TableName.valueOf(tableName));
    }
}