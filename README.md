# AI Hub

一个原生 Android App，用来集中管理 ChatGPT、Claude、Gemini、DeepSeek 等 AI 平台的多个账号、会员套餐、续费日期和使用额度。

技术栈：Kotlin · Jetpack Compose · Material 3 · Room · Android Keystore · Custom Tabs
最低系统：Android 8.0（API 26）

## 第一阶段已完成

- **仪表盘**：每月订阅总支出（按币种分开，不做汇率估算）、付费会员数、即将续费/到期提醒、额度偏低提醒、四个平台的概览卡片。
- **账号管理**：每个平台可添加多个账号；按平台筛选；一键打开官方聊天页面。
- **会员信息**：套餐（带常见套餐预设和参考价）、订阅状态、按月/按年计费、费用与币种、续费/到期日。
- **额度记录**：可选“暂不记录”或“手动记录”（已用 / 总额 / 单位 / 重置日）。所有额度都标注来源，没有数据就显示“未获取”，绝不估算。
- **安全**：数据只存本机；私密备注用 Keystore 生成的 AES-256-GCM 密钥加密；关闭云备份；默认开启隐私屏幕（禁止截屏、最近任务中隐藏）；App 不索取任何平台密码。

## 第二阶段已完成

- **DeepSeek API 余额自动同步**：在 DeepSeek 账号里填写自己的 API Key，App 调用官方 `GET https://api.deepseek.com/user/balance` 查询余额，标注“自动同步”和更新时间。打开仪表盘时自动刷新超过 1 小时的数据，右上角可手动同步。失败时保留上次成功的数据并写明原因。
- **订阅收据识别**：在邮箱里把 OpenAI / Anthropic / Google / App Store / Google Play 的订阅收据或扣款邮件“分享”到 AI Hub，或复制粘贴，App 在本机识别平台、套餐（含 Claude Max 5x/20x、Google AI Pro/Ultra）、金额币种、计费周期、扣款日和续费/到期日，确认后写入指定账号，标注“收据识别”。收据里没写续费日时按扣款日推算并明确提示；识别不到的字段保持原值。
- **来源标注**：套餐来源分“手动记录 / 收据识别”，额度来源分“未获取 / 手动记录 / 自动同步”。收据识别的套餐被手动修改后自动改回“手动记录”。
- **安全**：API Key 用 Keystore 加密、只发给官方接口、保存后不回显；收据原文只在内存中识别，不保存不上传。新增的 INTERNET 权限只用于余额查询。
- 数据库升级到 v2（带迁移，第一阶段的数据不会丢失）。

## 构建 APK

### 方式一：GitHub Actions（无需本地环境）

把本项目推到 GitHub 仓库后，`.github/workflows/build-apk.yml` 会自动运行单元测试并构建 debug APK。
在仓库的 **Actions → 最新一次运行 → Artifacts** 下载 `AIHub-debug-apk`，解压得到 `app-debug.apk`，传到手机安装即可（需允许“安装未知来源应用”）。

### 方式二：Android Studio

用 Android Studio（Ladybug 或更新版本）打开项目根目录，等待 Gradle 同步完成后，菜单 **Build → Build App Bundle(s) / APK(s) → Build APK(s)**。

### 方式三：命令行

需要 JDK 17+ 和 Android SDK（compileSdk 35），并在 `local.properties` 中写入 `sdk.dir=...`：

```bash
./gradlew assembleDebug
# 输出：app/build/outputs/apk/debug/app-debug.apk
```

## 项目结构

```
app/src/main/java/com/aihub/app/
├── AiHubApp.kt              Application + 简易依赖容器 + 设置
├── MainActivity.kt
├── data/
│   ├── model/Platform.kt    平台、套餐预设、状态等枚举
│   ├── db/                  Room：AccountEntity / AccountDao / AppDatabase
│   ├── security/SecureStore.kt  Keystore 加密存储（备注、API Key）
│   ├── sync/                DeepSeek 官方余额接口
│   ├── receipt/             订阅收据识别与导入
│   └── AccountRepository.kt
├── ui/
│   ├── dashboard/           仪表盘
│   ├── accounts/            账号列表
│   ├── edit/                添加 / 编辑账号（含 API Key 与余额同步）
│   ├── receipt/             收据导入页
│   ├── settings/            设置与开发路线
│   ├── components/          通用组件（平台头像、状态标签、额度条、打开官方页面）
│   ├── navigation/          底部导航与路由
│   └── theme/
└── util/Format.kt           日期、金额、倒计时格式化
```

## 后续阶段

各平台能否自动识别套餐与额度，见 [docs/FEASIBILITY.md](docs/FEASIBILITY.md)。
