package com.petanalyzer.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.petanalyzer.analyzer.AnalysisResult
import com.petanalyzer.analyzer.PetDetector
import kotlinx.coroutines.*

/**
 * 相机控制器 - 管理CameraX生命周期和帧分析
 */
class CameraController(private val context: Context) {

    private val TAG = "CameraController"
    val petDetector = PetDetector(context)
    private var analysisScope: CoroutineScope? = null

    // 分析结果回调
    var onAnalysisResult: ((AnalysisResult) -> Unit)? = null

    // 快照回调：检测到可爱行为时触发
    var onSnapshot: ((Bitmap) -> Unit)? = null

    // 控制分析频率
    private var lastAnalysisTime = 0L
    private val minAnalysisIntervalMs = 100L  // 每秒最多分析10帧

    init {
        petDetector.onSnapshotTrigger = { bitmap ->
            onSnapshot?.invoke(bitmap)
        }
    }

    /**
     * 启动相机
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        scope: CoroutineScope,
    ) {
        analysisScope = scope
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // 预览
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                // 图像分析
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(640, 480))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            ContextCompat.getMainExecutor(context),
                            { imageProxy -> processFrame(imageProxy) }
                        )
                    }

                // 选择后置摄像头
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis,
                )

            } catch (e: Exception) {
                Log.e(TAG, "Camera start failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 处理相机帧
     */
    private fun processFrame(imageProxy: ImageProxy) {
        val now = System.currentTimeMillis()
        if (now - lastAnalysisTime < minAnalysisIntervalMs) {
            imageProxy.close()
            return
        }
        lastAnalysisTime = now

        val scope = analysisScope ?: run {
            imageProxy.close()
            return
        }

        scope.launch(Dispatchers.IO) {
            try {
                val result = petDetector.analyze(imageProxy)
                withContext(Dispatchers.Main) {
                    onAnalysisResult?.invoke(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Frame analysis error", e)
            }
        }
    }

    fun cleanup() {
        petDetector.reset()
        analysisScope = null
    }
}
