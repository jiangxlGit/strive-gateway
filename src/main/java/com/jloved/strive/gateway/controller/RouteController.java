package com.jloved.strive.gateway.controller;

import com.jloved.strive.gateway.configuration.DynamicRouteServiceImpl;
import com.jloved.strive.gateway.model.GatewayFilterDefinition;
import com.jloved.strive.gateway.model.GatewayPredicateDefinition;
import com.jloved.strive.gateway.model.GatewayRouteDefinition;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/route")
public class RouteController {

  @Autowired
  private DynamicRouteServiceImpl dynamicRouteService;

  //增加路由
  @PostMapping("/add")
  public String add(@RequestBody GatewayRouteDefinition gwdefinition) {
    try {
      RouteDefinition definition = assembleRouteDefinition(gwdefinition);
      return this.dynamicRouteService.add(definition);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "succss";
  }

  //删除路由
  @DeleteMapping("/routes/{id}")
  public String delete(@PathVariable String id) {
    return this.dynamicRouteService.delete(id);
  }

  //更新路由
  @PostMapping("/update")
  public String update(@RequestBody GatewayRouteDefinition gwdefinition) {
    RouteDefinition definition = assembleRouteDefinition(gwdefinition);
    return this.dynamicRouteService.update(definition);
  }

  private RouteDefinition assembleRouteDefinition(GatewayRouteDefinition gwdefinition) {

    RouteDefinition definition = new RouteDefinition();

    // ID
    definition.setId(gwdefinition.getId());

    // Predicates
    List<PredicateDefinition> pdList = new ArrayList<>();
    for (GatewayPredicateDefinition gpDefinition : gwdefinition.getPredicates()) {
      PredicateDefinition predicate = new PredicateDefinition();
      predicate.setArgs(gpDefinition.getArgs());
      predicate.setName(gpDefinition.getName());
      pdList.add(predicate);
    }
    definition.setPredicates(pdList);

    // Filters
    List<FilterDefinition> fdList = new ArrayList<>();
    for (GatewayFilterDefinition gfDefinition : gwdefinition.getFilters()) {
      FilterDefinition filter = new FilterDefinition();
      filter.setArgs(gfDefinition.getArgs());
      filter.setName(gfDefinition.getName());
      fdList.add(filter);
    }
    definition.setFilters(fdList);

    // URI
    URI uri = UriComponentsBuilder.fromUriString(gwdefinition.getUri()).build().toUri();
    definition.setUri(uri);

    return definition;
  }
}