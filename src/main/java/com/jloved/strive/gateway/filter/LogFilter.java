package com.jloved.strive.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class LogFilter implements GlobalFilter, Ordered {

  @Override
  public int getOrder() {
    return -20;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

    StringBuilder logBuilder = new StringBuilder();
    ServerHttpRequest serverHttpRequest = exchange.getRequest();
    String method = serverHttpRequest.getMethodValue().toUpperCase();
    logBuilder.append(method).append(",").append(serverHttpRequest.getURI());
    if ("POST".equals(method)) {
      String body = exchange.getAttributeOrDefault("cachedRequestBody", "");
      if (StringUtils.isNotBlank(body)) {
        logBuilder.append(",body=").append(body);
      }
    }

    ServerHttpResponse serverHttpResponse = exchange.getResponse();
    DataBufferFactory bufferFactory = serverHttpResponse.bufferFactory();
    ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(serverHttpResponse) {
      @Override
      public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
        if (body instanceof Flux) {
          Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>) body;
          return super.writeWith(fluxBody.map(dataBuffer -> {
            byte[] content = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(content);
            DataBufferUtils.release(dataBuffer);
            String resp = new String(content, StandardCharsets.UTF_8);
            logBuilder.append(",resp=").append(resp);
            log.info(logBuilder.toString());
            byte[] uppedContent = new String(content, StandardCharsets.UTF_8).getBytes();
            return bufferFactory.wrap(uppedContent);
          }));
        }
        return super.writeWith(body);
      }
    };
    return chain.filter(exchange.mutate().response(decoratedResponse).build());
  }

}