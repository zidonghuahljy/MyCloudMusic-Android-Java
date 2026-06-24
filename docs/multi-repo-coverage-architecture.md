# Android 多仓库组件化覆盖率方案

## 目标

当 Android App 由多个独立 Git 仓库共同组装时，覆盖率平台不能再只用壳工程的 `commitHash` 标识一次构建。

原因是：壳工程 commit 可能不变，但某个组件 AAR 已经升级。此时如果仍然使用壳工程 commit 作为唯一构建身份，平台会把不同 APK 误认为同一次构建，导致源码映射、class 文件匹配、增量覆盖率都可能失真。

最终方案是引入整包构建身份：

```text
projectId + buildIdentityHash -> buildId
```

其中 `buildIdentityHash` 由整包所有源码仓库的 commit 信息生成。

## 最终仓库形态

App 由以下仓库组成：

- 壳工程仓库：负责 Application、Manifest、路由启动、依赖组装、签名和打包。
- 业务组件仓库：独立构建、测试、发布 AAR。
- 公共契约仓库：接口、路由协议、共享模型、通用 UI、网络能力等。

示例：

```text
mycloudmusic-android-shell
mycloudmusic-common-api
mycloudmusic-common-model
mycloudmusic-common-network
mycloudmusic-common-ui
mycloudmusic-feature-home
mycloudmusic-feature-player
mycloudmusic-feature-search
mycloudmusic-feature-user
mycloudmusic-feature-login
```

壳工程通过 Maven 坐标消费组件：

```gradle
implementation "com.mycloudmusic:common-api:1.4.0"
implementation "com.mycloudmusic:common-network:1.2.3"
implementation "com.mycloudmusic:feature-home:2.1.0"
implementation "com.mycloudmusic:feature-player:1.8.2"
implementation "com.mycloudmusic:feature-search:1.5.4"
```

每个组件 AAR 必须能让壳工程 CI 解析出它的源码身份：

- module 名称
- Maven 坐标
- version
- repository URL
- 完整 Git commit hash
- branch 或 tag
- source root 映射

## 构建身份

多仓库场景必须使用 `buildIdentityHash`，不能只使用单个 `commitHash`。

`buildIdentityHash` 是 `build-fingerprint.json` 的 SHA-256：

```text
buildIdentityHash = sha256(canonical_build_fingerprint_json)
```

`build-fingerprint.json` 必须稳定：

- UTF-8 编码
- key 顺序确定
- components 按 module 排序
- 使用完整 40 位 Git hash
- 不包含时间戳
- 不包含本机绝对路径

示例：

```json
{
  "schemaVersion": 1,
  "platform": "android",
  "applicationId": "com.ixuea.courses.mymusic",
  "variant": "devDebug",
  "shell": {
    "module": "app",
    "repositoryUrl": "https://github.com/example/mycloudmusic-android-shell.git",
    "commitHash": "466c02fce0e6d52bc51f2f507aa33ca6cc507335",
    "branch": "main"
  },
  "components": [
    {
      "module": "common-utils",
      "mavenCoordinate": "com.mycloudmusic:common-utils:1.0.0",
      "repositoryUrl": "https://github.com/example/mycloudmusic-common-utils.git",
      "commitHash": "2222222222222222222222222222222222222222",
      "branch": "main",
      "sourceRoots": ["src/main/java"]
    }
  ]
}
```

任意组件 commit 变化，`buildIdentityHash` 都必须变化。

## Android BuildConfig 约定

App 构建时注入：

```gradle
buildConfigField "String", "GIT_COMMIT_HASH", "\"${shellCommitHash}\""
buildConfigField "String", "BUILD_IDENTITY_HASH", "\"${buildIdentityHash}\""
buildConfigField "String", "BUILD_FINGERPRINT_JSON", "\"${escapedBuildFingerprintJson}\""
```

运行时覆盖率 SDK 优先使用：

```text
projectId + BUILD_IDENTITY_HASH
```

`GIT_COMMIT_HASH` 只保留给壳工程源码展示和旧平台兼容。

