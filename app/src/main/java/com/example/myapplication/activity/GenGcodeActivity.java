package com.example.myapplication.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.alibaba.fastjson.JSONObject;
import com.example.myapplication.R;
import com.example.myapplication.util.FileUtils;
import com.example.myapplication.util.HttpsTrustManager;
import com.example.myapplication.util.ProgressResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GenGcodeActivity extends AppCompatActivity {

    private static final String FILE_UPLOAD_URL = "https://192.168.1.67:448/file/uploadFileAndGenGcode";
    private static final String FILE_DOWN_URL = "https://192.168.1.67:448/file/downloadFile?fileName=";
    private Map<String, String> stlAndGcodeMap = new HashMap<>();
    private String currentFile = null;
    private static final String SUPPORTED_FILE_TYPES_REGEX = "(?i).*\\.(obj|stl|dae)";

    private static final String APP_DOWN_PATH = Environment.getExternalStorageDirectory() + "/download/";
    private static final int WRITE_REQ = 1001;
    private static final int READ_REQ = 1002;
    private static final int FILE_UPLOAD_CODE = 2000;
    private static boolean isReadPermissions = false;
    private static boolean isWritePermissions = false;

    private static final int UPLOAD_COMPLETED = 3001;
    private static final int UPLOAD_ERROR = 3002;
    private static final int DOWN_COMPLETED = 3010;
    private static final int DOWN_ERROR = 3012;

    private static final int UPLOAD_PROGRESS = 3030;
    private static final int DOWN_PROGRESS = 3040;


    private Button upBtn, downBtn, viewBtn;
    private ProgressBar upProgress, dwProgress;
    private TextView downText;
    private Map<String, Object> loadModelParameters = new HashMap<>();

    @SuppressLint("HandlerLeak")
    private Handler mainHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPLOAD_COMPLETED:
                    // 上传完成
                    downBtn.setVisibility(View.VISIBLE);
                    downText.setText(msg.obj.toString());
                    //upProgress.setVisibility(View.INVISIBLE);
                    break;
                case UPLOAD_ERROR:
                    // 上传失败
                    downBtn.setVisibility(View.INVISIBLE);
                    downText.setText(msg.obj.toString());
                    upProgress.setVisibility(View.INVISIBLE);
                    break;
                case DOWN_COMPLETED:
                    // 下载完成
                case DOWN_ERROR:
                    // 下载失败
                    downText.setText(msg.obj.toString());
                    dwProgress.setVisibility(View.INVISIBLE);
                    break;
                case UPLOAD_PROGRESS:
                    upProgress.setVisibility(View.VISIBLE);
                    upProgress.setProgress(Integer.parseInt(msg.obj.toString()));
                    break;
                case DOWN_PROGRESS:
                    dwProgress.setVisibility(View.VISIBLE);
                    dwProgress.setProgress(Integer.parseInt(msg.obj.toString()));
                    break;
            }


        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().hide();// 隐藏标题栏
        setContentView(R.layout.gen_gcode);

        upBtn = findViewById(R.id.up_button);

        downBtn = findViewById(R.id.down_button);
        //downBtn.setVisibility(View.INVISIBLE);

        downText = findViewById(R.id.down_text_show);
        //downText.setVisibility(View.INVISIBLE);

        upProgress = findViewById(R.id.progress_bar_up);
        upProgress.setVisibility(View.INVISIBLE);

        dwProgress = findViewById(R.id.progress_bar_dw);
        //dwProgress.setVisibility(View.INVISIBLE);

        viewBtn = findViewById(R.id.view_button);

        addUpBtnListener();
        addDwBtnListener();

        addViewBtnListener();
    }

    // 上传文件响应
    private void addUpBtnListener() {
        upBtn.setOnClickListener(v -> {
            // currentFile = APP_DOWN_PATH + "123.stl";
            //pickFile(upBtn);
            checkIsPermission();
            if (isReadPermissions && isWritePermissions) {
                FileUtils.createChooserDialog(this, "Select file", null, null, SUPPORTED_FILE_TYPES_REGEX,
                        (File file) -> {
                            if (file != null) {
                                currentFile = file.getAbsolutePath().replace("\\", "/");
                                doUpload();
                            }
                        });
            }
        });
    }


    // 下载文件响应
    private void addDwBtnListener() {
        downBtn.setOnClickListener(v -> {
            String currentGcode = stlAndGcodeMap.get(currentFile);
            if (null != currentGcode && currentGcode.length() > 1) {
                Thread dwThread = new Thread(() -> {
                    String outFileName = currentFile.replace("stl", "gcode");
                    downloadProgress(FILE_DOWN_URL + currentGcode.replace("stl", "gcode"), outFileName);
                });
                dwThread.start();
            }
        });
    }

    //预览文件响应
    private void addViewBtnListener() {
        viewBtn.setOnClickListener(v -> {
            checkIsPermission();
            if (isReadPermissions && isWritePermissions) {
                FileUtils.createChooserDialog(this, "Select file", null, null, SUPPORTED_FILE_TYPES_REGEX,
                        (File file) -> {
                            if (file != null && file.getName().endsWith("stl")) {

                                Thread myThread = new Thread(() -> {
                                    try {
                                        Intent it = new Intent(getApplicationContext(), ShowActivity.class);
                                        it.putExtra("uri", Uri.parse("file://" + file.getAbsolutePath()).toString());
                                        startActivity(it);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                });
                                myThread.start();
                            } else {
                                AlertDialog.Builder builder = new AlertDialog.Builder(GenGcodeActivity.this);
                                builder.setTitle("提示：");
                                builder.setMessage("非stl文件");
                                builder.setIcon(R.mipmap.ic_launcher);
                                builder.setCancelable(true);            //点击对话框以外的区域是否让对话框消失

                                //设置正面按钮
                                builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Toast.makeText(GenGcodeActivity.this, "你点击了确定", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                    }
                                });
                            }
                        });
            }
        });
    }


    private void doUpload() {
        downText.setVisibility(View.VISIBLE);
        if (null != currentFile && currentFile.length() > 1) {
            if (currentFile.endsWith(".stl")) {
                stlAndGcodeMap.put(currentFile, "");
                if (null != currentFile && currentFile.length() > 0) {
                    downText.setText("正在上传");
                    Thread upThread = new Thread(() -> {
                        File stlFile = new File(currentFile);
                        postProgress(FILE_UPLOAD_URL, stlFile, new HashMap<>());
                    });
                    upThread.start();
                }
            } else {
                sendMessage(UPLOAD_ERROR, "非.stl文件");
            }
        } else {
            sendMessage(UPLOAD_ERROR, "获取文件失败");
        }
    }

    // okhttps 上传文件
    public void postProgress(String url, File file, Map<String, String> commandLineMap) {
        checkIsPermission();
        if (isReadPermissions) {
            sendMessage(UPLOAD_PROGRESS, "0");
            String multipartStr = "multipart/form-data";
            RequestBody formBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(),
                            RequestBody.create(MediaType.parse(multipartStr), file))
                    .addFormDataPart("commandLineMap", commandLineMap.toString()).build();
            Request request = new Request.Builder().url(url).post(formBody).build();

            //构建我们的进度监听器
            final ProgressResponseBody.ProgressListener listener = (bytesRead, contentLength, done) -> {
                //计算百分比并更新ProgressBar
                final int percent = (int) (100 * bytesRead / contentLength);
                sendMessage(UPLOAD_PROGRESS, String.valueOf(percent));
                System.out.println("上传进度：" + (100 * bytesRead) / contentLength + "%");
            };

            //创建一个OkHttpClient，并添加网络拦截器
            OkHttpClient client = new OkHttpClient.Builder()
                    .sslSocketFactory(HttpsTrustManager.createSSLSocketFactory(), new HttpsTrustManager())
                    .hostnameVerifier(new HttpsTrustManager.TrustAllHostnameVerifier())
                    .readTimeout(600, TimeUnit.SECONDS)
                    .writeTimeout(600, TimeUnit.SECONDS)
                    .connectTimeout(1200, TimeUnit.SECONDS)
                    .addNetworkInterceptor(chain -> {
                        Response response = chain.proceed(chain.request());
                        //这里将ResponseBody包装成我们的ProgressResponseBody
                        return response.newBuilder()
                                .body(new ProgressResponseBody(response.body(), listener))
                                .build();
                    }).build();

            //发送响应
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    sendMessage(UPLOAD_ERROR, "上传失败，请重试");
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        JSONObject jsonObject = JSONObject.parseObject(response.body().string());
                        System.err.println(jsonObject);
                        String currentGcode = "";
                        if (null != jsonObject && 200 == jsonObject.getIntValue("code")) {
                            currentGcode = jsonObject.getString("data");
                            sendMessage(UPLOAD_COMPLETED, currentGcode);
                        } else {
                            currentGcode = null;
                            sendMessage(UPLOAD_ERROR, "上传失败，请重试");
                        }
                        stlAndGcodeMap.put(currentFile, currentGcode);
                    }
                }
            });
        }
    }


    private void sendMessage(int what, String message) {
        Message msg = new Message();
        msg.what = what;
        msg.obj = message;
        mainHandler.sendMessage(msg);
    }


    private void downloadProgress(String url, String outfileName) {

        checkIsPermission();
        if (isWritePermissions) {
            sendMessage(DOWN_PROGRESS, "0");
            try {
                //构建一个请求
                Request request = new Request.Builder().addHeader("Connection", "close")
                        .addHeader("Accept", "*/*")
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:0.9.4)")
                        .get().url(url).build();

                //构建我们的进度监听器
                final ProgressResponseBody.ProgressListener listener = (bytesRead, contentLength, done) -> {
                    //计算百分比并更新ProgressBar
                    final int percent = (int) (100 * bytesRead / contentLength);
                    sendMessage(DOWN_PROGRESS, String.valueOf(percent));
                    System.out.println("下载进度：" + (100 * bytesRead) / contentLength + "%");
                };
                //创建一个OkHttpClient，并添加网络拦截器
                OkHttpClient client = new OkHttpClient.Builder()
                        .sslSocketFactory(HttpsTrustManager.createSSLSocketFactory(), new HttpsTrustManager())
                        .hostnameVerifier(new HttpsTrustManager.TrustAllHostnameVerifier())
                        .readTimeout(600, TimeUnit.SECONDS)
                        .writeTimeout(600, TimeUnit.SECONDS)
                        .connectTimeout(1200, TimeUnit.SECONDS)
                        .addNetworkInterceptor(chain -> {
                            Response response = chain.proceed(chain.request());
                            //这里将ResponseBody包装成我们的ProgressResponseBody
                            return response.newBuilder()
                                    .body(new ProgressResponseBody(response.body(), listener))
                                    .build();
                        }).build();
                //发送响应
                Call call = client.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        sendMessage(DOWN_ERROR, "下载失败，请重试");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        if (response.isSuccessful()) {
                            //从响应体读取字节流
                            File gcodeFile = new File(outfileName);
                            writeToFile(response, gcodeFile);
                            sendMessage(DOWN_COMPLETED, "下载成功");
                        } else {
                            sendMessage(DOWN_ERROR, "下载失败，请重试");
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                System.err.println("execute download[ " + url + "] error:" + e.getMessage());
                Log.e("downFile", "error:", e);
            }
        }
    }

    private void writeToFile(Response response, File gcodeFile) {
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        ByteArrayOutputStream output = null;

        try {
            inputStream = response.body().byteStream();
            fileOutputStream = new FileOutputStream(gcodeFile);
            output = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int length;

            while ((length = inputStream.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }
            fileOutputStream.write(output.toByteArray());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
        File tempFile = new File(gcodeFile.getAbsolutePath());
        boolean b = tempFile.exists() && !tempFile.isDirectory();
        sendMessage(DOWN_COMPLETED, "下载成功," + gcodeFile.getName());
        System.err.println(gcodeFile.getAbsolutePath() + ",result:" + b);
    }

    /**
     * 检查读取和写入权限
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
