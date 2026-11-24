package tn.lamg.gmao.common;

public class ApiException extends RuntimeException {

    public final int status;
    public final String code;

    public ApiException(int status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static void require(boolean condition, String code, String message) {
        if (!condition) throw new ApiException(422, code, message);
    }
}
