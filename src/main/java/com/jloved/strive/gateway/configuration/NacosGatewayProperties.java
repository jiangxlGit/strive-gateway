package com.jloved.strive.gateway.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class NacosGatewayProperties {

  @Value("${spring.cloud.nacos.config.server-addr}")
  private String address;

  @Value("${spring.cloud.gateway.data-id}")
  private String dataId;

  @Value("${spring.cloud.gateway.group-id}")
  private String groupId;

  @Value("${spring.cloud.gateway.timeout}")
  private Long timeout;

  @Value("${spring.cloud.nacos.config.namespace}")
  private String nameSpace;
}
