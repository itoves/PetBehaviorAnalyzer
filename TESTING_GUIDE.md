# 姿态识别测试指南

## 已完成的改进

### 代码改进
1. ✅ `PetPoseDetector.kt` - 增强的几何特征分析
2. ✅ `YoloPoseDetector.kt` - YOLO 关键点检测框架
3. ✅ `PetDetector.kt` - 集成新的姿态检测逻辑

### 构建配置修复
1. ✅ 修复 AGP 版本（8.5.2）
2. ✅ 添加阿里云镜像源
3. ✅ 修复中文路径问题

## 测试步骤

### 第一阶段：测试几何特征改进

1. **安装 APK**
   ```bash
   # APK 位置
   app/build/outputs/apk/debug/app-debug.apk
   
   # 安装到手机
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **测试场景**
   - **站立姿态**：让狗狗站着不动，观察识别是否准确
   - **坐着姿态**：让狗狗坐下，观察识别
   - **躺下姿态**：让狗狗躺下，观察识别
   - **姿态转换**：从站→坐→躺，观察切换是否平滑

3. **观察指标**
   - 识别准确率：是否正确识别姿态
   - 稳定性：是否频繁闪烁切换
   - 响应速度：姿态改变后多久识别到

4. **记录问题**
   - 哪些姿态识别不准？
   - 什么角度容易误判？
   - 哪些品种的狗狗效果不好？

### 第二阶段：集成 YOLOv8-Pose（如果需要）

如果几何特征方案效果不够好，按以下步骤集成 YOLO 模型：

#### 1. 准备模型

**选项 A：快速测试（使用 COCO 人体模型）**
```bash
pip install ultralytics
python -c "from ultralytics import YOLO; YOLO('yolov8n-pose.pt').export(format='tflite')"
```

**选项 B：最佳效果（使用动物数据集微调）**
```bash
# 下载 AP-10K 数据集
git clone https://github.com/AlexTheBad/AP-10K

# 准备数据并训练
python train_animal_pose.py

# 导出模型
python export_tflite.py
```

#### 2. 添加模型到项目
```bash
# 复制模型文件
cp yolov8n-pose.tflite PetBehaviorAnalyzer/app/src/main/assets/
```

#### 3. 修改 PetDetector.kt

在 `analyzeBehavior()` 方法中启用 YOLO 检测：

```kotlin
// 静止状态：优先使用 YOLO 关键点检测
if (avgMovement <= MOVEMENT_SLEEP) {
    // 获取当前帧 bitmap
    val currentBitmap = getCurrentFrameBitmap()
    
    // YOLO 姿态检测
    val poseResult = yoloPoseDetector.detectPose(currentBitmap)
    
    if (poseResult != null && poseResult.confidence > 0.5f) {
        val analysis = yoloPoseDetector.analyzePoseFromKeypoints(poseResult)
        if (analysis.confidence > 0.6f) {
            return analysis.posture
        }
    }
    
    // 降级到几何特征分析
    val poseAnalysis = poseDetector.analyzeEnhancedPosture(...)
    return poseAnalysis.posture
}
```

#### 4. 重新构建测试
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 参数调优

### 几何特征方案参数

在 `PetPoseDetector.kt` 中调整：

```kotlin
// 站立判断
aspectRatio > 1.35f  // 增大 → 更严格，减小 → 更宽松

// 躺下判断
aspectRatio < 0.7f   // 增大 → 更宽松，减小 → 更严格

// 稳定性要求
aspectStdDev < 0.2f  // 增大 → 更宽松，减小 → 更严格
```

在 `PetDetector.kt` 中调整滞后帧数：

```kotlin
// 静止姿态切换确认帧数
if (behaviorConfidence <= -3)  // 增大 → 更稳定但响应慢
```

### YOLO 方案参数

在 `YoloPoseDetector.kt` 中调整：

```kotlin
// 置信度阈值
if (confidence < 0.5f)  // 增大 → 更严格，减小 → 更宽松

// 姿态判断阈值
bodyAngle in -30f..30f      // 身体角度范围
legExtension > 0.6f         // 腿部伸展程度
headHeight > 0.7f           // 头部高度
```

## 常见问题

### Q1: 姿态识别频繁闪烁
**解决方案**：
- 增加滞后确认帧数（-3 → -5）
- 提高稳定性要求（0.2 → 0.15）

### Q2: 某些角度识别不准
**解决方案**：
- 调整宽高比阈值
- 考虑使用 YOLO 关键点检测

### Q3: 不同品种效果差异大
**解决方案**：
- 使用 YOLO 模型
- 用特定品种数据微调模型

### Q4: 响应速度慢
**解决方案**：
- 减少确认帧数
- 使用更轻量的模型（yolov8n）

## 性能对比

| 方案 | 预期准确率 | FPS | 模型大小 |
|------|-----------|-----|----------|
| 原始方案 | 60-70% | 30+ | 0 MB |
| 几何特征增强 | 75-85% | 30+ | 0 MB |
| YOLO (COCO) | 80-90% | 20-25 | 6 MB |
| YOLO (微调) | 90-95% | 20-25 | 6 MB |

## 反馈与改进

测试后请记录：
1. 整体准确率提升了多少？
2. 哪些场景还有问题？
3. 是否需要集成 YOLO 模型？
4. 需要调整哪些参数？

根据反馈可以进一步优化算法和参数。
