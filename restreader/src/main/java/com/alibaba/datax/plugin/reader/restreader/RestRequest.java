package com.alibaba.datax.plugin.reader.restreader;

import cn.hutool.http.Method;
import com.alibaba.datax.common.plugin.RecordSender;

/**
 * @Description TODO
 * @Author cyj
 * @Date 2024-1-15 16:55
 * @Version 1.0
 */
public interface RestRequest {

    /**
     * 发送http请求
     * @return
     */
    String sendHttpRequest();

    void calculateTotalPages(String resp);

    /**
     * 分页总页码
     * @return
     */
    int getTotalPages();

    int getPageSize();

    int getPageIndex();

    void validate();

    /**
     * 向writer写数据
     * @param recordSender
     * @param jsonResp
     */
    void send2Writer(RecordSender recordSender, String jsonResp);

    Method getMethod();
}
