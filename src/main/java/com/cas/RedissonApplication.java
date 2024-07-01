package com.cas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author: xianglong[1391086179@qq.com]
 * @date: 上午10:26 2021/3/22
 * @version: V1.0
 * @review:
 */
@ServletComponentScan
@SpringBootApplication(scanBasePackages = "com.cas")
@RestController
public class RedissonApplication {

    public static void main(String[] args) {
        try{
            SpringApplication.run(RedissonApplication.class, args);
            System.out.println("环境启动成功！！！！");
        } catch (Exception e) {
            System.out.println("环境启动失败！！！！");
        }
    }



}
