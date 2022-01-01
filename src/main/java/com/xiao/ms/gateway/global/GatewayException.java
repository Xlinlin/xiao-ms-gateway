package com.xiao.ms.gateway.global;

/**
 * GatewayException <br>
 *
 * @date: 2022/1/1 <br>
 * @author: llxiao <br>
 * @since: 1.0 <br>
 * @version: 1.0 <br>
 */
public class GatewayException extends Exception {
    private Integer errorCode;
    private String errorMsg;

    public GatewayException(Integer errorCode, String errorMsg) {
        super(errorMsg);
        this.errorCode = errorCode;
        this.errorMsg = errorMsg;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public Integer getErrorCode() {
        return errorCode;
    }
}
