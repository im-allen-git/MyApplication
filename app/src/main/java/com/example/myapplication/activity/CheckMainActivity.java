package com.example.myapplication.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public class CheckMainActivity extends AppCompatActivity {

    private static final String WEB_URL = "http://192.168.1.163:8080/examples/src/3DPrinting.html";
    // private static final String WEB_URL = "http://192.168.1.66:8080/index.html";

    // private static final String WEB_URL = "https://192.168.1.67:448/index.html";


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
        initView();
    }


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @SuppressLint("SetJavaScriptEnabled")
    private void initView() {

        setContentView(R.layout.index);
        // 拿到webView组件
        WebView webView = (WebView) findViewById(R.id.children);

        // 拿到webView的设置对象
        WebSettings settings = webView.getSettings();
        settings.setAppCacheEnabled(true); // 开启缓存
        settings.setJavaScriptEnabled(true); // 开启javascript支持

        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setBlockNetworkImage(true);
        settings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        // settings.setAppCacheEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
        settings.setEnableSmoothTransition(true);


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
        clickBtn.setOnClickListener(v -> webView.loadUrl("javascript:showModule()"));
        bigBtn.setOnClickListener(v -> webView.loadUrl("javascript:cameraSides(7)"));


        litBtn.setOnClickListener(v -> {
            webView.loadUrl("javascript:cameraSides(8)");
            webView.evaluateJavascript("javascript:callJS()", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    //此处为 js 返回的结果
                }
            });
        });

        Button jumpBtn = findViewById(R.id.jump_button);
        jumpBtn.setOnClickListener(v -> {
            Thread myThread = new Thread(() -> {
                try {
                    Intent it = new Intent(getApplicationContext(), GenGcodeActivity.class);
                    startActivity(it);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            myThread.start();
        });


        // 复写WebViewClient类的shouldOverrideUrlLoading方法
        webView.setWebViewClient(new MyWebViewClient());

        /*litBtn.setOnClickListener(v -> {

            // 使用
            *//*DownloadCompleteReceiver receiver = new DownloadCompleteReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            registerReceiver(receiver, intentFilter);
*//*
            downloadBySystem("http://192.168.1.66:8080/123.stl", "",".stl");
        });*/
    }


    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            // 步骤2：根据协议的参数，判断是否是所需要的url
            // 一般根据scheme（协议格式） & authority（协议名）判断（前两个参数）
            //假定传入进来的 url = "js://webview?arg1=111&arg2=222"（同时也是约定好的需要拦截的）

            Uri uri = Uri.parse(url);
            // 如果url的协议 = 预先约定的 js 协议
            // 就解析往下解析参数
            if (uri.getScheme().equals("js")) {

                // 如果 authority  = 预先约定协议里的 webview，即代表都符合约定的协议
                // 所以拦截url,下面JS开始调用Android需要的方法
                if (uri.getAuthority().equals("webview")) {
                    //  步骤3：
                    // 执行JS所需要调用的逻辑
                    System.out.println("js调用了Android的方法");
                    // 可以在协议上带有参数并传递到Android上
                    String paramUrl = uri.getQueryParameter("url");
                    System.err.println(paramUrl);
                }
                return true;
            }
            return super.shouldOverrideUrlLoading(view, url);
        }
    }

}
