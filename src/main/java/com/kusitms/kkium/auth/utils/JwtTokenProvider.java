package com.kusitms.kkium.auth.utils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import com.kusitms.kkium.user.utils.CustomUserDetailService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

  @Value("${JWT_SECRET}")
  private String secretKey;

  @Value("${JWT_ACCESS_TOKEN_EXPIRATION}")
  private Long expiration;

  private Key key;

  private final CustomUserDetailService customUserDetailService;

  @PostConstruct
  protected void init() {
    this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
  }

  public String createToken(String userId) {
    Date now = new Date();
    return Jwts.builder()
        .setSubject(userId)
        .setIssuedAt(now)
        .setExpiration(new Date(now.getTime() + expiration))
        .signWith(key, SignatureAlgorithm.HS512)
        .compact();
  }

  public Authentication getAuthentication(String token) {
    UserDetails userDetails = customUserDetailService.loadUserByUsername(this.getUserPk(token));
    return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
  }

  public boolean validateToken(String jwtToken) {
    try {
      Claims claims =
          Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(jwtToken).getBody();
      return !claims.getExpiration().before(new Date());
    } catch (Exception e) {
      return false;
    }
  }

  public String resolveToken(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }

  public String getUserPk(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(key)
        .build()
        .parseClaimsJws(token)
        .getBody()
        .getSubject();
  }
}
