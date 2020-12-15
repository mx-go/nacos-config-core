package com.github.mx.nacos.config.core.annotation;

import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配置类注解
 * <p>
 * Create by max on 2020/12/05
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
@RefreshScope
public @interface RefreshConfig {

    @AliasFor(annotation = Component.class)
    String value() default "";

    @AliasFor(annotation = RefreshScope.class)
    ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;
}