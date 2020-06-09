package com.example.myapplication.util;

import android.app.Activity;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.myapplication.pojo.UserInfo;

import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class OkHttpUtil extends Activity {
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
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .connectTimeout(180, TimeUnit.SECONDS).build();

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


    /**
     * @param url            :访问链接
     * @param commandLineMap : 命令集合
     * @return
     * @throws IOException
     */
    public JSONObject postFile(String url, File file, Map<String, String> commandLineMap) throws IOException {

        String multipartStr = "multipart/form-data";
        RequestBody formBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MediaType.parse(multipartStr), file))
                .addFormDataPart("commandLineMap", commandLineMap.toString()).build();
        Request request = new Request.Builder().url(url).post(formBody).build();
        return executeCall(url, request);

    }


    public JSONObject post(String url, Map<String, String> param) throws IOException {

        UserInfo user = new UserInfo();
        user.setEmail(param.get("email"));
        user.setPass_word(param.get("pass_word"));


        MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json; charset=utf-8");
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


    public boolean downFromImgService(String imgUrl, String fileName) {
        File file = new File(fileName);
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        ByteArrayOutputStream output = null;
        try {

            Request request = new Request.Builder().addHeader("Connection", "close").addHeader("Accept", "*/*")
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:0.9.4)").get().url(imgUrl).build();
            Response response = clientLong.newCall(request).execute();

            inputStream = response.body().byteStream();

            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();// 返回此抽象路径名父目录的抽象路径名；创建
            } else if (file.exists() && file.isFile()) {
                file.delete();
            }

            fileOutputStream = new FileOutputStream(file);
            output = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            fileOutputStream.write(output.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("execute download[ " + imgUrl + "] error:" + e.getMessage());
            Log.e("downFromImgService", "error:", e);
            return false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        file = new File(fileName);
        return file.exists() && !file.isDirectory();
    }


}