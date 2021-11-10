package com.jloved.strive.gateway.configuration;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DynamicRouteServiceImplByNacos implements CommandLineRunner {

  private final DynamicRouteServiceImpl dynamicRouteService;

  private final NacosGatewayProperties nacosGatewayProperties;

  /**
   * 监听Nacos Server下发的动态路由配置
   */
  public void dynamicRouteByNacosListener() {
    try {
      Properties properties = new Properties();
      properties.put(PropertyKeyConst.SERVER_ADDR, nacosGatewayProperties.getAddress());
      properties.put(PropertyKeyConst.NAMESPACE, nacosGatewayProperties.getNameSpace());

      ConfigService configService = NacosFactory.createConfigService(properties);

      configService.addListener(nacosGatewayProperties.getDataId(),
          nacosGatewayProperties.getGroupId(), new Listener() {
            @Override
            public void receiveConfigInfo(String configInfo) {
              List<RouteDefinition> list = JSONObject.parseArray(configInfo, RouteDefinition.class);
              list.forEach(definition -> dynamicRouteService.update(definition));
            }

            @Override
            public Executor getExecutor() {
              return null;
            }
          });
    } catch (NacosException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void run(String... args) {
    dynamicRouteByNacosListener();

  }
}
