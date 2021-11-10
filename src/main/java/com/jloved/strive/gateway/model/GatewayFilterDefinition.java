package com.jloved.strive.gateway.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class GatewayFilterDefinition {

  // Filter Name
  private String name;

  // 对应的路由规则
  private Map<String, String> args = new LinkedHashMap<>();
}