## classfiles.zip 结构

CI 创建 Build 时必须上传整包所有源码仓库的 `.class`，不能只上传壳工程 class。

推荐结构：

```text
classfiles.zip
  manifest.json
  app/classes/...
  components/common-utils/classes/...
  components/feature-home/classes/...
```

`manifest.json` 示例：

```json
{
  "schemaVersion": 1,
  "entries": [
    {
      "module": "app",
      "repositoryUrl": "https://github.com/example/mycloudmusic-android-shell.git",
      "commitHash": "466c02fce0e6d52bc51f2f507aa33ca6cc507335",
      "classRoot": "app/classes",
      "sourceRoots": ["app/src/main/java", "app/src/main/kotlin"]
    },
    {
      "module": "common-utils",
      "repositoryUrl": "https://github.com/example/mycloudmusic-common-utils.git",
      "commitHash": "2222222222222222222222222222222222222222",
      "classRoot": "components/common-utils/classes",
      "sourceRoots": ["src/main/java"]
    }
  ]
}
```

平台用这个 manifest 把 JaCoCo probe 映射回正确仓库和正确 commit。

## diffs.zip 结构

增量覆盖率需要支持多个仓库的 diff。

推荐结构：

```text
diffs.zip
  manifest.json
  app.diff
  common-utils.diff
  feature-home.diff
```

`manifest.json` 示例：

```json
{
  "schemaVersion": 1,
  "entries": [
    {
      "module": "app",
      "repositoryUrl": "https://github.com/example/mycloudmusic-android-shell.git",
      "baseCommit": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
      "headCommit": "466c02fce0e6d52bc51f2f507aa33ca6cc507335",
      "diffFile": "app.diff"
    },
    {
      "module": "common-utils",
      "repositoryUrl": "https://github.com/example/mycloudmusic-common-utils.git",
      "baseCommit": "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
      "headCommit": "2222222222222222222222222222222222222222",
      "diffFile": "common-utils.diff"
    }
  ]
}
```

每个 diff 建议使用：

```bash
git diff <baseCommit> <headCommit> --unified=0 -- src/main/java src/main/kotlin
```

没有变更的组件可以不传 diff，但仍然应该上传 class 文件用于全量覆盖率合并。

## 平台 API 约定

### 创建 Build

扩展现有接口：

```text
POST /api/builds
Content-Type: multipart/form-data
```

字段：

```text
projectId           string，必填
platform            string，必填，固定 android
buildIdentityHash   string，必填
buildFingerprint    file 或 string，必填，build-fingerprint.json
binary              file，必填，classfiles.zip
diffs               file，可选，diffs.zip
shellCommitHash     string，可选，壳工程完整 commit
branch              string，可选，壳工程分支或发布分支
variant             string，可选，例如 devDebug
```

成功响应：

```json
{
  "success": true,
  "data": {
    "buildId": "6a38f5a7b3ec204aa0cf9bbe",
    "buildIdentityHash": "..."
  }
}
```

幂等键：

```text
projectId + buildIdentityHash
```

同一个 identity 重复创建 Build，应复用同一个 Build，并替换 classfiles/diffs 等构建产物。

### 解析 Build

多仓库 Android SDK 使用：

```text
GET /api/builds/resolve?projectId=&buildIdentityHash=
```

成功响应：

```json
{
  "success": true,
  "data": {
    "buildId": "6a38f5a7b3ec204aa0cf9bbe"
  }
}
```

旧接口可以保留：

```text
GET /api/builds/resolve?projectId=&commitHash=
```

但多仓库项目必须使用 `buildIdentityHash`。

### 上传原始覆盖率

保持现有接口：

```text
POST /api/builds/:buildId/raw-coverage
Content-Type: multipart/form-data
```

字段：

```text
file         必填，JaCoCo .ec 文件
deviceInfo   可选，设备信息 JSON
testerName   可选，测试人员标识
```

raw coverage 上传不需要重复传组件 commit，因为 `buildId` 已经绑定了完整构建身份。

