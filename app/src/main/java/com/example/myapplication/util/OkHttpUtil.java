package com.example.myapplication.util;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.myapplication.pojo.UserInfo;
import com.google.gson.Gson;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class OkHttpUtil {
    public static final MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

    private final static String SERVICE_URL = "https://192.168.1.67:448";

    public final static String LOGIN_URL = SERVICE_URL + "/user/login";


    private static OkHttpClient client = new OkHttpClient.Builder()
            .sslSocketFactory(HttpsTrustManager.createSSLSocketFactory(), new HttpsTrustManager())
            .hostnameVerifier(new HttpsTrustManager.TrustAllHostnameVerifier())
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS).build();

    private static OkHttpClient clientLong = new OkHttpClient.Builder()
            .sslSocketFactory(HttpsTrustManager.createSSLSocketFactory(), new HttpsTrustManager())
            .hostnameVerifier(new HttpsTrustManager.TrustAllHostnameVerifier())
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS).build();

    private volatile static OkHttpUtil singleton = null;

    /*
     * 私有化构造方法
     * 使用单例模式
     * */
    public OkHttpUtil() {
    }

    /**
     * getInstance
     *
     * @return
     */
    public static OkHttpUtil getInstance() {

        if (singleton == null) {
            synchronized (OkHttpUtil.class) {
                if (singleton == null) {
                    singleton = new OkHttpUtil();
                }
            }
        }
        return singleton;
    }


    /**
     * get调用（有重试机制，默认15次重试，每次1秒）
     *
     * @param url
     * @return
     * @throws IOException
     */
    public JSONObject callUrlByGet(String url) throws IOException {

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return executeCall(url, request);
    }


    /**
     * Post调用
     *
     * @param url
     * @return
     * @throws IOException
     */
    public JSONObject callUrlByPost(String url, Object param) throws IOException {
        String param_ = JSONObject.toJSONString(param);
        RequestBody requestBody = RequestBody.create(param_, mediaType);
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        return executeCall(url, request);
    }

    public JSONObject post(String url , Map<String,String> param) throws IOException {

         UserInfo user = new UserInfo();
         user.setEmail(param.get("email"));
         user.setPass_word(param.get("pass_word"));


        MediaType MEDIA_TYPE_JSON= MediaType.parse("application/json; charset=utf-8");
         RequestBody requestBody = FormBody.create(MediaType.parse("application/json; charset=utf-8"), JSONObject.toJSONString(user));

        /*FormBody.Builder builder = new FormBody.Builder();

        RequestBody requestBody = builder.build();
        for (Map.Entry<String, String> entry : param.entrySet()) {
            builder.add(entry.getKey(), entry.getValue());
        }*/

        Request request = new Request.Builder()
                .url(url)//请求的url
                .post(requestBody)
                .build();
        return executeCall(url, request);

    }

    /**
     * call url by retry times
     *
     * @param url
     * @param request
     * @return
     * @throws IOException
     */
    @Nullable
    private JSONObject executeCall(String url, Request request) throws IOException {
        Response response = null;
        System.err.println(url);
        try {
            response = clientLong.newCall(request).execute();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            //重试15次（每次1秒）
            try {
                int count = 0;
                while (true) {
                    Thread.sleep(1000);
                    try {
                        response = clientLong.newCall(request).execute();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (count > 2) {
                        break;
                    }
                    ++count;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (response == null || !response.isSuccessful()) {
            System.err.println("url:[{" + url + "}]");
            throw new IOException("call url is not successful");
        }

        return response.body() != null ?
                JSON.parseObject(response.body().string()) : null;
    }

}