package io.github.seal90.serviceclient.carries.by.rsocket.api;

import lombok.Getter;

@Getter
public enum ErrorCodeCore implements ErrorCode {
    SUCCESS("SUCCESS", "SUCCESS"),
    ERROR("ERROR", "ERROR"),
    UNIMPLEMENTED("UNIMPLEMENTED", "UNIMPLEMENTED"),
    ;
    private String code;
    private String message;

    private ErrorCodeCore(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
