package com.gestao.lafemme.api.config;

import com.sdk.mpoauth.config.MercadoPagoOAuthAutoConfiguration;
import com.sdk.mpoauth.controller.MercadoPagoOAuthController;
import com.sdk.mpoauth.service.MercadoPagoOAuthService;
import com.gestao.lafemme.api.controllers.MercadoPagoCallbackController;
import com.gestao.lafemme.api.services.ConfiguracaoService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(MercadoPagoOAuthAutoConfiguration.class)
public class SdkConfig {

    @Bean
    public MercadoPagoOAuthController mercadoPagoOAuthController(
            MercadoPagoOAuthService oAuthService, 
            ConfiguracaoService configuracaoService
    ) {
        return new MercadoPagoCallbackController(oAuthService, configuracaoService);
    }
}