package com.ixuea.courses.mymusic.coverage;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.json.JSONObject;

/**
 * 覆盖率文件上传器，使用 OkHttp 将 .ec 文件 POST 到覆盖率平台。
 *
 * buildId 不需要手动维护：构造时只传 projectId，commitHash 来自 BuildConfig.GIT_COMMIT_HASH
 * （build.gradle 编译时注入，见接入文档第 2.4 节）。首次上传时用 (projectId, commitHash) 调
 * GET /api/builds/resolve 换成 buildId 并缓存，要求 CI/本机已经用同一个 commitHash 调过
 * POST /api/builds 上传过 classfiles.zip，否则 resolve 会 404（预期行为，说明这次构建还没有
 * 可用于解析覆盖率数据的编译产物）。
 */
public class CoverageUploader {

    private static final String TAG = "CoverageUploader";

    private final String serverUrl;
    private final String projectId;
    private final String commitHash;
    private final OkHttpClient client;
    private final AtomicReference<String> cachedBuildId = new AtomicReference<>(null);

    public CoverageUploader(String serverUrl, String projectId, String commitHash) {
        this.serverUrl = serverUrl.endsWith("/")
                ? serverUrl.substring(0, serverUrl.length() - 1)
                : serverUrl;
        this.projectId = projectId;
        this.commitHash = commitHash;
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
        if (commitHash == null || commitHash.isEmpty()) {
            Log.w(TAG, "GIT_COMMIT_HASH not available, skipping upload");
            return;
        }

        String buildId = cachedBuildId.get();
        if (buildId != null) {
            doUpload(coverageFile, buildId);
            return;
        }

        resolveBuildId(resolvedBuildId -> {
            if (resolvedBuildId == null) {
                Log.w(TAG, "No build found for commit " + commitHash
                        + ". Make sure CI called POST /api/builds for this commit first.");
                return;
            }
            cachedBuildId.set(resolvedBuildId);
            doUpload(coverageFile, resolvedBuildId);
        });
    }

    private interface ResolveCallback {
        void onResolved(String buildId);
    }

    /**
     * 用 (projectId, commitHash) 换 buildId，换到之后由调用方缓存。
     */
    private void resolveBuildId(ResolveCallback callback) {
        String url;
        try {
            url = serverUrl + "/api/builds/resolve"
                    + "?projectId=" + URLEncoder.encode(projectId, StandardCharsets.UTF_8.name())
                    + "&commitHash=" + URLEncoder.encode(commitHash, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            callback.onResolved(null);
            return;
        }

        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "Resolve buildId failed: " + e.getMessage());
                callback.onResolved(null);
            }

            @Override
            public void onResponse(Call call, Response response) {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onResolved(null);
                        return;
                    }
                    JSONObject json = new JSONObject(response.body().string());
                    JSONObject data = json.optJSONObject("data");
                    String buildId = data != null ? data.optString("buildId", null) : null;
                    callback.onResolved(buildId == null || buildId.isEmpty() ? null : buildId);
                } catch (Exception e) {
                    Log.d(TAG, "Failed to parse resolve response: " + e.getMessage());
                    callback.onResolved(null);
                } finally {
                    response.close();
                }
            }
        });
    }

    private void doUpload(File coverageFile, String buildId) {
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
