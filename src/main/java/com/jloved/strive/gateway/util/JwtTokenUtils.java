package com.jloved.strive.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.CompressionCodecs;
import io.jsonwebtoken.CompressionException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Jwt工具类
 *
 * @author jiangxl
 * @date 2020-03-25
 */

@Slf4j
@Component
public class JwtTokenUtils implements InitializingBean {

  private final JwtSecurityProperties jwtSecurityProperties;
  public static final String AUTHORITIES_KEY = "auth";
  private Key key;

  public JwtTokenUtils(JwtSecurityProperties jwtSecurityProperties) {
    this.jwtSecurityProperties = jwtSecurityProperties;
  }

  @Override
  public void afterPropertiesSet() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecurityProperties.getBase64Secret());
    this.key = Keys.hmacShaKeyFor(keyBytes);
  }

  public String createToken(String subject) {
    Map<String, Object> claims = new HashMap<>(16);
    claims.put("roles", "user");
    return Jwts.builder()
        .setSubject(subject)
        .claim(AUTHORITIES_KEY, claims)
        .setId(UUID.randomUUID().toString())
        .setIssuedAt(new Date())
        .setExpiration(
            new Date((new Date()).getTime() + jwtSecurityProperties.getTokenValidityInSeconds()))
        .compressWith(CompressionCodecs.DEFLATE)
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  public String createToken(String subject, Map<String, Object> claims) {
    return Jwts.builder()
        .setSubject(subject)
        .claim(AUTHORITIES_KEY, claims)
        .setId(UUID.randomUUID().toString())
        .setIssuedAt(new Date())
        .setExpiration(
            new Date((new Date()).getTime() + jwtSecurityProperties.getTokenValidityInSeconds()))
        .compressWith(CompressionCodecs.DEFLATE)
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  public Date getExpirationDateFromToken(String token) {
    Date expiration;
    try {
      final Claims claims = getClaimsFromToken(token);
      expiration = claims.getExpiration();
    } catch (Exception e) {
      expiration = null;
    }
    return expiration;
  }

  @SneakyThrows
  public boolean validateToken(String authToken) {
    try {
      Jwts.parser().setSigningKey(key).parseClaimsJws(authToken);
      return true;
    } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
      log.info("Invalid JWT signature.");
      throw new Exception("Invalid JWT signature");
    } catch (ExpiredJwtException e) {
      log.info("Expired JWT token.");
      throw new Exception("Expired JWT token");
    } catch (UnsupportedJwtException e) {
      log.info("Unsupported JWT token.");
      throw new Exception("Unsupported JWT token");
    } catch (IllegalArgumentException e) {
      log.info("JWT token compact of handler are invalid.");
      throw new Exception("JWT token compact of handler are invalid");
    } catch (CompressionException e) {
      log.info("JWT token compression exception.");
      throw new Exception("JWT token compression exception");
    }
  }

  public Claims getClaimsFromToken(String token) {
    Claims claims;
    try {
      claims = Jwts.parser()
          .setSigningKey(key)
          .parseClaimsJws(token)
          .getBody();
    } catch (Exception e) {
      claims = null;
    }
    return claims;
  }
}
