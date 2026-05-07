# 狗狗姿态识别改进方案

## 问题分析

原有实现仅依赖**检测框的宽高比**来判断姿态（站/坐/躺），存在以下问题：
- 不同角度（侧面/正面）的宽高比差异很大
- 不同品种（柯基矮胖/灰狗修长）的比例不同
- 无法准确区分相似姿态（坐着 vs 站着）

## 解决方案

### 方案一：增强的几何特征分析（已实现）✅

**文件**：`PetPoseDetector.kt`

**改进点**：
1. **多帧宽高比趋势分析**：检测比例变化方向（站→躺、躺→站）
2. **稳定性评估**：计算宽高比标准差，确保姿态稳定
3. **置信度计算**：结合极端比例和稳定性
4. **更严格的阈值**：站立 > 1.35，躺下 < 0.7
5. **改进的滞后机制**：静止姿态切换需要 4 帧确认

**优点**：
- 无需额外模型，轻量级
- 已集成到现有代码

**缺点**：
- 精度有限，复杂姿态仍可能误判

---

### 方案二：YOLOv8-Pose 关键点检测（推荐）⭐

**文件**：`YoloPoseDetector.kt`

**原理**：
使用 YOLOv8-Pose 模型检测狗狗的关键点（头、颈、四肢、尾巴），通过关键点的位置和角度关系判断姿态。

**关键点判断逻辑**：
- **站立**：身体水平（-30°~30°）+ 腿伸展 > 0.6 + 头部高 > 0.7
- **躺下**：身体水平 + 腿不伸展 < 0.3 + 头部低 < 0.4
- **坐着**：身体倾斜（30°~70°）+ 腿部分伸展（0.3~0.6）

---

## 实施步骤

### 步骤 1：获取 YOLOv8-Pose 模型

#### 选项 A：使用预训练的 COCO 模型（快速测试）

```bash
# 安装 ultralytics
pip install ultralytics

# 下载并转换模型
python -c "
from ultralytics import YOLO

# 加载 YOLOv8n-Pose 模型（最轻量）
model = YOLO('yolov8n-pose.pt')

# 导出为 TFLite
model.export(format='tflite', imgsz=640)
"
```

**注意**：COCO 模型是为人体训练的，对狗狗效果可能不佳，但可以先测试流程。

#### 选项 B：使用 AP-10K 动物数据集微调（推荐）

```bash
# 1. 下载 AP-10K 数据集
# https://github.com/AlexTheBad/AP-10K

# 2. 准备 YOLO 格式的数据
# 将 AP-10K 转换为 YOLO-Pose 格式

# 3. 微调模型
from ultralytics import YOLO

model = YOLO('yolov8n-pose.pt')
model.train(
    data='ap10k.yaml',  # 数据集配置
    epochs=50,
    imgsz=640,
    batch=16
)

# 4. 导出为 TFLite
model.export(format='tflite', imgsz=640)
```

#### 选项 C：使用自定义狗狗数据集（最佳效果）

