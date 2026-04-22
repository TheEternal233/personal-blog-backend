package xyz.kuailemao.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author TheEternal
 * @since 2026/4/2
 */
@Getter
@AllArgsConstructor
public enum RequestHeaderEnum {

    /**
     * Github获取个人信息Accept请求头
     */
    GITHUB_USER_INFO("Accept", "application/vnd.github.v3+json");


    /**
     * 请求头
     */
    public final String header;

    /**
     * 内容
     */
    public final String content;
}
