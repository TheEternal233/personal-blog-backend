package xyz.kuailemao.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import xyz.kuailemao.interceptor.AccessLimitInterceptor;
import xyz.kuailemao.interceptor.RequestCacheClearInterceptor;

/**
 * @author TheEternal
 * <p>
 * 创建时间：2026/3/29
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private AccessLimitInterceptor accessLimitInterceptor;
    @Resource
    private RequestCacheClearInterceptor interceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // redis限流拦截器
        registry.addInterceptor(accessLimitInterceptor).addPathPatterns("/**").order(1);
        registry.addInterceptor(interceptor).addPathPatterns("/**").order(2);


    }

    // 2. 静态资源配置（新增这个方法！）
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 用户端（根路径）
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");

        // 管理端（/admin/ 路径）
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("classpath:/static/admin/");
    }
}
