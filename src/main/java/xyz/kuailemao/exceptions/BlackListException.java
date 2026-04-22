package xyz.kuailemao.exceptions;

/**
 * @author TheEternal
 * @since 2026/4/3
 * 自定义黑名单异常
 */
public class BlackListException extends Exception {
    public BlackListException(String message) {
        super(message);
    }
}
