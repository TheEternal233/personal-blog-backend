package xyz.kuailemao.utils;

import xyz.kuailemao.domain.response.ResponseResult;

import java.util.function.Supplier;

/**
 * @author ThEt
 * <p>
 * 创建时间：2026/3/09 8:58
 */
public class ControllerUtils {
    public static  <T> ResponseResult<T> messageHandler(Supplier<T> supplier) {
        return ResponseResult.success(supplier.get());
    }
}
