package com.ixuea.courses.mymusic.activity;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import com.ixuea.courses.mymusic.R;
import com.ixuea.courses.mymusic.api.Api;
import com.ixuea.courses.mymusic.domain.BaseMode;
import com.ixuea.courses.mymusic.domain.User;
import com.ixuea.courses.mymusic.domain.response.DetailResponse;
import com.ixuea.courses.mymusic.listener.HttpObserver;
import com.ixuea.courses.mymusic.util.LogUtil;
import com.ixuea.courses.mymusic.util.StringUtil;
import com.ixuea.courses.mymusic.util.ToastUtil;

import org.apache.commons.lang3.StringUtils;

public class RegisterActivity extends BaseLoginActivity {

    EditText et_name;
    EditText et_email;
    EditText et_phone;
    EditText et_pwd;
    EditText et_alignPwd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
    }

    @Override
    protected void initViews() {
        super.initViews();
        et_name = findViewById(R.id.et_username);
        et_email = findViewById(R.id.et_email);
        et_phone = findViewById(R.id.et_phone);
        et_pwd = findViewById(R.id.et_pwd);
        et_alignPwd = findViewById(R.id.et_alignPwd);
        findViewById(R.id.btn_register).setOnClickListener(v -> onClickRegister());
    }

    public void onClickRegister(){
        String name = et_name.getText().toString().trim();
        String email = et_email.getText().toString().trim();
        String phone = et_phone.getText().toString().trim();
        String pwd = et_pwd.getText().toString().trim();
        String pwdAlign = et_alignPwd.getText().toString().trim();
        if(StringUtils.isBlank(name)){
            ToastUtil.errorShortToast("请输入昵称!");
            return;
        }
        if(StringUtils.isBlank(email)){
            ToastUtil.errorShortToast("请输入邮箱!");
            return;
        }
        if(StringUtils.isBlank(phone)){
            ToastUtil.errorShortToast("请输入电话号码!");
            return;
        }
        if(StringUtils.isBlank(pwd)){
            ToastUtil.errorShortToast("请输入密码!");
            return;
        }
        if(StringUtils.isBlank(pwdAlign)){
            ToastUtil.errorShortToast("请输入确认密码!");
            return;
        }
        if(!StringUtil.isPhone(phone)){
            ToastUtil.errorShortToast("手机号格式有误!");
            return;
        }
        if(!StringUtil.isEmail(email)){
            ToastUtil.errorShortToast("邮箱格式有误!");
            return;
        }
        if(!StringUtil.isNickname(pwd)){
            ToastUtil.errorShortToast("请输入2~10位昵称!");
            return;
        }
        if(!StringUtil.isPassword(pwd)){
            ToastUtil.errorShortToast("请输入6~16位密码!");
            return;
        }
        if(!pwd.equals(pwdAlign)){
            ToastUtil.errorShortToast("两次输入密码不一致!");
            return;
        }
        User user = new User();
        user.setEmail(email);
        user.setPassword(pwd);
        user.setPhone(phone);
        user.setNickname(name);
        Api.getInstance().register(user)
                .subscribe(new HttpObserver<DetailResponse<BaseMode>>() {
                    @Override
                    public void onSuccess(DetailResponse<BaseMode> data) {
                        LogUtil.d("register","<<<<<<<<<<<<<<<<<<<<>>>>" + data.getMessage());
                        login(user);
                    }
                });
    }

}
