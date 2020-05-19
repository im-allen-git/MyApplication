package com.example.myapplication.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

import com.alibaba.fastjson.JSONObject;
import com.example.myapplication.R;
import com.example.myapplication.main.CheckMainActivity;
import com.example.myapplication.pojo.UserInfo;
import com.example.myapplication.util.OkHttpUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LoginActivity extends AppCompatActivity {

    private LoginViewModel loginViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        loginViewModel = ViewModelProviders.of(this, new LoginViewModelFactory())
                .get(LoginViewModel.class);

        final EditText usernameEditText = findViewById(R.id.username);
        final EditText passwordEditText = findViewById(R.id.password);
        final Button loginButton = findViewById(R.id.login);
        final ProgressBar loadingProgressBar = findViewById(R.id.loading);

        final Button registerButton = findViewById(R.id.register_button);
        final Button forgetButton = findViewById(R.id.forget_button);
        final RadioButton acceptButton = findViewById(R.id.accept_btn);



        registerButton.setOnClickListener(v -> new Thread(() -> {
            Intent it = new Intent(getApplicationContext(), RegisterActivity.class);
            startActivity(it);
            finish();
        }).start());


        forgetButton.setOnClickListener(v -> new Thread(() -> {
            Intent it = new Intent(getApplicationContext(), ForgetActivity.class);
            startActivity(it);
            finish();
        }).start());

        AtomicInteger isCheck = new AtomicInteger();
        acceptButton.setOnClickListener(v -> {
            if(acceptButton.isChecked()){
                isCheck.set(1);
            } else {
                isCheck.set(0);
            }
            System.err.println("isCheck:" + isCheck.get());
        });


        loginViewModel.getLoginFormState().observe(this, loginFormState -> {
            if (loginFormState == null) {
                return;
            }
            loginButton.setEnabled(loginFormState.isDataValid());
            if (loginFormState.getUsernameError() != null) {
                usernameEditText.setError(getString(loginFormState.getUsernameError()));
            }
            if (loginFormState.getPasswordError() != null) {
                passwordEditText.setError(getString(loginFormState.getPasswordError()));
            }
        });

        loginViewModel.getLoginResult().observe(this, loginResult -> {
            if (loginResult == null) {
                return;
            }
            loadingProgressBar.setVisibility(View.GONE);
            if (loginResult.getError() != null) {
                showLoginFailed(loginResult.getError());
            }
            if (loginResult.getSuccess() != null) {
                updateUiWithUser(loginResult.getSuccess());
            }


            Thread myThread = new Thread() {
                @Override
                public void run() {
                    if (checkUserInfo(usernameEditText.getText().toString(), passwordEditText.getText().toString()) > 0) {
                        Intent it = new Intent(getApplicationContext(), CheckMainActivity.class);

                        startActivity(it);
                        finish();
                    }
                }
            };

            myThread.start();
            // setResult(Activity.RESULT_OK);
            //Complete and destroy login activity once successful
            // finish();
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {

            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login(usernameEditText.getText().toString(),
                            passwordEditText.getText().toString());
                }
                return false;
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                loginViewModel.login(usernameEditText.getText().toString(),
                        passwordEditText.getText().toString());
            }
        });
    }

    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }


    private int checkUserInfo(String email, String password) {

        int count = 0;

        // DBUtils.getInfoByEmail(email);
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
        }
        // count = 1;
        return count;
    }
}
