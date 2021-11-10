package com.jloved.strive.gateway;


import java.io.IOException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.io.support.PropertiesLoaderUtils;

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

  public static void main(String[] args) throws IOException {
    SpringApplication application = new SpringApplication(StriveGatewayApp.class);
    application.setDefaultProperties(PropertiesLoaderUtils.loadAllProperties("application.properties"));
    application.run(args);
  }

  protected SpringApplicationBuilder configure(
      SpringApplicationBuilder builder) {
    return builder.sources(this.getClass());
  }
}
