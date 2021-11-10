package com.jloved.strive.gateway.model;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;

@Data
public class GatewayPredicateDefinition {

  // 断言对应的Name
  private String name;

  // 配置的断言规则
  private Map<String, String> args = new LinkedHashMap<>();
}