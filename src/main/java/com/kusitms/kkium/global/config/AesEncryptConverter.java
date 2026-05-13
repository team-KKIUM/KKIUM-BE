package com.kusitms.kkium.global.config;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Converter
@Component
public class AesEncryptConverter implements AttributeConverter<String, String> {

  @Value("${notion.aes-secret-key}")
  private String secretKey;

  private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
  private static final int IV_SIZE = 16;

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) return null;
    try {
      byte[] iv = new byte[IV_SIZE];
      new SecureRandom().nextBytes(iv);
      IvParameterSpec ivSpec = new IvParameterSpec(iv);
      SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
      byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
      byte[] combined = new byte[IV_SIZE + encrypted.length];
      System.arraycopy(iv, 0, combined, 0, IV_SIZE);
      System.arraycopy(encrypted, 0, combined, IV_SIZE, encrypted.length);
      return Base64.getEncoder().encodeToString(combined);
    } catch (Exception e) {
      throw new RuntimeException("암호화 실패", e);
    }
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) return null;
    try {
      byte[] combined = Base64.getDecoder().decode(dbData);
      byte[] iv = new byte[IV_SIZE];
      byte[] encrypted = new byte[combined.length - IV_SIZE];
      System.arraycopy(combined, 0, iv, 0, IV_SIZE);
      System.arraycopy(combined, IV_SIZE, encrypted, 0, encrypted.length);
      IvParameterSpec ivSpec = new IvParameterSpec(iv);
      SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
      Cipher cipher = Cipher.getInstance(ALGORITHM);
      cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new RuntimeException("복호화 실패", e);
    }
  }
}
