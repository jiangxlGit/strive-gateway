package com.jloved.strive.gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.jloved.strive.gateway.configuration.NacosGatewayProperties;
import com.jloved.strive.gateway.util.IpUtil;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.Data;
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
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Data
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthFilter implements GlobalFilter, Ordered {

  private final NacosGatewayProperties nacosGatewayProperties;

  private final StringRedisTemplate stringRedisTemplate;

  @Override
  public int getOrder() {
    return -100;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String url = exchange.getRequest().getURI().getPath();
    //跳过不需要验证的路径
    List<String> list = Arrays.asList(nacosGatewayProperties.getSkipAuthUrls());
    if (!nacosGatewayProperties.getAuthSwitch()) {
      return chain.filter(exchange);
    }
    if (list.contains(url) || isIPWhiteList(exchange.getRequest().getRemoteAddress())) {
      return chain.filter(exchange);
    }
    //从请求头中取出token
    String token = exchange.getRequest().getHeaders().getFirst("Authorization");

    //未携带token或token在黑名单内
    if (token == null || token.isEmpty()) {
      return writeTo(exchange, "401", "401 Unauthorized");
    }

    if (!stringRedisTemplate.hasKey(token)) {
      log.info("session unauthorized,token:{}", token);
      return writeTo(exchange, "401", "Session Unauthorized");
    }

    //取出token包含的身份
    String userName = verifyJWT(token, url);
    if (userName.isEmpty()) {
      log.info("invalid token,token:{}", token);
      return writeTo(exchange, "401", "invalid token");
    }
    //将现在的request，添加当前身份
    ServerHttpRequest mutableReq = exchange.getRequest().mutate()
        .header("Authorization-UserName", userName).build();
    ServerWebExchange mutableExchange = exchange.mutate().request(mutableReq).build();

    String refreshTokenKey = stringRedisTemplate.opsForValue().get(token);
    String querySource = stringRedisTemplate.opsForHash().get(refreshTokenKey, "source").toString();
    if ("back".equals(querySource)) {
      //刷新token 失效时间
      stringRedisTemplate.expire(token, nacosGatewayProperties.getBackTokenExpireTime(), TimeUnit.SECONDS);
    } else {
      //刷新token 失效时间
      stringRedisTemplate.expire(token, 0, TimeUnit.SECONDS);
    }

    return chain.filter(mutableExchange);
  }

  private boolean isIPWhiteList(InetSocketAddress remoteAddress) {
    log.info("requestIP:{}", remoteAddress.getHostName());
    List<String> list = Arrays.asList(nacosGatewayProperties.getWhiteList());
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
   * @param token
   * @return userName
   */
  private String verifyJWT(String token, String url) {
    String userName;
    String issuer = "wjt001";

    try {
      Algorithm algorithm = Algorithm.HMAC256(nacosGatewayProperties.getSecretKey());
      JWTVerifier verifier = JWT.require(algorithm)
          .withIssuer(issuer)
          .build();
      DecodedJWT jwt = verifier.verify(token);
      userName = jwt.getClaim("userName").asString();
    } catch (JWTVerificationException e) {
      log.error(e.getMessage(), e);
      return "";
    }
    return userName;
  }

}