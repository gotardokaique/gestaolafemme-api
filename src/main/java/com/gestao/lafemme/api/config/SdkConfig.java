package com.gestao.lafemme.api.config;

import com.sdk.mpoauth.config.MercadoPagoOAuthAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(MercadoPagoOAuthAutoConfiguration.class)
public class SdkConfig {
}