package com.jloved.strive.gateway.exception;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.authority.AuthorityException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowException;
import com.alibaba.csp.sentinel.slots.system.SystemBlockException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

/**
 * 错误页优化，自定义返回数据
 */
@Component
public class MyUrlBlockHandler implements BlockExceptionHandler {

  @Override
  public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
      BlockException e) throws Exception {
    JsonResult error = null;
    if (e instanceof FlowException) {
      // 限流异常
      error = JsonResult.builder()
          .status(-1)
          .msg("被限流了")
          .build();
    } else if (e instanceof DegradeException) {
      error = JsonResult.builder()
          .status(-1)
          .msg("降级")
          .build();
    } else if (e instanceof ParamFlowException) {
      error = JsonResult.builder()
          .status(-1)
          .msg("热点参数限流")
          .build();
    } else if (e instanceof SystemBlockException) {
      error = JsonResult.builder()
          .status(-1)
          .msg("系统规则不通过")
          .build();
    } else if (e instanceof AuthorityException) {
      error = JsonResult.builder()
          .status(-1)
          .msg("授权规则不通过")
          .build();
    }
    httpServletResponse.setStatus(500);
    httpServletResponse.setCharacterEncoding("utf-8");
    httpServletResponse.setHeader("Content-type", "application/json;charset=utf-8");
    httpServletResponse.setContentType("application/json;charset=utf-8");
    new ObjectMapper().writeValue(httpServletResponse.getWriter(), error);
  }
}

@Builder
@Data
class JsonResult {

  private Integer status;
  private String msg;
}
