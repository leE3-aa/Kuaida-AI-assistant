# 快答 App 🎤

> 面试官刚问完，你脑子还在转圈圈？  
> 让 AI 替你"快答"一手。

一款 Android 上的面试 AI 小助手。**录音 / 打字** 问题进去，**结构化、能直接口述** 的回答出来。

---

## ✨ 它能做什么

- 🎙️ **录音就能问** — 模拟面试时按一下录音，问题自动转文字
- ⚡ **5 秒内出答案** — 听到问题到看到答案，不让冷场超过一口气
- 🧠 **答案能直接念** — 默认口语化 + 有结构，不是机翻味的长篇大论
- 🔁 **一键换风格** — "更简洁"、"补个案例"、"再口语点"，按按钮就行
- 🎚️ **模型随便切** — 国内国外多家 Provider，自己的 Key 自己花
- 💰 **看得见花了多少** — 每次 token、费用都摆在那，不偷偷烧钱
- 📚 **带个小脑子** — 上传简历 / 项目 PDF，回答会贴着你本人的经历讲
- 📒 **历史能回看** — 哪次答得漂亮、哪次翻车，回头复盘一目了然

## 🎯 适合谁

准备校招、社招、跳槽、远程面试的同学。前提：
- 你能自己申请到一个大模型 API Key
- 你不打算靠它"替你面试"，而是想要一份**靠谱的回答草稿**

不适合：完全不想动脑、希望 AI 接管整场面试的朋友 🙃

## 🛠 用了什么搭起来的

| 模块 | 选型 |
| --- | --- |
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | 多模块（`app` / `core:*` / `feature:*`）+ MVVM |
| 依赖注入 | Hilt |
| 导航 | Navigation Compose |
| PDF 解析 | PdfBox-Android |
| 构建 | Gradle KTS + Version Catalog |
| 最低系统 | Android 8.0 (SDK 26)，目标 SDK 35 |

## 📁 项目长这样

```
快答app/
├── app/                # 应用入口
├── core/
│   ├── common/         # 通用工具 / 主题 / 组件
│   ├── model/          # 数据模型
│   └── data/           # 仓库 / 本地存储 / 网络
└── feature/
    ├── workbench/      # 工作台（主问答页）
    ├── knowledge/      # 知识库（简历 / PDF）
    ├── history/        # 历史记录
    ├── settings/       # 设置（模型 / API Key）
    └── templates/      # 模板
```

## 🚀 跑起来

1. Clone 下来
   ```bash
   git clone <repo-url>
   cd 快答app
   ```
2. 根目录建一个 `local.properties`（已被 gitignore，不会传上去），填本机 SDK 路径：
   ```properties
   sdk.dir=/your/path/to/Android/sdk
   ```
3. 用 Android Studio 打开直接 Run，或者命令行打个 debug 包：
   ```bash
   ./gradlew :app:assembleDebug
   ```
   产物在 `app/build/outputs/apk/debug/app-debug.apk`，拖进手机就能装。

**第一次启动**：去「设置」页填你的 API Key，就能开聊了。Key 只存在你手机本地，谁也偷不走。

## 📌 当前进度

- ✅ MVP 已经能跑：录音、转写、回答生成、模型切换、历史、知识库、模板都通了
- 🚧 还在打磨：UI 视觉升级中
- 🙅 不打算做：账号系统、云同步、上架应用商店（这玩意儿就是自己用的）

## ⚖️ 一句话许可

自用 + 给朋友用，**不商用、不上架**。觉得有意思就 Star 一下，觉得没意思就当没看见 😄
