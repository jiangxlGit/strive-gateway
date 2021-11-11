package com.jloved.strive.gateway.configuration;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.Configuration;

@Data
@RefreshScope
@Configuration
public class NacosGatewayProperties {

  /**
   * 网关nacos配置服务地址
   */
  @Value("${spring.cloud.nacos.config.server-addr}")
  private String address;

  /**
   * 网关nacos配置data-id
   */
  @Value("${spring.cloud.gateway.data-id}")
  private String dataId;

  /**
   * 网关nacos配置group-id
   */
  @Value("${spring.cloud.gateway.group-id}")
  private String groupId;

  /**
   * 网关超时时间
   */
  @Value("${spring.cloud.gateway.timeout}")
  private Long timeout;

  /**
   * 命名空间
   */
  @Value("${spring.cloud.nacos.config.namespace}")
  private String nameSpace;

  /**
   * 后台登入有效时间
   */
  @Value("${jwt.secretKey}")
  private String secretKey;

  /**
   * 跳过验证url列表
   */
  @Value("${jwt.auth.skip.urls}")
  private String[] skipAuthUrls;

  /**
   * 是否需要验证，true：需要，false：不需要
   */
  @Value("${jwt.auth.switch}")
  private Boolean authSwitch;

  /**
   * 白名单列表
   */
  @Value("${jwt.white.list}")
  private String[] whiteList;

  /**
   * 后台登入有效时间
   */
  @Value("${back.token.expire.time:9000}")
  private long backTokenExpireTime;
}
