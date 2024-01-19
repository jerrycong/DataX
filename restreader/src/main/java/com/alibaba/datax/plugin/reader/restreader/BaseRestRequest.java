package com.alibaba.datax.plugin.reader.restreader;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.alibaba.datax.common.element.*;
import com.alibaba.datax.common.exception.CommonErrorCode;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.unstructuredstorage.reader.ColumnEntry;
import com.alibaba.datax.plugin.unstructuredstorage.reader.Key;
import com.alibaba.datax.plugin.unstructuredstorage.reader.UnstructuredStorageReaderUtil;
import com.alibaba.fastjson2.JSON;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @Description rest请求基类
 * @Author cyj
 * @Date 2024-1-15 19:26
 * @Version 1.0
 */
public abstract class BaseRestRequest implements RestRequest {

    private static final Logger LOG = LoggerFactory.getLogger(BaseRestRequest.class);

    protected Configuration configuration;

    // rest请求参数
    protected String url;
    protected String method;  //get, post
    protected int timeout;  //超时时间，单位秒，默认10秒
    protected String body;  //请求体内容
    protected String dataMode; //数据格式：multiData 多条数据，返回为数组  oneData 单条数据，返回为Map
    protected String dataPath;
    /**
     * 自定义的header
     */
    protected Map<String, String> customHeader;
    /**
     * url参数，如pageNo=1&pageIndex=0&pageSize=10&timeRangeType=0&doorId=，不需要带问号
     */
    protected String parameters;

    /**
     * 是否有分页
     */
    protected Boolean pagination;
    /**
     * 总数变量路径，如result.totalElements
     */
    protected String totalParamPath;
    /**
     * 起始页变量名
     */
    protected String startIndexParam;
    /**
     * 分页变量名
     */
    protected String pageSizeParam;
    /**
     * 页码变量名
     */
    protected String pageNoParam;

    /**
     * 总页码
     */
    protected int totalPages;

    protected void init(){
        this.url = this.configuration.getString("url");
        this.method = this.configuration.getString("method", "GET").toUpperCase();
        this.timeout = this.configuration.getInt("timeout", 10);
        this.customHeader = this.configuration.getMap("customHeader", String.class);
        this.body = this.configuration.getString("body");
        this.dataPath = this.configuration.getString("dataPath", "");
        this.dataMode = this.configuration.getString("dataMode", "multiData");
        this.pagination = this.configuration.getBool("pagination", false);
        this.totalParamPath = this.configuration.getString("totalParamPath");
        this.startIndexParam = this.configuration.getString("startIndexParam", "pageIndex");
        this.pageSizeParam = this.configuration.getString("pageSizeParam","pageSize");
        this.pageNoParam = this.configuration.getString("pageNoParam", "pageNo");
    }

    @Override
    public String sendHttpRequest(){
        String url = this.url;
        if(StringUtils.isNotBlank(this.parameters)){
            url += "?" + this.parameters;
        }
        HttpRequest request = HttpUtil.createRequest(this.getMethod(), url);
        request.setReadTimeout(this.timeout * 1000);
        if(this.customHeader != null){
            this.customHeader.forEach((key, value) -> {
                request.header(key, value);
            });
        }
        if(StringUtils.isNotBlank(this.body)){
            request.body(body);
        }
        HttpResponse response = request.execute();
        LOG.debug("sendHttpRequest url: {}, response : {}", url, response);
        if(response.getStatus() == 200){
            return response.body();
        }
        throw DataXException.asDataXException(RestErrorCode.STATUS_ERROR, "响应状态码非200: " + response.getStatus());
    }

    @Override
    public void calculateTotalPages(String resp){
        Configuration jsonData = Configuration.from(resp);
        Integer totalRecords = jsonData.getInt(this.totalParamPath);

        int pageSize = getPageSize();

        LOG.info("totalRecords: {}, pageSize: {}", totalRecords, pageSize);

        if(totalRecords % pageSize == 0){
            this.totalPages = totalRecords / pageSize;
        } else {
            this.totalPages = totalRecords / pageSize + 1;
        }

    }

    @Override
    public int getTotalPages() {
        return this.totalPages;
    }

    @Override
    public void send2Writer(RecordSender recordSender, String jsonResp){
        //默认是数组格式
        Configuration jsonData = Configuration.from(jsonResp);
//        LOG.info("jsonData: {}", jsonData);
        List<Object> dataList = jsonData.getList(this.dataPath);

        List<ColumnEntry> columns = UnstructuredStorageReaderUtil.getListColumnEntry(this.configuration, Key.COLUMN);
        if(CollectionUtils.isNotEmpty(dataList)){
            for (Object object : dataList) {
                String singleDataJson = JSON.toJSONString(object);
                Record record = recordSender.createRecord();
                for (int i = 0; i < columns.size(); i++) {
                    Configuration singleDataConfiguration = Configuration.from(singleDataJson);

                    ColumnEntry columnEntry = columns.get(i);
                    Column column = getColumn(columnEntry, singleDataConfiguration);
                    if (column != null) {
                        record.setColumn(i, column);
                    }
                }
                recordSender.sendToWriter(record);
                recordSender.flush();
            }
        }

    }

    @Override
    public void validate() {
        List<ColumnEntry> columns = UnstructuredStorageReaderUtil.getListColumnEntry(this.configuration, Key.COLUMN);
        if (null == columns || columns.size() == 0) {
            throw DataXException.asDataXException(
                    CommonErrorCode.CONFIG_ERROR,
                    "column属性必填");
        }
        if(this.pagination){
            if(StringUtils.isBlank(this.totalParamPath)){
                throw DataXException.asDataXException(CommonErrorCode.CONFIG_ERROR, "totalParamPath属性必填");
            }
            if(StringUtils.isBlank(this.startIndexParam)){
                throw DataXException.asDataXException(CommonErrorCode.CONFIG_ERROR, "startIndexParam属性必填");
            }
            if(StringUtils.isBlank(this.pageSizeParam)){
                throw DataXException.asDataXException(CommonErrorCode.CONFIG_ERROR, "pageSizeParam属性必填");
            }
        }
    }

    protected Column getColumn(ColumnEntry columnEntry, Configuration singleDataConf){
        String type = columnEntry.getType();
        switch (type.toUpperCase()) {
            case "STRING":
                return new StringColumn(singleDataConf.getString(columnEntry.getValue()));
            case "LONG":
                return new LongColumn(singleDataConf.getLong(columnEntry.getValue()));
            case "BOOL":
                return new BoolColumn(singleDataConf.getBool(columnEntry.getValue()));
            default:
                return null;
        }
    }

}
