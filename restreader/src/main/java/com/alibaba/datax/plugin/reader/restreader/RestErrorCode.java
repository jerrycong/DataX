package com.alibaba.datax.plugin.reader.restreader;

import com.alibaba.datax.common.exception.CommonErrorCode;
import com.alibaba.datax.common.spi.ErrorCode;

/**
 * @Description TODO
 * @Author cyj
 * @Date 2024-1-15 15:57
 * @Version 1.0
 */
public enum RestErrorCode implements ErrorCode {
    STATUS_ERROR("status-not-200", "http请求响应不是200");

    private final String code;

    private final String describe;

    private RestErrorCode(String code, String describe) {
        this.code = code;
        this.describe = describe;
    }

    @Override
    public String getCode() {
        return this.code;
    }

    @Override
    public String getDescription() {
        return this.describe;
    }

    @Override
    public String toString() {
        return String.format("Code:[%s], Describe:[%s]", this.code,
                this.describe);
    }
}
