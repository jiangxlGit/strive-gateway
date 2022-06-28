package com.jloved.strive.gateway.filter;

import com.jloved.strive.gateway.configuration.NacosGatewayProperties;
import com.jloved.strive.gateway.util.IpUtil;
import com.jloved.strive.gateway.util.JwtSecurityProperties;
import com.jloved.strive.gateway.util.JwtTokenUtils;
import io.jsonwebtoken.Claims;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthFilter implements GlobalFilter, Ordered {

  private final NacosGatewayProperties nacosGatewayProperties;

  private final StringRedisTemplate stringRedisTemplate;

  private final JwtTokenUtils jwtTokenUtils;

  private final JwtSecurityProperties jwtSecurityProperties;

  @Override
  public int getOrder() {
    return -100;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String url = exchange.getRequest().getURI().getPath();
    //跳过不需要验证的路径
    List<String> list = Arrays.asList(nacosGatewayProperties.getSkipAuthUrls());
    log.info(" ===== urlList: {}, url: {}", list, url);
    if (!nacosGatewayProperties.getAuthSwitch()) {
      return chain.filter(exchange);
    }
    if (list.contains(url) || isIPWhiteList(
        Objects.requireNonNull(exchange.getRequest().getRemoteAddress()))) {
      return chain.filter(exchange);
    }
    //从请求头中取出token
    String token = exchange.getRequest().getHeaders().getFirst("Authorization");

    //未携带token或token在黑名单内
    if (token == null || token.isEmpty()) {
      return writeTo(exchange, "401", "401 Unauthorized");
    }

    // 去除token前后空格
    token = token.trim();

    // 删除前缀Bearer(包括空格)
    if (token.startsWith("Bearer ")) {
      token = token.substring(7);
    }

    if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(token))) {
      log.info("session unauthorized,token:{}", token);
      return writeTo(exchange, "401", "Session Unauthorized");
    }

    //取出token包含的身份
    String openId = verifyJWT(token);
    if (StringUtils.isEmpty(openId)) {
      log.info("invalid token,token:{}", token);
      return writeTo(exchange, "401", "invalid token");
    }
    //将现在的request，添加当前身份
    ServerHttpRequest mutableReq = exchange.getRequest().mutate()
        .header("Authorization", token).build();
    ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();

    String refreshTokenKey = stringRedisTemplate.opsForValue().get(token);
    String querySource = stringRedisTemplate.opsForHash().get(refreshTokenKey, "source").toString();
    if ("back".equals(querySource)) {
      //刷新token 失效时间
      stringRedisTemplate.expire(token, jwtSecurityProperties.getTokenValidityInSeconds() / 1000,
          TimeUnit.SECONDS);
    } else {
      //刷新token 失效时间
      stringRedisTemplate.expire(token, 0, TimeUnit.SECONDS);
    }

    return chain.filter(mutableExchange);
  }

  private boolean isIPWhiteList(InetSocketAddress remoteAddress) {
    log.info("requestIP:{}", remoteAddress.getHostName());
    String[] list = nacosGatewayProperties.getWhiteList();
    for (String ipSection : list) {
      if (IpUtil.ipExistsInRange(remoteAddress.getHostName(), ipSection)) {
        return true;
      }
    }
    return false;
  }

  public Mono<Void> writeTo(ServerWebExchange exchange, String code, String message) {
    ServerHttpResponse originalResponse = exchange.getResponse();
    originalResponse.setStatusCode(HttpStatus.OK);
    originalResponse.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
    String response = "{\"code\": \"" + code + "\",\"msg\": \"" + message + ".\"}";
    byte[] result = response.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = originalResponse.bufferFactory().wrap(result);
    return originalResponse.writeWith(Flux.just(buffer));
  }

  /**
   * JWT验证
   *
   * @return userName
   */
  private String verifyJWT(String token) {
    String openId = null;
    if (StringUtils.hasText(token) && jwtTokenUtils.validateToken(token)) {
      Claims claims = jwtTokenUtils.getClaimsFromToken(token);
      Object obj = claims.get(JwtTokenUtils.AUTHORITIES_KEY);
      if (!ObjectUtils.isEmpty(obj) && obj instanceof Map) {
        Map<String, String> map = (Map<String, String>) obj;
        openId = map.get("openId");
      }
    } else {
      log.debug("no valid JWT token found");
    }
    return openId;
  }

}