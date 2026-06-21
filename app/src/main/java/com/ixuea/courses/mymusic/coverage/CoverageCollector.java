package com.ixuea.courses.mymusic.coverage;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Android 代码覆盖率收集器（JaCoCo + 自动上传）
 *
 * 使用方式：
 * 1. app/build.gradle 中 debug buildType 添加 testCoverageEnabled true
 * 2. Application.onCreate() 中调用 CoverageCollector.init()
 * 3. App 进入后台时自动 dump .ec 文件并上传到覆盖率平台
 */
public class CoverageCollector {

    private static final String TAG = "CoverageCollector";
    private static final String COVERAGE_DIR = "coverage";
    private static final String COVERAGE_EXT = ".ec";

    // dump 最小间隔 30 秒，防止频繁触发
    private static final long MIN_DUMP_INTERVAL_MS = 30_000L;

    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicLong lastDumpTimeMs = new AtomicLong(0L);
    private static volatile CoverageUploader uploader = null;

    // 单线程执行器：串行执行 dump/upload，避免并发写文件
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * 初始化收集器。
     *
     * @param application Application 实例
     * @param serverUrl   覆盖率平台地址，如 "http://192.168.1.100:3001"
     * @param projectId   平台上创建的项目 ID
     */
    public static void init(Application application, String serverUrl, String buildId) {
        if (!initialized.compareAndSet(false, true)) return;

        if (serverUrl != null && !serverUrl.isEmpty()
                && buildId != null && !buildId.isEmpty()) {
            uploader = new CoverageUploader(serverUrl, buildId);
        }

        application.registerActivityLifecycleCallbacks(
                new CoverageLifecycleCallbacks(application)
        );
        Log.d(TAG, "CoverageCollector initialized (upload=" + (uploader != null) + ")");
    }

    /**
     * 手动触发覆盖率数据 dump。
     *
     * @param context Application Context
     * @param force   true 时忽略时间间隔限制
     * @return 保存的文件路径，失败返回 null
     */
    public static String dumpCoverage(Context context, boolean force) {
        long now = System.currentTimeMillis();
        long last = lastDumpTimeMs.get();

        if (!force && now - last < MIN_DUMP_INTERVAL_MS) {
            Log.d(TAG, "Skip dump: too frequent (" + (now - last) + "ms ago)");
            return null;
        }
        if (!lastDumpTimeMs.compareAndSet(last, now)) {
            if (!force) return null;
            lastDumpTimeMs.set(now);
        }

        File coverageFile = null;
        try {
            coverageFile = createCoverageFile(context);

            Class<?> rtClass = Class.forName("org.jacoco.agent.rt.RT");
            Object agent = rtClass.getMethod("getAgent").invoke(null);
            if (agent == null) {
                Log.d(TAG, "JaCoCo agent is null — testCoverageEnabled must be true");
                if (!force) lastDumpTimeMs.set(last);
                return null;
            }

            byte[] data = (byte[]) agent.getClass()
                    .getMethod("getExecutionData", boolean.class)
                    .invoke(agent, false);

            if (data == null || data.length == 0) {
                Log.d(TAG, "JaCoCo dump returned empty data");
                coverageFile.delete();
                // 不重置计时器，保持节流间隔生效
                return null;
            }

            try (FileOutputStream fos = new FileOutputStream(coverageFile)) {
                fos.write(data);
            }
            Log.d(TAG, "Coverage saved: " + coverageFile.getName() + " (" + data.length + " bytes)");

            if (uploader != null) {
                final File toUpload = coverageFile;
                executor.execute(() -> uploader.upload(toUpload));
            }

            return coverageFile.getAbsolutePath();

        } catch (ClassNotFoundException e) {
            Log.d(TAG, "JaCoCo runtime not found — testCoverageEnabled must be true in debug buildType");
            if (coverageFile != null) coverageFile.delete();
            if (!force) lastDumpTimeMs.set(last);
            return null;
        } catch (Exception e) {
            Log.d(TAG, "dumpCoverage failed: " + e.getMessage());
            if (coverageFile != null) coverageFile.delete();
            if (!force) lastDumpTimeMs.set(last);
            return null;
        }
    }

    private static File createCoverageFile(Context context) throws IOException {
        File dir = new File(context.getFilesDir(), COVERAGE_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create coverage dir: " + dir.getAbsolutePath());
        }
        String ts = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(dir, "coverage_" + ts + "_" + Process.myPid() + COVERAGE_EXT);
    }

    // -------------------------------------------------------------------------
    // 生命周期回调：App 进入后台时自动触发 dump
    // -------------------------------------------------------------------------
    private static class CoverageLifecycleCallbacks implements Application.ActivityLifecycleCallbacks {

        private final Application application;
        // 用 resumed Activity 数量判断前后台，避免旋转等配置变更误判
        private final AtomicInteger resumedCount = new AtomicInteger(0);

        CoverageLifecycleCallbacks(Application application) {
            this.application = application;
        }

        @Override public void onActivityCreated(Activity a, Bundle b) {}
        @Override public void onActivityStarted(Activity a) {}
        @Override public void onActivityStopped(Activity a) {}
        @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}

        @Override
        public void onActivityResumed(Activity activity) {
            resumedCount.incrementAndGet();
        }

        @Override
        public void onActivityPaused(Activity activity) {
            int count = resumedCount.updateAndGet(c -> c > 0 ? c - 1 : 0);
            if (count == 0) {
                // 所有 Activity 已 paused，App 进入后台
                final Context ctx = application;
                executor.execute(() -> dumpCoverage(ctx, false));
            }
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (activity.isFinishing() && resumedCount.get() == 0) {
                long elapsed = System.currentTimeMillis() - lastDumpTimeMs.get();
                if (elapsed >= MIN_DUMP_INTERVAL_MS) {
                    final Context ctx = application;
                    executor.execute(() -> dumpCoverage(ctx, true));
                }
            }
        }
    }
}
