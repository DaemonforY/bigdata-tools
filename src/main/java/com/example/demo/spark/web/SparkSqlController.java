package com.example.demo.spark.web;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Spark SQL / DataFrame 演示：学生成绩多维分析
 *
 * GET  /spark/sql                 → 模板
 * POST /spark/sql/query           → 跑一段用户输入的 SQL，返回结果
 * GET  /spark/sql/preview         → 看一眼 student_score 表前 20 行
 *
 * 学生看点：
 *   - DataFrame = RDD + Schema + Catalyst 优化器
 *   - 一份数据可以同时被 SQL / DSL 操作
 *   - 学到的 SQL 能直接跑在 PB 级数据上
 */
@Controller
@RequestMapping("/spark/sql")
public class SparkSqlController {

    @Autowired
    private SparkSession spark;

    private volatile boolean prepared = false;

    /** 第一次访问时把演示数据注册成临时视图 */
    private synchronized void ensureTable() {
        if (prepared) return;
        StructType schema = new StructType()
                .add("student", DataTypes.StringType, false)
                .add("clazz",   DataTypes.StringType, false)
                .add("subject", DataTypes.StringType, false)
                .add("score",   DataTypes.IntegerType, false);

        List<Row> rows = Arrays.asList(
                RowFactory.create("张三", "1班", "数学",   95),
                RowFactory.create("张三", "1班", "语文",   80),
                RowFactory.create("张三", "1班", "英语",   72),
                RowFactory.create("李四", "1班", "数学",   88),
                RowFactory.create("李四", "1班", "语文",   91),
                RowFactory.create("李四", "1班", "英语",   85),
                RowFactory.create("王五", "2班", "数学",   60),
                RowFactory.create("王五", "2班", "语文",   77),
                RowFactory.create("王五", "2班", "英语",   90),
                RowFactory.create("赵六", "2班", "数学",   99),
                RowFactory.create("赵六", "2班", "语文",   65),
                RowFactory.create("赵六", "2班", "英语",   88)
        );
        Dataset<Row> df = spark.createDataFrame(rows, schema);
        df.createOrReplaceTempView("student_score");
        prepared = true;
    }

    @GetMapping
    public String demo() {
        return "spark/sql_demo";
    }

    @GetMapping("/preview")
    @ResponseBody
    public List<Map<String, Object>> preview() {
        ensureTable();
        return rowsToList(spark.sql("SELECT * FROM student_score").collectAsList());
    }

    @PostMapping("/query")
    @ResponseBody
    public Map<String, Object> query(@RequestParam String sql) {
        ensureTable();
        long t0 = System.currentTimeMillis();
        Dataset<Row> result;
        try {
            result = spark.sql(sql);
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }

        // 把 explain 也带上，让学生看 Catalyst 优化后的物理计划
        String plan = result.queryExecution().simpleString();
        List<Map<String, Object>> data = rowsToList(result.collectAsList());

        return Map.of(
                "elapsedMs", System.currentTimeMillis() - t0,
                "schema", Arrays.stream(result.schema().fieldNames()).collect(Collectors.toList()),
                "rows", data,
                "plan", plan
        );
    }

    private static List<Map<String, Object>> rowsToList(List<Row> rows) {
        List<Map<String, Object>> data = new ArrayList<>();
        for (Row r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (String f : r.schema().fieldNames()) {
                m.put(f, r.getAs(f));
            }
            data.add(m);
        }
        return data;
    }
}
