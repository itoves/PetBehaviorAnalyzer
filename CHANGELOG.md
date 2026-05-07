# 版本更新日志

---

## v1.2.0 (2026-05-07) — 宠物日记 + 数据持久化大更新 📖

### 新增功能

#### 📊 行为日记 — 记录每一次互动
- **自动记录**：每次检测到宠物且行为/情绪变化时自动保存到 Room 数据库
- **去重机制**：仅行为或情绪真正变化时才记录，避免重复日志
- **按日期分组时间线**：LazyColumn 按日期分组展示，每天显示记录数和主导情绪
- **可爱UI**：每条记录包含时间戳、宠物类型emoji、行为标签、情绪标签、描述片段

#### 📸 有趣瞬间自动抓拍
- **智能触发**：检测到歪头杀、玩耍、靠近等可爱行为时自动抓拍
- **3秒冷却**：防止连续抓拍占用存储
- **全分辨率保存**：JPEG 格式保存到 filesDir/snapshots/
- **自动清理**：超过30天的快照自动删除

#### ⚠️ 异常行为提醒
- **4项检测规则**：
  - 过度睡眠：近2小时内睡眠占比超过70%
  - 频繁挠痒：1小时内挠痒次数超过15次
  - 持续低落：15分钟内出现10次以上SAD情绪
  - 长时间不活动：超过3小时无活跃行为
- **夜间豁免**：22:00-07:00期间跳过睡眠/不活动检测
- **温馨提醒**：页面顶部展示提醒卡片，可手动关闭
- **周期性检查**：每5分钟自动检测一次

#### 📈 活动量统计
- **今日统计卡片**：展示各类行为计数（走路、跑步、玩耍、睡觉等）
- **7天趋势柱状图**：Canvas 绘制自定义柱状图，展示一周活动量变化
- **活动建议**：根据统计结果给出个性化活动建议文案

#### 📅 心情日历
- **月视图日历**：自定义7列网格日历组件
- **每日情绪emoji**：每天显示主导情绪对应的emoji
- **月份切换**：支持前后月份浏览，未来月份不可进入
- **详情弹窗**：点击某天查看当日所有情绪分布

#### 🎴 日记卡生成与分享
- **精美卡片布局**：渐变背景 + 宠物类型 + 主导情绪 + 活动统计 + 精选描述
- **Compose → Bitmap**：通过 GraphicsLayer 渲染为 PNG 图片
- **一键分享**：通过 FileProvider + Intent.ACTION_SEND 分享到微信/相册等
- **自动缓存**：生成的卡片图片保存在 cache 目录

### 技术改进
- 引入 **Room 2.6.1** + **KSP 2.1.0-1.0.29** 数据持久化层
- 新增 BehaviorLogEntity、BehaviorLogDao（13条查询）、PetDatabase、PetDiaryRepository
- 拆分 PetDetector.imageProxyToFullBitmap() 获取全分辨率帧
- CameraController 暴露 captureSnapshot() 回调
- 新增 SnapshotManager 管理快照存储与冷却
- 新增 FileProvider 支持快照查看和日记卡分享
- 底部导航栏从2个标签扩展为3个（相机分析 / 狗语对话 / 宠物日记）
- 宠物日记标签页内使用 ScrollableTabRow 切换4个子页

### 修改的源码文件
| 文件 | 变更 |
|------|------|
| `build.gradle.kts` (root) | 添加 KSP 插件声明 |
| `app/build.gradle.kts` | KSP + Room 依赖，versionCode→3，versionName→"1.2" |
| `AndroidManifest.xml` | 添加 FileProvider |
| `PetAnalyzerApp.kt` | 初始化 Room 数据库 + Repository |
| `PetDetector.kt` | 拆分全分辨率bitmap方法，添加快照回调 |
| `CameraController.kt` | 暴露快照回调 |
| `MainScreen.kt` | 添加第3个导航标签"宠物日记"，接入快照和日志 |

### 新增的源码文件（18个）
- `data/entity/BehaviorLogEntity.kt` — Room 实体
- `data/dao/BehaviorLogDao.kt` — 13条查询方法
- `data/PetDatabase.kt` — 数据库单例
- `data/repository/PetDiaryRepository.kt` — 仓库封装 + 异常检测
- `camera/SnapshotManager.kt` — 快照保存与冷却
- `ui/diary/PetDiaryScreen.kt` — 日记容器页
- `ui/diary/BehaviorTimelineScreen.kt` — 行为时间线
- `ui/diary/ActivityStatsScreen.kt` — 活动统计
- `ui/diary/MoodCalendarScreen.kt` — 心情日历
- `ui/diary/DiaryCardScreen.kt` — 日记卡生成
- `ui/diary/AbnormalAlertManager.kt` — 异常检测引擎
- `ui/diary/components/TimelineItem.kt` — 时间线条目
- `ui/diary/components/StatsBarChart.kt` — Canvas 柱状图
- `ui/diary/components/CalendarGrid.kt` — 日历网格
- `ui/diary/components/DiaryCardComposable.kt` — 日记卡布局
- `res/xml/file_paths.xml` — FileProvider 路径配置

