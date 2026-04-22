package xyz.kuailemao.annotation;

import java.lang.annotation.*;

/**
 * @author TheEternal
 * <p>
 * 创建时间：2026/3/29
 * 系统日志记录
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogAnnotation {

    String module() default "";
    String operation() default "";
}
