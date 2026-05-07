package xyz.kuailemao.utils;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;

@Component
public class AiFunctionUtil {

    // ====================== 1. 获取当前时间 ======================
    @Description("获取当前时间")
    @Bean
    public Function<TimeRequest, String> getCurrentTime() {
        return request -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return "当前时间：" + ZonedDateTime.now(ZoneId.of("Asia/Shanghai")).format(formatter);
        };
    }
    public record TimeRequest() {}




    // ====================== 5. 随机励志语录 ======================
    @Description("随机返回一句励志语录")
    @Bean
    public Function<QuoteRequest, String> randomQuote() {
        return req -> {
            List<String> quotes = Arrays.asList(
                "你只管努力，剩下的交给时间。",
                "慢慢来，谁都有发光的机会。",
                "生活原本沉闷，但跑起来就有风。",
                    "江山雾笼烟雨摇，十年一剑斩皇朝"
            );
            return quotes.get(new Random().nextInt(quotes.size()));
        };
    }
    public record QuoteRequest() {}

    // ====================== 6. 生成强密码 ======================
    @Description("生成随机安全密码")
    @Bean
    public Function<PwdRequest, String> generatePassword() {
        return req -> {
            String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890!@#$%^&*";
            StringBuilder sb = new StringBuilder();
            for (int i=0;i<12;i++) sb.append(chars.charAt(new Random().nextInt(chars.length())));
            return "你的随机密码：" + sb;
        };
    }
    public record PwdRequest() {}



    // ====================== 9. 今日幸运数字 ======================
    @Description("获取今日幸运数字")
    @Bean
    public Function<LuckyRequest, String> luckyNumber() {
        return req -> "今日幸运数字：" + new Random().nextInt(99);
    }
    public record LuckyRequest() {}

    // ====================== 10. 统计字数 ======================
    @Description("统计一段文字的字数")
    @Bean
    public Function<WordCountRequest, String> wordCount() {
        return req -> "总字数：" + req.text().length();
    }
    public record WordCountRequest(String text) {}
}