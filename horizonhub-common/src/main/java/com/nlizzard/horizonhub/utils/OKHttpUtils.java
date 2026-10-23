package com.nlizzard.horizonhub.utils;

import com.nlizzard.horizonhub.entity.enums.ResponseCodeEnum;
import com.nlizzard.horizonhub.exception.BusinessException;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class OKHttpUtils {
    /**
     * 请求超时时间5秒
     */
    private static final int TIME_OUT_SECONDS = 5;

    private static final Logger logger = LoggerFactory.getLogger(OKHttpUtils.class);

    private static OkHttpClient.Builder getClientBuilder() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
                .followRedirects(false)
                .addInterceptor(new RedirectInterceptor())
                .retryOnConnectionFailure(false);

        clientBuilder.connectTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIME_OUT_SECONDS, TimeUnit.SECONDS);

        // JDK 9+ 必须同时传入 SSLSocketFactory 和 X509TrustManager
        X509TrustManager trustManager = new TrustAllCerts();
        clientBuilder.sslSocketFactory(createSSLSocketFactory(trustManager), trustManager);
        clientBuilder.hostnameVerifier((hostname, session) -> true);

        return clientBuilder;
    }

    private static Request.Builder getRequestBuilder(Map<String, String> header) {
        Request.Builder requestBuilder = new Request.Builder();
        if (null != header) {
            for (Map.Entry<String, String> map : header.entrySet()) {
                String key = map.getKey();
                String value = (map.getValue() == null) ? "" : map.getValue();
                requestBuilder.addHeader(key, value);
            }
        }
        return requestBuilder;
    }

    private static FormBody.Builder getBuilder(Map<String, String> params) {
        FormBody.Builder builder = new FormBody.Builder();
        if (params == null) {
            return builder;
        }
        for (Map.Entry<String, String> map : params.entrySet()) {
            String key = map.getKey();
            String value = (map.getValue() == null) ? "" : map.getValue();
            builder.add(key, value);
        }
        return builder;
    }

    public static String getRequest(String url) throws BusinessException {
        ResponseBody responseBody = null;
        try {
            OkHttpClient.Builder clientBuilder = getClientBuilder();
            Request.Builder requestBuilder = getRequestBuilder(null);
            OkHttpClient client = clientBuilder.build();
            Request request = requestBuilder.url(url).build();
            Response response = client.newCall(request).execute();
            responseBody = response.body();
            return responseBody != null ? responseBody.string() : null;
        } catch (SocketTimeoutException | ConnectException e) {
            logger.error("OKhttp GET 请求超时,url:{}", url, e);
            throw new BusinessException(ResponseCodeEnum.CODE_900);
        } catch (Exception e) {
            logger.error("OKhttp GET 请求异常", e);
            return null;
        } finally {
            if (responseBody != null) {
                responseBody.close();
            }
        }
    }

    public static String postRequest(String url, Map<String, String> header, Map<String, String> params) throws BusinessException {
        ResponseBody responseBody = null;
        try {
            OkHttpClient.Builder clientBuilder = getClientBuilder();
            Request.Builder requestBuilder = getRequestBuilder(header);
            FormBody.Builder builder = getBuilder(params);
            OkHttpClient client = clientBuilder.build();
            RequestBody requestBody = builder.build();
            Request request = requestBuilder.url(url).post(requestBody).build();
            Response response = client.newCall(request).execute();
            responseBody = response.body();
            return responseBody != null ? responseBody.string() : null;
        } catch (SocketTimeoutException | ConnectException e) {
            logger.error("OKhttp POST 请求超时,url:{}", url, e);
            throw new BusinessException(ResponseCodeEnum.CODE_900);
        } catch (Exception e) {
            logger.error("OKhttp POST 请求异常,url:{}", url, e);
            return null;
        } finally {
            if (responseBody != null) {
                responseBody.close();
            }
        }
    }

    private static SSLSocketFactory createSSLSocketFactory(X509TrustManager trustManager) {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{trustManager}, new SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            logger.error("创建 SSLSocketFactory 失败", e);
            return null;
        }
    }
}

class TrustAllCerts implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}

class RedirectInterceptor implements Interceptor {
    private static final Logger logger = LoggerFactory.getLogger(RedirectInterceptor.class);

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        int code = response.code();
        if (code == 307 || code == 301 || code == 302) {
            String location = response.headers().get("Location");
            logger.info("重定向地址,location:{}", location);
            if (location != null) {
                Request newRequest = request.newBuilder().url(location).build();
                response.close(); // 记得关闭上一个 response
                return chain.proceed(newRequest);
            }
        }
        return response;
    }
}
