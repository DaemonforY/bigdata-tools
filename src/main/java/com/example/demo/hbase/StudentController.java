package com.example.demo.hbase;

import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
/**
 * @Description Controller 示例（核心功能）
 * @Author miaoyongbin
 * @Date 2025/7/2 06:54:21
 * @Version 1.0
 */
@Controller
@RequestMapping("/hbase/student")
public class StudentController {
    // 学生列表页面
    @GetMapping("/list")
    public String list(Model model) throws Exception {
        Table table = HBaseUtil.getTable("student");
        Scan scan = new Scan();
        return getString(model, table, scan);
    }

    private String getString(Model model, Table table, Scan scan) throws IOException {
        ResultScanner scanner = table.getScanner(scan);
        List<Map<String, String>> students = new ArrayList<>();
        for (Result res : scanner) {
            Map<String, String> stu = new HashMap<>();
            String rowKey = Bytes.toString(res.getRow());
            stu.put("id", rowKey);
            stu.put("name", Bytes.toString(res.getValue(Bytes.toBytes("info"), Bytes.toBytes("name"))));
            stu.put("age", Bytes.toString(res.getValue(Bytes.toBytes("info"), Bytes.toBytes("age"))));
            stu.put("score", Bytes.toString(res.getValue(Bytes.toBytes("score"), Bytes.toBytes("math"))));
            students.add(stu);
        }
        table.close();
        model.addAttribute("students", students);
        return "hbase/student_list";
    }

    // 插入学生页面
    @GetMapping("/add")
    public String addPage() {
        return "hbase/student_add";
    }

    // 插入学生操作
    @PostMapping("/add")
    public String add(@RequestParam String id, @RequestParam String name,
                      @RequestParam String age, @RequestParam String score) throws Exception {
        Table table = HBaseUtil.getTable("student");
        Put put = new Put(Bytes.toBytes(id));
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"), Bytes.toBytes(name));
        put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("age"), Bytes.toBytes(age));
        put.addColumn(Bytes.toBytes("score"), Bytes.toBytes("math"), Bytes.toBytes(score));
        table.put(put);
        table.close();
        return "redirect:/student/list";
    }

    // 查看学生详情
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable String id, Model model) throws Exception {
        Table table = HBaseUtil.getTable("student");
        Get get = new Get(Bytes.toBytes(id));
        Result res = table.get(get);
        Map<String, String> stu = new HashMap<>();
        stu.put("id", id);
        stu.put("name", Bytes.toString(res.getValue(Bytes.toBytes("info"), Bytes.toBytes("name"))));
        stu.put("age", Bytes.toString(res.getValue(Bytes.toBytes("info"), Bytes.toBytes("age"))));
        stu.put("score", Bytes.toString(res.getValue(Bytes.toBytes("score"), Bytes.toBytes("math"))));
        table.close();
        model.addAttribute("stu", stu);
        return "hbase/student_detail";
    }

    // 按前缀查询
    @GetMapping("/prefix")
    public String prefix(@RequestParam String prefix, Model model) throws Exception {
        Table table = HBaseUtil.getTable("student");
        Scan scan = new Scan();
        scan.setRowPrefixFilter(Bytes.toBytes(prefix));
        return getString(model, table, scan);
    }

    // 多版本查询
    @GetMapping("/version/{id}")
    public String version(@PathVariable String id, Model model) throws Exception {
        Table table = HBaseUtil.getTable("student");
        Get get = new Get(Bytes.toBytes(id));
        get.readAllVersions();
        get.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"));
        Result res = table.get(get);
        List<String> names = new ArrayList<>();
        for (Cell cell : res.rawCells()) {
            names.add(Bytes.toString(CellUtil.cloneValue(cell)) + " (ts=" + cell.getTimestamp() + ")");
        }
        table.close();
        model.addAttribute("id", id);
        model.addAttribute("names", names);
        return "hbase/student_versions";
    }
}
