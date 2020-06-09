package com.example.myapplication.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.myapplication.R;
import com.example.myapplication.util.HttpsTrustManager;

import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CheckMainActivity extends AppCompatActivity {

    private static final String WEB_URL = "http://192.168.1.163:8080/examples/src/3DPrinting.html";

    private static final String FILE_UPLOAD_URL = "https://192.168.1.67:448/file/uploadFileAndGenGcode";

    private static final String FILE_DOWN_URL = "https://192.168.1.67:448/file/downloadFile?fileName=";

    private Map<String, String> stlAndGcodeMap = new HashMap<>();
    private String currentFile = null;

    private static final String APP_DOWN_PATH = Environment.getExternalStorageDirectory() + "/download/";
    private static final int WRITE_REQ = 9980;

    private static final int READ_REQ = 8870;

    private static boolean isReadPermissions = false;

    private static boolean isWritePermissions = false;

    private static final OkHttpClient clientLong = new OkHttpClient.Builder()
            .sslSocketFactory(HttpsTrustManager.createSSLSocketFactory(), new HttpsTrustManager())
            .hostnameVerifier(new HttpsTrustManager.TrustAllHostnameVerifier())
            .readTimeout(360, TimeUnit.SECONDS)
            .writeTimeout(360, TimeUnit.SECONDS)
            .connectTimeout(720, TimeUnit.SECONDS).build();


    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);// 隐藏状态栏
        Objects.requireNonNull(getSupportActionBar()).hide();// 隐藏标题栏

        //设置Activity横屏显示
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        //设置Activity竖屏显示
        // setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // setContentView(R.layout.check_main);

        /*setContentView(R.layout.index);
        WebView webView=(WebView)findViewById(R.id.children);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        //确保跳转到另一个网页时仍然在当前WebView显示
        webView.loadUrl(WEB_URL);*/

        setContentView(R.layout.index);
        // 拿到webView组件
        WebView webView = (WebView) findViewById(R.id.children);
        // 拿到webView的设置对象
        WebSettings settings = webView.getSettings();
        settings.setAppCacheEnabled(true); // 开启缓存
        settings.setJavaScriptEnabled(true); // 开启javascript支持
        // 加载url到webView中
        webView.loadUrl(WEB_URL);

        /*Intent intent = new Intent();
        intent.setAction("android.intent.action.VIEW");
        Uri content_url = Uri.parse(WEB_URL);
        intent.setData(content_url);
        startActivity(intent);*/

        Button clickBtn = findViewById(R.id.click_button);

        Button bigBtn = findViewById(R.id.big_button);

        Button litBtn = findViewById(R.id.lit_button);

        Button upBtn = findViewById(R.id.up_button);

        clickBtn.setOnClickListener(v -> webView.loadUrl("javascript:showModule()"));
        bigBtn.setOnClickListener(v -> webView.loadUrl("javascript:cameraSides(7)"));
        // litBtn.setOnClickListener(v -> webView.loadUrl("javascript:cameraSides(8)"));

        litBtn.setOnClickListener(v -> {
            String currentGcode = stlAndGcodeMap.get(currentFile);
            if (null != currentGcode && currentGcode.length() > 1) {
                Thread dwThread = new Thread(() -> {
                    String outFileName = currentFile.replace("stl", "gcode");
                    boolean b = downFromImgService(FILE_DOWN_URL + currentGcode.replace("stl", "gcode"), outFileName);
                    System.err.println(FILE_DOWN_URL + currentGcode.replace("stl", "gcode") + ",result:" + b);
                });
                dwThread.start();
            }
        });

        upBtn.setOnClickListener(v -> {
            Thread upThread = new Thread(() -> {
                currentFile = APP_DOWN_PATH + "123.stl";
                stlAndGcodeMap.put(currentFile, "");

                File stlFile = new File(currentFile);
                try {
                    String currentGcode = "";
                    JSONObject jsonObject = postFile(FILE_UPLOAD_URL, stlFile, new HashMap<>());
                    System.err.println(jsonObject);
                    if (null != jsonObject && 200 == jsonObject.getIntValue("code")) {
                        currentGcode = jsonObject.getString("data");
                    } else {
                        currentGcode = null;
                    }
                    stlAndGcodeMap.put(currentFile, currentGcode);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("CheckMain", "error:", e);
                }
            });
            upThread.start();
        });
    }


    public JSONObject postFile(String url, File file, Map<String, String> commandLineMap) throws IOException {

        checkIsPermission();
        if (isReadPermissions) {
            String multipartStr = "multipart/form-data";
            RequestBody formBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse(multipartStr), file))
                    .addFormDataPart("commandLineMap", commandLineMap.toString()).build();
            Request request = new Request.Builder().url(url).post(formBody).build();
            return executeCall(url, request);
        }
        return null;

    }

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
                    Thread.sleep(10000);
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

    public boolean downFromImgService(String imgUrl, String outfileName) {
        checkIsPermission();

        if (isWritePermissions) {
            File gcodeFile = new File(outfileName);
            InputStream inputStream = null;
            FileOutputStream fileOutputStream = null;
            ByteArrayOutputStream output = null;
            try {

                Request request = new Request.Builder().addHeader("Connection", "close").addHeader("Accept", "*/*")
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:0.9.4)").get().url(imgUrl).build();
                Response response = clientLong.newCall(request).execute();

                inputStream = response.body().byteStream();

                if (!gcodeFile.getParentFile().exists()) {
                    gcodeFile.getParentFile().mkdirs();// 返回此抽象路径名父目录的抽象路径名；创建
                } else if (gcodeFile.exists() && gcodeFile.isFile()) {
                    gcodeFile.delete();
                }


                fileOutputStream = new FileOutputStream(gcodeFile);
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
            gcodeFile = new File(outfileName);
            return gcodeFile.exists() && !gcodeFile.isDirectory();
        }
        return false;
    }

    /**
     * 检查全选
     */
    private void checkIsPermission() {
        //CameraDemoActivity 是activity的名字
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //有权限的情况
            isWritePermissions = true;
        } else {
            //没有权限，进行权限申请
            //REQ是本次请求的辨认编号,即 requestCode
            isWritePermissions = false;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQ);
        }

        //CameraDemoActivity 是activity的名字
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            //有权限的情况
            isReadPermissions = true;
        } else {
            //没有权限，进行权限申请
            //REQ是本次请求的辨认编号,即 requestCode
            isReadPermissions = false;
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_REQ);
        }
    }

    /***
     * 申请权限后的回调函数
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == WRITE_REQ) {
            if (null != grantResults && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //申请权限成功
                isWritePermissions = true;
            } else {
                //申请权限被拒绝
                isWritePermissions = false;
            }
        }

        if (requestCode == READ_REQ) {
            if (null != grantResults && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //申请权限成功
                isReadPermissions = true;
            } else {
                //申请权限被拒绝
                isReadPermissions = false;
            }
        }
    }

}
