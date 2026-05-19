package com.kusitms.kkium.global.config;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

  // Netty HTTP 클라이언트 설정
  @Bean
  public ReactorResourceFactory resourceFactory() {
    ReactorResourceFactory factory = new ReactorResourceFactory();
    factory.setUseGlobalResources(false);
    return factory;
  }

  @Bean
  public WebClient webClient() {
    // HTTP 클라이언트 설정
    Function<HttpClient, HttpClient> mapper =
        client ->
            HttpClient.create()
                .compress(true) // gzip/br 응답 자동 디코딩
                .responseTimeout(Duration.ofSeconds(120))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .doOnConnected(
                    connection ->
                        connection
                            .addHandlerLast(new ReadTimeoutHandler(120))
                            .addHandlerLast(new WriteTimeoutHandler(10)));

    // HTTP 클라이언트와 연결
    ClientHttpConnector connector = new ReactorClientHttpConnector(resourceFactory(), mapper);

    ExchangeStrategies strategies =
        ExchangeStrategies.builder()
            .codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB
            .build();

    return WebClient.builder().clientConnector(connector).exchangeStrategies(strategies).build();
  }

  @Bean
  public ObjectMapper objectMapper() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    JavaTimeModule module = new JavaTimeModule();
    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
    module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));

    return new ObjectMapper()
        .registerModule(module)
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
  }
}