## 源码展示规则

平台展示源码时，不能默认所有源码都在壳工程仓库。

源码查找 key 应该是：

```text
module + repositoryUrl + commitHash + sourceRelativePath
```

这些信息来自 `build-fingerprint.json` 或 `classfiles.zip/manifest.json`。

## 覆盖率报告结构

平台报告需要保留 module/repository 归属。

推荐结构：

```json
{
  "buildId": "...",
  "buildIdentityHash": "...",
  "summary": {
    "lineCoverage": 82.4,
    "changedLineCoverage": 76.8
  },
  "modules": [
    {
      "module": "app",
      "repositoryUrl": "...",
      "commitHash": "...",
      "summary": {
        "lineCoverage": 80.1,
        "changedLineCoverage": 70.0
      }
    },
    {
      "module": "common-utils",
      "repositoryUrl": "...",
      "commitHash": "...",
      "summary": {
        "lineCoverage": 86.3,
        "changedLineCoverage": 81.2
      }
    }
  ]
}
```

平台应支持：

- 整包覆盖率
- 单 module 覆盖率
- 单仓库覆盖率
- 单 module 增量覆盖率
- 整包增量覆盖率

## Runtime SDK 流程

Android SDK 运行时只需要：

```text
baseUrl
projectId
buildIdentityHash
```

流程：

```text
App 启动
CoverageCollector 初始化
App 进入后台
SDK dump .ec
SDK 调 GET /api/builds/resolve?projectId=&buildIdentityHash=
SDK 缓存 buildId
SDK 调 POST /api/builds/:buildId/raw-coverage 上传 .ec
```

旧平台未适配前，可以临时回退到 `commitHash` resolve。

## CI 流程

最终 CI：

```text
1. 解析壳工程和所有组件仓库身份。
2. 生成 build-fingerprint.json。
3. 计算 buildIdentityHash。
4. 注入 BUILD_IDENTITY_HASH 和 BUILD_FINGERPRINT_JSON 到 BuildConfig。
5. 构建 Android variant。
6. 收集壳工程和组件的 class 文件。
7. 生成带 manifest.json 的 classfiles.zip。
8. 为有变更的仓库生成 diffs.zip。
9. POST /api/builds 创建或复用 Build。
10. 分发 APK。
11. App 运行时上传 .ec。
12. 平台按 buildIdentityHash 合并覆盖率并计算全量/增量覆盖率。
```

## 平台需要适配的能力

1. 使用 `projectId + buildIdentityHash` 作为多仓库 Build 幂等键。
2. 存储 `build-fingerprint.json`。
3. 解析 `classfiles.zip/manifest.json`。
4. 支持多仓库源码拉取。
5. 支持多仓库 diff 输入。
6. 支持 module/repository 维度覆盖率聚合。
7. `resolve` 接口支持 `buildIdentityHash`。
8. 保留单仓库 `commitHash` 兼容逻辑。

## 本仓库验证实现

本项目当前验证了一个最小多仓库组件：

```text
MyCloudMusic-Android-Java       壳工程
mycloudmusic-common-utils       独立组件仓库
```

壳工程通过 composite build 引入组件：

```gradle
includeBuild('../mycloudmusic-common-utils') {
    dependencySubstitution {
        substitute module('com.mycloudmusic:common-utils') using project(':')
    }
}
```

App 依赖组件坐标：

```gradle
implementation 'com.mycloudmusic:common-utils:1.0.0'
```

`StringUtil` 已从壳工程迁移到组件仓库；App 登录、注册、评分页面继续通过相同包名使用该工具类。

壳工程构建时会生成并注入：

```text
BuildConfig.GIT_COMMIT_HASH
BuildConfig.BUILD_IDENTITY_HASH
BuildConfig.BUILD_FINGERPRINT_JSON
```

覆盖率上传时优先使用 `BUILD_IDENTITY_HASH` resolve Build，旧平台未适配时回退到 `GIT_COMMIT_HASH`。
