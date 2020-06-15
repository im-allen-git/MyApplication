package com.example.myapplication.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.ImageView;

import com.example.myapplication.R;
import com.example.myapplication.activity.CheckMainActivity;


public class WelcomeActivity extends Activity {

    private ImageView ivTuPian;

    @SuppressLint({"ResourceType", "WrongViewCast"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);// 隐藏状态栏
        // getSupportActionBar().hide();// 隐藏标题栏
        setContentView(R.layout.activity_welcome);

        ivTuPian = findViewById(R.id.welcome_img);
        initView();

        Thread myThread = new Thread() {
            @Override
            public void run() {
                try {
                    sleep(2000);
                    Intent it = new Intent(getApplicationContext(), CheckMainActivity.class);
                    startActivity(it);
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        myThread.start();
    }

    private void initView() {
        Bitmap bitmap = setTextToImg("欢迎使用儿童体态检测APP");
        ivTuPian.setImageBitmap(bitmap);
    }

    /**
     * 文字绘制在图片上，并返回bitmap对象
     */
    private Bitmap setTextToImg(String text) {
        BitmapDrawable icon = (BitmapDrawable) getResources().getDrawable(R.drawable.child);

        Bitmap bitmap = icon.getBitmap().copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        // 抗锯齿
        paint.setAntiAlias(true);
        // 防抖动
        paint.setDither(true);
        paint.setTextSize(28);
        paint.setColor(Color.parseColor("black"));
        canvas.drawText(text, (bitmap.getWidth() / 5), (bitmap.getHeight() / 5 * 1), paint);
        return bitmap;
    }
}
