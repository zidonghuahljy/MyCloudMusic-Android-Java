package com.ixuea.courses.mymusic.activity;

import android.os.Bundle;
import android.widget.EditText;

import com.ixuea.courses.mymusic.AppContent;
import com.ixuea.courses.mymusic.MainActivity;
import com.ixuea.courses.mymusic.R;
import com.ixuea.courses.mymusic.api.Api;
import com.ixuea.courses.mymusic.domain.Session;
import com.ixuea.courses.mymusic.domain.User;
import com.ixuea.courses.mymusic.domain.response.DetailResponse;
import com.ixuea.courses.mymusic.listener.HttpObserver;
import com.ixuea.courses.mymusic.util.StringUtil;
import com.ixuea.courses.mymusic.util.ToastUtil;

import org.apache.commons.lang3.StringUtils;

public class LoginActivity extends BaseLoginActivity {

    EditText et_username;
    EditText et_pwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    @Override
    protected void initViews() {
        super.initViews();
        et_username = findViewById(R.id.et_username);
        et_pwd = findViewById(R.id.et_password);
        findViewById(R.id.btn_login).setOnClickListener(v -> onLoginClick());
        findViewById(R.id.btn_forget).setOnClickListener(v -> onForgetClick());
    }

    public void onLoginClick(){
        String userName = et_username.getText().toString().trim();
        if(StringUtils.isBlank(userName)) {
            ToastUtil.errorLongToast(R.string.error_username);
            return;
        }
        String pwd = et_pwd.getText().toString().trim();
        if(StringUtils.isBlank(pwd)) {
            ToastUtil.errorLongToast(R.string.error_pwd);
            return;
        }

        boolean isP = StringUtil.isPhone(userName);
        boolean isE = StringUtil.isEmail(userName);
//        LogUtil.d("eee","isPhone:" + isP + ",isEmail:" + isE);
        if(isP && isE){
            ToastUtil.errorShortToast(R.string.login_username_formate);
            return;
        }

        //判断密码长度的格式
        if (!StringUtil.isPassword(pwd)) {
            ToastUtil.errorShortToast(R.string.error_password_format);
            return;
        }

//        ToastUtil.shortSuccessToast(R.string.login_success);
        //判断是手机号还是邮箱
        String phone = null;
        String email = null;

        if (StringUtil.isPhone(userName)) {
            //手机号
            phone = userName;
        } else {
            //邮箱
            email = userName;
        }

        User user = new User();

//这里虽然同时传递了手机号和邮箱
//但服务端登录的时候有先后顺序
        user.setPhone(phone);
        user.setEmail(email);
        user.setPassword(pwd);
        //调用登录接口
       login(user);
    }

    //忘记密码点击事件
    public void onForgetClick(){

    }

}
