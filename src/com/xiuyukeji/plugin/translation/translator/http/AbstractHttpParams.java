package com.xiuyukeji.plugin.translation.translator.http;

import java.util.HashMap;
import java.util.Map;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

/**
 * 请求参数基类
 *
 * @author Created by jz on 2017/10/24 14:48
 */
abstract class AbstractHttpParams implements HttpParams {
    final Map<String, String> params = new HashMap<>();
    
    @Override
    public String send2String(String baseUrl) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            CloseableHttpResponse response = send(httpClient, baseUrl);
            return EntityUtils.toString(response.getEntity());
        }
    }
    
    @Override
    public HttpParams put(String key, String value) {
        params.put(key, value);
        return this;
    }
    
    /**
     * 发送请求
     *
     * @param httpClient
     * @param baseUrl
     * @return
     * @throws Exception
     */
    protected abstract CloseableHttpResponse send(CloseableHttpClient httpClient, String baseUrl) throws Exception;
}