---

## v1.1.0 (2026-05-07) — 狗语对话大更新 🐶

### 新增功能

#### 🎵 狗语对话 — 与狗狗交流
- **叫声播放面板**：8种真实狗叫音频（来自 HuggingFace dog-dataset 学术数据集），点击按钮播放：
  - 🥰 友好问候 — 温柔短促的叫声，向狗狗表达友好
  - 🎾 玩耍邀请 — 轻快活泼的叫声，邀请狗狗一起玩耍
  - 🍖 食物召唤 — 兴奋的叫声，引起狗狗对食物的兴趣
  - 👋 呼唤过来 — 中频持续的叫声，呼唤狗狗靠近
  - ⚠️ 警告制止 — 低频有力的叫声，用于制止不良行为
  - 😢 安抚安慰 — 柔和低沉的叫声，安抚焦虑的狗狗
  - 🤩 兴奋夸奖 — 高频欢快的叫声，表扬狗狗
  - 🔔 吸引注意 — 短促清脆的叫声，引起狗狗注意
- **音频来源**：真实犬只叫声 WAV 文件（CC0/学术许可证），通过 Python 脚本自动裁剪最优片段
- **播放引擎**：Android MediaPlayer + AssetFileDescriptor，真实狗叫音频

#### 🎙️ 狗叫分析 — 听懂狗狗的心声
- **录音分析功能**：点击录音按钮捕捉狗狗的叫声（最长3秒）
- **智能分析引擎**：基于音频频率、振幅、持续时间等特征进行分析：
  - 识别吠叫类型：开心叫、警戒叫、焦虑叫、玩耍叫、问候叫
  - 分析情绪倾向：兴奋、平静、焦虑、友善
  - 给出对应的拟人化解读和实用建议
- **音频特征提取**：RMS 能量、零交叉率、自相关音高估计、频率方差
- **可视化结果展示**：叫声类型、情绪标签、拟人化解读、建议操作、置信度、时长/能量/音高详情

#### 🧭 底部导航栏
- 新增底部导航栏，包含"相机分析"和"狗语对话"两个标签页
- 图标 + 文字标签，清晰直观
- 保留原有相机行为分析功能

### 技术改进
- 使用 Android `MediaPlayer` + 真实狗叫 WAV 音频文件（取代之前的 AudioTrack 合成方案）
- 使用 Android `AudioRecord` 进行录音采集和分析
- 音频特征提取：RMS 能量、零交叉率、频谱质心等

---

## v1.0.0 (初始版本) — 宠物行为分析翻译官 🐾

### 核心功能

#### 📷 实时相机检测
- 使用 CameraX 实现实时相机预览
- 后置摄像头，640x480 分辨率采集
- 每秒分析约10帧，流畅实时反馈

#### 🤖 AI 行为识别
- **宠物检测**：基于 EfficientDet-Lite0 目标检测模型
- **行为分类**：支持 17 种宠物行为识别
  - 坐、站、躺、睡、走、跑、玩耍
  - 吃、喝、伸懒腰、看、靠近
  - 歪头杀、挠痒、叫唤
- **运动分析**：基于30帧滑动窗口，分析位移、宽高比、面积变化
- **滞后稳定机制**：防止行为标签闪烁

#### 😊 情绪感知
- **规则推断**：基于行为类型映射情绪
- **MobileNetV1 辅助**：每5帧运行图像分类器，微调情绪判断
- 支持 7 种情绪：平静、开心、兴奋、困了、好奇、想玩、不开心

#### 💬 拟人化翻译
- 以动物第一人称生成可爱的中文描述
- 每种行为有多种随机描述，增加趣味性
- 猫猫和狗狗分别有不同的语气前缀

#### 🎨 可爱风格 UI
- Material 3 设计语言，Jetpack Compose 实现
- 粉色/薄荷绿/薰衣草紫温柔配色
- 行为气泡弹性动画 + 脉搏呼吸效果
- 宠物类型/情绪状态实时显示

### 技术栈
| 类别 | 技术 |
|------|------|
| 语言 | Kotlin 2.1.0 |
| UI | Jetpack Compose + Material 3 |
| 相机 | CameraX 1.3.1 |
| 目标检测 | EfficientDet-Lite0 (TFLite) |
| 图像分类 | MobileNetV1 (TFLite) |
| 最低SDK | Android 8.0 (API 26) |
| 目标SDK | Android 14 (API 35) |

---

*文档最后更新：2026年5月7日*
