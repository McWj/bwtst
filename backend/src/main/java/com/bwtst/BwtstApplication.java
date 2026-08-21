package com.bwtst;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// add by codebuddy 20260817
@SpringBootApplication
@MapperScan("com.bwtst.mapper")
// add by codebuddy 20260817: 开启定时任务（要货单上传 ERP）
@EnableScheduling
public class BwtstApplication {

    public static void main(String[] args) {
        SpringApplication.run(BwtstApplication.class, args);
    }
}
