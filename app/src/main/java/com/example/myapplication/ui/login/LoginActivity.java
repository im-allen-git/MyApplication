package com.example.myapplication.ui.login;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;

public class LoginActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }


    private int checkUserInfo(String email, String password) {

        int count = 0;

        /*// DBUtils.getInfoByEmail(email);
        HashMap<String, String> paramMap = new HashMap<>();
        paramMap.put("email", email);
        paramMap.put("pass_word", password);
        try {
            // get
            String url = OkHttpUtil.LOGIN_URL + "?email=" + email + "&pass_word=" + password;
            JSONObject jsonObject = OkHttpUtil.getInstance().callUrlByGet(url);

            // post
            // JSONObject jsonObject = OkHttpUtil.getInstance().callUrlByPost(OkHttpUtil.LOGIN_URL, paramMap);

            // JSONObject jsonObject = OkHttpUtil.getInstance().post(OkHttpUtil.LOGIN_URL, paramMap);
            if (null != jsonObject && jsonObject.containsKey("code") && 200 == jsonObject.getIntValue("code")) {
                count = jsonObject.getIntValue("total");
                UserInfo userInfo = jsonObject.getObject("data", UserInfo.class);
                System.err.println(userInfo);
            }
        } catch (IOException e) {
            e.printStackTrace();
            count = -1;
        }*/
        count = 1;
        return count;
    }
}
