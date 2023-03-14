package com.example.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entry.MybatiusPlusTest;
import com.example.demo.mapper.MybatisPlusTestMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping
@RestController
public class DockerDemo {

    @Autowired
    private MybatisPlusTestMapper mapper;

    @RequestMapping("/")
    public String hello() {
        return "Docker，我告诉你，沉默王二是沙雕";
    }

    @PostMapping("/insert")
    public int insert(@RequestBody MybatiusPlusTest test){
        return mapper.insert(test);
    }

    @GetMapping("/getData")
    public MybatiusPlusTest getData(@RequestParam Integer id){
       return mapper.selectById(id);
    }

    @GetMapping("/getDataPage")
    public List<MybatiusPlusTest> getDataPage(){
        Page<MybatiusPlusTest> mybatiusPlusTestPage = mapper.selectPage(new Page<>(1, 10), new QueryWrapper<MybatiusPlusTest>().lambda()
                .eq(MybatiusPlusTest::getMybatisPlus, "111"));

        return  mybatiusPlusTestPage.getRecords();
    }

    @GetMapping("/getData's")
    public MybatiusPlusTest getDataList1(@RequestParam Integer id){
        return mapper.selectById(id);
    }

}