package com.bbww.easyledger;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.bbww.easyledger.mapper")
public class EasyLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyLedgerApplication.class, args);
    }

}
