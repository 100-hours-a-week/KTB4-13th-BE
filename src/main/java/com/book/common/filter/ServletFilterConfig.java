package com.book.common.filter;

import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Servlet 필터를 등록하는 설정 클래스입니다.
 */
@Configuration
public class ServletFilterConfig {


    @Bean
    FilterRegistrationBean<RequestMdcFilter> requestMdcFilterRegistration(final RequestMdcFilter filter) {
        final FilterRegistrationBean<RequestMdcFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 2);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RequestLoggingFilter> requestLoggingFilterRegistration(final RequestLoggingFilter filter) {
        final FilterRegistrationBean<RequestLoggingFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setOrder(SecurityFilterProperties.DEFAULT_FILTER_ORDER - 1);
        return registration;
    }
}
