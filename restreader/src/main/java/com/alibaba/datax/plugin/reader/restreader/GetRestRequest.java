package com.alibaba.datax.plugin.reader.restreader;

import cn.hutool.http.Method;
import com.alibaba.datax.common.exception.CommonErrorCode;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.util.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description get请求
 * @Author cyj
 * @Date 2024-1-15 16:57
 * @Version 1.0
 */
public class GetRestRequest extends BaseRestRequest {
    private static final Logger LOG = LoggerFactory.getLogger(GetRestRequest.class);

    public GetRestRequest(Configuration configuration) {
        this.configuration = configuration;
        init();
    }


    @Override
    public int getPageSize(){
        String pageSizeParamRegex = this.pageSizeParam + "=" + "(\\d+)";
        Pattern pattern = Pattern.compile(pageSizeParamRegex);
        Matcher matcher = pattern.matcher(this.url);

        if (matcher.find()) {
            String[] pageSizeArrs = matcher.group().split("=");
            return Integer.parseInt(pageSizeArrs[1]);
        }
        throw DataXException.asDataXException(CommonErrorCode.CONFIG_ERROR, "每页记录数不存在");
    }

    @Override
    public int getPageIndex(){
        String startIndexRegex = this.startIndexParam + "=" + "(\\d+)";

        Pattern pattern = Pattern.compile(startIndexRegex);
        Matcher matcher = pattern.matcher(this.url);

        if (matcher.find()) {
            String[] pageIndexArrs = matcher.group().split("=");
            LOG.info("getPageIndex: {}", pageIndexArrs[1]);
            return Integer.parseInt(pageIndexArrs[1]);
        }
        return 0;
    }

    @Override
    public Method getMethod() {
        return Method.GET;
    }


}
