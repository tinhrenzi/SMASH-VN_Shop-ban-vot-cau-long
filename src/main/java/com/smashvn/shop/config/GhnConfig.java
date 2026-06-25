package com.smashvn.shop.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@Data
public class GhnConfig {

    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shop-id}")
    private Integer shopId;

    @Value("${ghn.base-url}")
    private String baseUrl;

    @Value("${ghn.from-district-id}")
    private Integer fromDistrictId;

    @Value("${ghn.from-ward-code}")
    private String fromWardCode;

    @Value("${ghn.from-address:10 Kim Mã, Ba Đình, Hà Nội}")
    private String fromAddress;

    @Value("${ghn.webhook-token:smashvn_ghn_webhook_secret_2026}")
    private String webhookToken;


    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
