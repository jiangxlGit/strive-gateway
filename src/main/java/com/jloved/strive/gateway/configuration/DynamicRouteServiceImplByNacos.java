package com.jloved.strive.gateway.configuration;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import com.jloved.strive.gateway.service.DynamicRouteServiceImpl;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DynamicRouteServiceImplByNacos {

  private final DynamicRouteServiceImpl dynamicRouteService;

  private final NacosGatewayProperties nacosGatewayProperties;

  private ConfigService configService;

  @PostConstruct
  public void init() {
    log.info("gateway route init...");
    try {
      configService = initConfigService();
      if (configService == null) {
        log.warn("initConfigService fail");
        return;
      }
      String configInfo = configService.getConfig(nacosGatewayProperties.getDataId(),
          nacosGatewayProperties.getGroupId(), nacosGatewayProperties.getTimeout());
      log.info("获取网关当前配置:\r\n{}", configInfo);
      List<RouteDefinition> definitionList = JSON.parseArray(configInfo, RouteDefinition.class);
      if (definitionList != null) {
        for (RouteDefinition definition : definitionList) {
          log.info("update route : {}", definition.toString());
          dynamicRouteService.add(definition);
        }
      }
    } catch (Exception e) {
      log.error("初始化网关路由时发生错误", e);
    }
    dynamicRouteByNacosListener(nacosGatewayProperties.getDataId(), nacosGatewayProperties.getGroupId());
  }

  /**
   * 监听Nacos下发的动态路由配置
   *
   * @param dataId
   * @param group
   */
  public void dynamicRouteByNacosListener(String dataId, String group) {
    try {
      configService.addListener(dataId, group, new Listener() {
        @Override
        public void receiveConfigInfo(String configInfo) {
          log.info("进行网关更新:\n\r{}", configInfo);
          List<RouteDefinition> definitionList = JSON.parseArray(configInfo, RouteDefinition.class);
          log.info("update route : {}", definitionList.toString());
          dynamicRouteService.updateList(definitionList);
        }

        @Override
        public Executor getExecutor() {
          log.info("getExecutor\n\r");
          return null;
        }
      });
    } catch (NacosException e) {
      log.error("从nacos接收动态路由配置出错!!!", e);
    }
  }

  /**
   * 初始化网关路由 nacos config
   *
   * @return
   */
  private ConfigService initConfigService() {
    try {
      Properties properties = new Properties();
      properties.setProperty("serverAddr", nacosGatewayProperties.getAddress());
      properties.setProperty("namespace", nacosGatewayProperties.getNameSpace());
      properties.setProperty("username", "nacos");
      properties.setProperty("password", "nacos");
      return configService = NacosFactory.createConfigService(properties);
    } catch (Exception e) {
      log.error("初始化网关路由时发生错误", e);
      return null;
    }
  }
}