1. 收集 200-500 张狗狗图片（不同品种、角度、姿态）
2. 使用 [LabelImg](https://github.com/HumanSignal/labelImg) 或 [CVAT](https://www.cvat.ai/) 标注关键点
3. 定义关键点：
   - 0: 鼻子
   - 1: 左眼
   - 2: 右眼
   - 3: 左耳
   - 4: 右耳
   - 5: 颈部
   - 6: 前左爪
   - 7: 前右爪
   - 8: 后左爪
   - 9: 后右爪
   - 10: 尾巴根部
   - 11: 尾巴末端
4. 训练并导出模型

---

### 步骤 2：将模型添加到 Android 项目

```bash
# 将生成的 .tflite 文件复制到 assets 目录
cp yolov8n-pose.tflite PetBehaviorAnalyzer/app/src/main/assets/
```

---

### 步骤 3：集成到 PetDetector

修改 `PetDetector.kt`，添加 YOLOv8-Pose 检测：

```kotlin
class PetDetector(context: Context) {
    private val detector: ObjectDetector
    private val moodClassifier: PetMoodClassifier
    private val poseDetector: PetPoseDetector
    private val yoloPoseDetector: YoloPoseDetector  // 新增
    
    init {
        // ... 现有初始化代码
        yoloPoseDetector = YoloPoseDetector(context)
    }
    
    private fun analyzeBehavior(): PetBehaviorType {
        // ... 现有运动检测逻辑
        
        // 静止状态：优先使用 YOLO 关键点检测
        if (avgMovement <= MOVEMENT_SLEEP) {
            val currentBitmap = getCurrentFrameBitmap()  // 需要保存当前帧
            val poseResult = yoloPoseDetector.detectPose(currentBitmap)
            
            if (poseResult != null && poseResult.confidence > 0.5f) {
                val analysis = yoloPoseDetector.analyzePoseFromKeypoints(poseResult)
                if (analysis.confidence > 0.6f) {
                    return analysis.posture
                }
            }
            
            // 降级到几何特征分析
            return poseDetector.analyzeEnhancedPosture(...).posture
        }
        
        // ... 其他逻辑
    }
}
```

---

### 步骤 4：调整关键点索引

根据实际使用的模型，修改 `YoloPoseDetector.kt` 中的 `KeypointIndex`：

```kotlin
object KeypointIndex {
    // 如果使用 COCO 模型（人体）
    const val NOSE = 0
    const val LEFT_EYE = 1
    // ...
    
    // 如果使用 AP-10K 或自定义模型（动物）
    // 需要根据数据集的关键点定义调整
}
```

---

## 模型资源

### 推荐的预训练模型

1. **YOLOv8-Pose (Ultralytics)**
   - 官网：https://docs.ultralytics.com/tasks/pose/
   - GitHub：https://github.com/ultralytics/ultralytics
   - 支持导出 TFLite

2. **AP-10K 数据集**
   - GitHub：https://github.com/AlexTheBad/AP-10K
   - 论文：https://arxiv.org/abs/2108.12617
   - 包含 10,000+ 动物图片，54 种动物

3. **Animal-Pose-Dataset**
   - GitHub：https://github.com/noahcao/animal-pose-dataset
   - 5 种动物（狗、猫、牛、马、羊）

### 其他工具

- **DeepLabCut**：科研级动物姿态追踪
- **SLEAP**：社交动物姿态估计
- **MMPose**：OpenMMLab 姿态估计工具箱

---

## 性能对比

| 方案 | 精度 | 速度 | 模型大小 | 实施难度 |
|------|------|------|----------|----------|
| 几何特征分析 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 0 MB | 简单 ✅ |
| YOLOv8n-Pose (COCO) | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ~6 MB | 中等 |
| YOLOv8n-Pose (微调) | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ~6 MB | 中等 |
| YOLOv8s-Pose (微调) | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ~22 MB | 中等 |

---

## 测试建议

1. **先测试几何特征方案**：已集成，无需额外工作
2. **如果效果不佳**：
   - 下载 YOLOv8n-Pose COCO 模型快速测试
   - 如果关键点检测有效，再考虑用动物数据集微调
3. **收集失败案例**：记录哪些姿态识别不准，用于后续优化

---

## 下一步行动

- [ ] 测试当前几何特征方案的实际效果
- [ ] 下载 YOLOv8n-Pose 模型并转换为 TFLite
- [ ] 将模型文件添加到 `app/src/main/assets/`
- [ ] 测试 YOLO 关键点检测效果
- [ ] 根据效果决定是否需要微调模型
- [ ] 调整关键点判断逻辑的阈值参数

---

## 联系与支持

如果需要帮助：
1. 模型转换问题 → 查看 Ultralytics 文档
2. 关键点标注 → 使用 CVAT 或 LabelImg
3. 训练问题 → 参考 YOLOv8 官方教程

Sources:
- YOLOv8 Documentation: https://docs.ultralytics.com
- AP-10K Dataset: https://github.com/AlexTheBad/AP-10K
- TensorFlow Lite: https://www.tensorflow.org/lite
