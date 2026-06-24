package com.ixuea.courses.mymusic;

import android.app.Application;

import androidx.multidex.MultiDexApplication;

import com.facebook.stetho.Stetho;
import com.ixuea.courses.mymusic.coverage.CoverageCollector;
import com.ixuea.courses.mymusic.domain.Session;
import com.ixuea.courses.mymusic.domain.event.LoginSuccessEvent;
import com.ixuea.courses.mymusic.util.PreferenceUtil;
import com.ixuea.courses.mymusic.util.ToastUtil;
import com.ixuea.courses.mymusic.util.StringUtil;

import org.greenrobot.eventbus.EventBus;

import es.dmoral.toasty.Toasty;

/**
 * 相当于一个全局的Activity，在整个应用的生命周期中只执行一次
 * 要在Manifest文件的application节点中的name节点中引用
 */
public class AppContent extends MultiDexApplication {
    private static AppContent appContent;

    @Override
    public void onCreate() {
        super.onCreate();
        //初始化toast工具类
        Toasty.Config.getInstance().apply();

        ToastUtil.init(getApplicationContext());

        // 仅 debug 构建开启覆盖率收集
        if (BuildConfig.DEBUG) {
            // 10.0.2.2 是 Android 模拟器访问宿主机 localhost 的特殊地址
            CoverageCollector.init(
                    this,
                    "http://10.0.2.2:3001",
                    "6a390d5dd0f12e7a31a4c9a3"   // projectId（平台上创建项目后拿到的，长期不变）
            );

            // 验证多仓库组件（common-utils）的增量覆盖率：只传 30，只命中 durationLabel
            // 的 "short" 分支，"medium"/"long" 故意不覆盖
            android.util.Log.d("AppContent", "duration label: " + StringUtil.durationLabel(30));
        }

        //初始化Stetho抓包
        //使用默认参数初始化
//        Stetho.initializeWithDefaults(this);
    }

    public static AppContent getInstance(){
        if(appContent == null){
            appContent = new AppContent();
        }
        return appContent;
    }

    public void login(PreferenceUtil ps, Session session){
        ps.setSession(session.getSession());
        ps.setUserId(session.getUser());
        //发送通知，关闭登录注册界面
        EventBus.getDefault().post(new LoginSuccessEvent());
        onLogin();
    }

    /**
     * 用于初始化其他登录后的数据
     */
    private void onLogin() {

    }


}
