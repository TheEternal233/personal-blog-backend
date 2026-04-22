package xyz.kuailemao.annotation;

import java.lang.annotation.*;

/**
 * @author TheEternal
 * @since 2026/3/29
 * 封禁验证注解
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckBlacklist {
}
