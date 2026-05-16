package xyz.kuailemao.constants;

/**
 * @author TheEternal
 * @since 2026/3/27
 * 黑名单常量类
 */
public class BlackListConst {

    /**
     * 黑名单类型：用户
     */
    public static final int BLACK_LIST_TYPE_USER = 1;

    /**
     * 黑名单类型：路人或攻击者
     */
    public static final int BLACK_LIST_TYPE_BOT = 2;

    /**
     * 限流白名单用户ID（不受限流和封号限制）
     */
    public static final Long RATE_LIMIT_WHITELIST_UID = 88065989L;

    /**
     * 限流白名单IP（不受限流和封号限制）
     */
    public static final String RATE_LIMIT_WHITELIST_IP = "127.0.0.1";
}
