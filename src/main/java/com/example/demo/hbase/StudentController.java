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
        return scanToView(model, new Scan());
    }

    private String scanToView(Model model, Scan scan) throws IOException {
        List<Map<String, String>> students = new ArrayList<>();
        try (Table table = HBaseUtil.getTable("student");
             ResultScanner scanner = table.getScanner(scan)) {
            for (Result res : scanner) {
                Map<String, String> stu = new HashMap<>();
                stu.put("id", Bytes.toString(res.getRow()));
                stu.put("name", Bytes.toString(res.getValue(Bytes.toBytes("info"), Bytes.toBytes("name"))));
                stu.put("age", Bytes.toString(res.getValue(Bytes.toBytes("info"), Bytes.toBytes("age"))));
                stu.put("score", Bytes.toString(res.getValue(Bytes.toBytes("score"), Bytes.toBytes("math"))));
                students.add(stu);
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
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
        try (Table table = HBaseUtil.getTable("student")) {
            Put put = new Put(Bytes.toBytes(id));
            put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"), Bytes.toBytes(name));
            put.addColumn(Bytes.toBytes("info"), Bytes.toBytes("age"), Bytes.toBytes(age));
            put.addColumn(Bytes.toBytes("score"), Bytes.toBytes("math"), Bytes.toBytes(score));
            table.put(put);
        }
        return "redirect:/hbase/student/list";
    }

    // 查看学生详情
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable String id, Model model) throws Exception {
        Map<String, String> stu = new HashMap<>();
        try (Table table = HBaseUtil.getTable("student")) {
            Result res = table.get(new Get(Bytes.toBytes(id)));
            stu.put("id", id);
            stu.put("name", Bytes.toString(res.getValue(Bytes.toBytes("info"), Bytes.toBytes("name"))));
            stu.put("age", Bytes.toString(res.getValue(Bytes.toBytes("info"), Bytes.toBytes("age"))));
            stu.put("score", Bytes.toString(res.getValue(Bytes.toBytes("score"), Bytes.toBytes("math"))));
        }
        model.addAttribute("stu", stu);
        return "hbase/student_detail";
    }

    // 按前缀查询
    @GetMapping("/prefix")
    public String prefix(@RequestParam String prefix, Model model) throws Exception {
        Scan scan = new Scan();
        scan.setRowPrefixFilter(Bytes.toBytes(prefix));
        return scanToView(model, scan);
    }

    // 多版本查询
    @GetMapping("/version/{id}")
    public String version(@PathVariable String id, Model model) throws Exception {
        List<String> names = new ArrayList<>();
        try (Table table = HBaseUtil.getTable("student")) {
            Get get = new Get(Bytes.toBytes(id));
            get.readAllVersions();
            get.addColumn(Bytes.toBytes("info"), Bytes.toBytes("name"));
            Result res = table.get(get);
            for (Cell cell : res.rawCells()) {
                names.add(Bytes.toString(CellUtil.cloneValue(cell)) + " (ts=" + cell.getTimestamp() + ")");
            }
        }
        model.addAttribute("id", id);
        model.addAttribute("names", names);
        return "hbase/student_versions";
    }
}
