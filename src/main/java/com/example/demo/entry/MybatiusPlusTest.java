package com.example.demo.entry;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jdk.jfr.TransitionFrom;
import lombok.Data;

import java.util.Date;

@TableName(value = "mybatis_plus_test")
@Data
public class MybatiusPlusTest {

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("mybatis_plus")
    private String mybatisPlus;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableField("create_by")
    @TransitionFrom
    private String createBy;

}
