package com.jloved.strive.gateway;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * @author jiangxl
 * @version V1.0
 * @ClassName StriveGatewayApp
 * @Description 启动类
 * @Date 2021/11/10 15:19
 */
@SpringBootApplication
@EnableDiscoveryClient
public class StriveGatewayApp {

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(StriveGatewayApp.class);
    application.run(args);
  }
}
