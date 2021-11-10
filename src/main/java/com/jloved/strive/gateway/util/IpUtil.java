package com.jloved.strive.gateway.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;

public class IpUtil {

  private static final String UNKNOWN = "unknown";
  private static final String LOCALHOST = "127.0.0.1";
  private static final String SEPARATOR = ",";
  private static final String IP_REX = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\.((1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.){2}(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";


  public static String getIpAddr(HttpServletRequest request) {
    String ipAddress;
    try {
      ipAddress = request.getHeader("x-forwarded-for");
      if (ipAddress == null || ipAddress.length() == 0 || UNKNOWN.equalsIgnoreCase(ipAddress)) {
        ipAddress = request.getHeader("Proxy-Client-IP");
      }
      if (ipAddress == null || ipAddress.length() == 0 || UNKNOWN.equalsIgnoreCase(ipAddress)) {
        ipAddress = request.getHeader("WL-Proxy-Client-IP");
      }
      if (ipAddress == null || ipAddress.length() == 0 || UNKNOWN.equalsIgnoreCase(ipAddress)) {
        ipAddress = request.getRemoteAddr();
        if (LOCALHOST.equals(ipAddress)) {
          InetAddress inet = null;
          try {
            inet = InetAddress.getLocalHost();
          } catch (UnknownHostException e) {
            e.printStackTrace();
          }
          ipAddress = inet.getHostAddress();
        }
      }
      // 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
      // "***.***.***.***".length()
      if (ipAddress != null && ipAddress.length() > 15) {
        if (ipAddress.indexOf(SEPARATOR) > 0) {
          ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
        }
      }
    } catch (Exception e) {
      ipAddress = "";
    }
    return ipAddress;
  }

  /**
   * 验证IP是否属于某个IP段
   *
   * @ip 所验证的IP号码
   * @ipSection IP段(以 ' - ' 分隔)
   */
  public static boolean ipExistsInRange(String ip, String ipSection) {
    ipSection = ipSection.trim();
    ip = ip.trim();
    if (StringUtils.isNotBlank(ipSection) && ipSection.contains("-")) {
      int idx = ipSection.indexOf('-');
      String beginIP = ipSection.substring(0, idx);
      String endIP = ipSection.substring(idx + 1);
      if (!isIp(ip) || !isIp(beginIP) || !isIp(endIP)) {
        return false;
      }
      return getIp2long(beginIP) <= getIp2long(ip) && getIp2long(ip) <= getIp2long(endIP);
    } else {
      return false;
    }
  }

  public static long getIp2long(String ip) {
    ip = ip.trim();
    String[] ips = ip.split("\\.");
    long ip2long = 0L;
    for (int i = 0; i < 4; ++i) {
      ip2long = ip2long << 8 | Integer.parseInt(ips[i]);
    }
    return ip2long;
  }

  public static boolean isIp(String ip) {
    if (StringUtils.isBlank(ip)) {
      return false;
    }
    return Pattern.matches(IP_REX, ip);
  }

  public static void main(String[] args) {
    System.out.println(isIp("172.16.255.254"));
    System.out.println(isIp("10.10.300.116"));
    //10.10.10.116 是否属于固定格式的IP段10.10.1.00-10.10.255.255
    String ip = "10.10.0.116";
    String ipSection = "10.10..00-10.10.255.255";
    boolean exists = ipExistsInRange(ip, ipSection);
    System.out.println(exists);
    System.out.println(getIp2long(ip));
  }

}
