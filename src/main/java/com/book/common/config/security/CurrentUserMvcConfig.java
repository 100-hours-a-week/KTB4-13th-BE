package com.book.common.config.security;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the current-user argument resolver with Spring MVC. */
@Configuration(proxyBeanMethods = false)
public class CurrentUserMvcConfig implements WebMvcConfigurer {
    @Override
    public void addArgumentResolvers(final List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver());
    }
}
