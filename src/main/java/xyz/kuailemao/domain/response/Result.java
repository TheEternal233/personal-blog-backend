package xyz.kuailemao.domain.response;

import lombok.Data;

@Data
public class Result {
    private int code;
    private String msg;
    private Object data;

    private Result(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static Result success() {
        return new Result(200, "成功", null);
    }

    public static Result success(Object data) {
        return new Result(200, "成功", data);
    }

    public static Result error(String msg) {
        return new Result(400, msg, null);
    }
}