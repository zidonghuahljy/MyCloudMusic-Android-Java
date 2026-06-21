package com.ixuea.courses.mymusic.coverage;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 覆盖率文件上传器，使用 OkHttp 将 .ec 文件 POST 到覆盖率平台。
 */
public class CoverageUploader {

    private static final String TAG = "CoverageUploader";

    private final String serverUrl;
    private final String buildId;
    private final OkHttpClient client;

    public CoverageUploader(String serverUrl, String buildId) {
        this.serverUrl = serverUrl.endsWith("/")
                ? serverUrl.substring(0, serverUrl.length() - 1)
                : serverUrl;
        this.buildId = buildId;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 异步上传覆盖率文件，失败仅打印日志，不抛出异常。
     */
    public void upload(File coverageFile) {
        if (!coverageFile.exists()) {
            Log.d(TAG, "Coverage file not found: " + coverageFile.getAbsolutePath());
            return;
        }

        RequestBody fileBody = RequestBody.create(
                coverageFile,
                MediaType.get("application/octet-stream")
        );

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", coverageFile.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(serverUrl + "/api/builds/" + buildId + "/raw-coverage")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "Upload failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) {
                Log.d(TAG, "Upload response: " + response.code());
                response.close();
            }
        });
    }
}
