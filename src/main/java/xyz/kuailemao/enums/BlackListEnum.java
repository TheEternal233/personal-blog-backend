package xyz.kuailemao.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author TheEternal
 * @since 2026/4/1
 * 黑名单枚举类
 */
@Getter
@AllArgsConstructor
public enum BlackListEnum {

    // 是否封禁
    IS_BANNED(0, "封禁"),
    IS_NOT_BANNED(1, "未封禁");

    private final Integer code;
    private final String desc;
}
