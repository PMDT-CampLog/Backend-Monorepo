package com.camplog.profile.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuração para servir mídias de usuário localmente em ambiente de desenvolvimento.
 * Em produção, as mídias são servidas diretamente pelo CDN CloudFront.
 */
@Configuration
public class MediaWebConfig implements WebMvcConfigurer {

    @Value("${app.media.local-upload-dir:./uploads}")
    private String localUploadDir;

    @Value("${app.media.storage-mode:local}")
    private String storageMode;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if ("local".equals(storageMode)) {
            registry.addResourceHandler("/media/**")
                    .addResourceLocations("file:" + localUploadDir + "/")
                    .setCachePeriod(3600);
        }
    }
}
