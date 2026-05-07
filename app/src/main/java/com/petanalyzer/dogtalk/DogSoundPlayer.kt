package com.petanalyzer.dogtalk

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 使用 MediaPlayer 播放 assets/sounds/ 目录下的真实狗叫音频
 *
 * 用户需要将下载的狗叫 WAV/MP3 文件放入 app/src/main/assets/sounds/：
 *   friendly_bark.wav  - 友好问候
 *   play_invite.wav    - 玩耍邀请
 *   food_call.wav      - 食物召唤
 *   come_here.wav      - 呼唤过来
 *   warning.wav        - 警告制止
 *   comfort.wav        - 安抚安慰
 *   praise.wav         - 兴奋夸奖
 *   attention.wav      - 吸引注意
 *
 * 推荐音源（学术数据集，免费）：
 *   ETH Zurich AE Dataset: https://data.vision.ee.ethz.ch/cvl/ae_dataset/
 *   HuggingFace dog-dataset: https://huggingface.co/datasets/437aewuh/dog-dataset
 *   BarkMeowDB (Zenodo): https://zenodo.org/record/3563990
 *   NYU UrbanSound8K: https://urbansounddataset.weebly.com/download-urbansound8k.html
 */
class DogSoundPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null

    suspend fun play(barkType: BarkType) = withContext(Dispatchers.IO) {
        stop()

        try {
            val afd = context.assets.openFd(barkType.assetFileName)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setOnCompletionListener { release() }
                prepare()
                start()
            }
        } catch (e: Exception) {
            // 音频文件不存在或格式不支持
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun stop() {
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {}
        release()
    }

    private fun release() {
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }
}
