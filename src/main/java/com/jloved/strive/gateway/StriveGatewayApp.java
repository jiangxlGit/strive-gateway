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
    // sentinel 接入网关 设置类型为 1 不设置的话 按普通服务
    System.setProperty("csp.sentinel.app.type", "1");
    SpringApplication application = new SpringApplication(StriveGatewayApp.class);
    application.run(args);
  }
}
