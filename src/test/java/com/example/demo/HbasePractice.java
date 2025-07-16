package com.example.demo;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class HbasePractice {
    static {
        // 关闭所有第三方日志
        System.setProperty("org.apache.commons.logging.Log",
                "org.apache.commons.logging.impl.NoOpLog");
        System.setProperty("hbase.root.logger", "OFF");
    }

    @Test
    public void hbaseExample() throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "hadoop102,hadoop103,hadoop104");

        try (Connection connection = ConnectionFactory.createConnection(conf)) {
            Table table = connection.getTable(TableName.valueOf("user"));
            // 插入数据
            Put put = new Put("1004".getBytes());
            put.addColumn("info".getBytes(), "name".getBytes(),"nima".getBytes());
            table.put(put);

            Get get = new Get("1004".getBytes());
            Result result = table.get(get);
            byte[] value = result.getValue("info".getBytes(), "name".getBytes());
            System.out.println(new String(value));
        }
    }
    @Test
//    用HBase Shell创建一个订单表（order），包含buyer和item两个列族，插入三条订单数据，并用scan命令查询。
    public void prac1() throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "hadoop102,hadoop103,hadoop104");
        try(Connection connection = ConnectionFactory.createConnection(conf)) {
            Table table = connection.getTable(TableName.valueOf("order"));
            Put put = new Put("1001".getBytes());
            put.addColumn("buyer".getBytes(),"name".getBytes(),"Alex".getBytes());
            put.addColumn("buyer".getBytes(),"age".getBytes(),"18".getBytes());
            put.addColumn("item".getBytes(),"name".getBytes(),"java".getBytes());
            put.addColumn("item".getBytes(),"desc".getBytes(),"very good".getBytes());
            table.put(put);
        }
    }

    @Test

//    用Java实现：批量插入100条商品数据到商品表（product），并查询某个商品详情。
    public void prac2() throws IOException {
        Configuration conf = HBaseConfiguration.create();
        conf.set("hbase.zookeeper.quorum", "hadoop102,hadoop103,hadoop104");
        try(Connection connection = ConnectionFactory.createConnection(conf)) {
            Table table = connection.getTable(TableName.valueOf("product"));
            for (int i = 0; i <100 ; i++) {
                Put put = new Put(Bytes.toBytes(String.format("%04d", i)));
                put.addColumn("name".getBytes(),"pro_name".getBytes(),("product"+i).getBytes());
                table.put(put);
            }

            Get get = new Get("0006".getBytes());
            Result result = table.get(get);
            byte[] value = result.getValue("name".getBytes(), "pro_name".getBytes());
            System.out.println("==== 这是我的输出 ====");
            System.out.println(new String(value));
            System.out.println("==== 这是我的输出 ====");
        }

    }

}
