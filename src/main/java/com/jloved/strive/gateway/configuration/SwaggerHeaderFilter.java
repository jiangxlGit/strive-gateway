package com.jloved.strive.gateway.configuration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
/**
 * @ClassName   SwaggerHeaderFilter
 * @Description 如果是高版本的Spring Cloud Gateway，那么yml配置文件中的SwaggerHeaderFilter配置应该去掉
 * @author      jiangxl
 * @Date        2021/11/11 17:25
 * @version     V1.0
 */
@Component
public class SwaggerHeaderFilter extends AbstractGatewayFilterFactory {

  private static final String HEADER_NAME = "X-Forwarded-Prefix";

  @Override
  public GatewayFilter apply(Object config) {
    return (exchange, chain) -> {
      ServerHttpRequest request = exchange.getRequest();
      String path = request.getURI().getPath();
      if (!StringUtils.endsWithIgnoreCase(path, SwaggerProvider.API_URI)) {
        return chain.filter(exchange);
      }

      String basePath = path.substring(0, path.lastIndexOf(SwaggerProvider.API_URI));
      System.out.println("basePath is:" + basePath);

      ServerHttpRequest newRequest = request.mutate().header(HEADER_NAME, basePath).build();
      ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
      return chain.filter(newExchange);
    };
  }
}
