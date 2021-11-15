package com.jloved.strive.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
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

  public Authentication getAuthentication(String token) {
    Claims claims = Jwts.parser()
        .setSigningKey(key)
        .parseClaimsJws(token)
        .getBody();

    if (claims.get(AUTHORITIES_KEY) != null) {
      Collection<? extends GrantedAuthority> authorities =
          Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
              .map(SimpleGrantedAuthority::new)
              .collect(Collectors.toList());

      User principal = new User(claims.getSubject(), "", authorities);

      return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    } else {
      return null;
    }
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
