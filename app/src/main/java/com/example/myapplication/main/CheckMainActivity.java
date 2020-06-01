package com.example.myapplication.main;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

import java.util.Objects;

public class CheckMainActivity extends AppCompatActivity {

    private String WEB_URL = "http://192.168.1.163:8080/examples/src/3DPrinting.html";


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

        clickBtn.setOnClickListener(v -> webView.loadUrl("javascript:showModule()"));
        bigBtn.setOnClickListener(v -> webView.loadUrl("javascript:cameraSides(7)"));
        litBtn.setOnClickListener(v -> webView.loadUrl("javascript:cameraSides(8)"));
    }
}
