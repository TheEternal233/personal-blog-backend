package xyz.kuailemao.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import xyz.kuailemao.domain.response.ResponseResult;

import java.io.BufferedReader;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author ThEt
 * <p>
 * 创建时间：2026/3/09 8:58
 */
@Slf4j
public class ControllerUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static  <T> ResponseResult<T> messageHandler(Supplier<T> supplier) {
        return ResponseResult.success(supplier.get());
    }

    /**
     * 从请求中提取 content 字段，同时支持 form-urlencoded 和 JSON body
     * 前端通过 JSON.stringify 发送数据，可能带 application/json 或 x-www-form-urlencoded 头
     */
    public static String extractContent(HttpServletRequest request) {
        // 先读 raw body（避免 getParameter 消费流）
        String body = readRawBody(request);

        if (!body.isEmpty()) {
            // 尝试 JSON 对象解析 {"content": "xxx"}
            try {
                Map<?, ?> map = objectMapper.readValue(body, Map.class);
                Object value = map.get("content");
                return value != null ? value.toString() : null;
            } catch (Exception ignored) {}

            // 尝试 JSON 字符串解析 "xxx"
            try {
                String value = objectMapper.readValue(body, String.class);
                if (value != null && !value.isEmpty()) {
                    return value;
                }
            } catch (Exception ignored) {}
        }

        // 回退：从 query string 或 form 参数获取
        return request.getParameter("content");
    }

    private static String readRawBody(HttpServletRequest request) {
        try {
            return new String(request.getInputStream().readAllBytes());
        } catch (Exception e) {
            try {
                BufferedReader reader = request.getReader();
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            } catch (Exception ex) {
                log.warn("读取请求体失败: {}", ex.getMessage());
            }
        }
        return "";
    }
}
