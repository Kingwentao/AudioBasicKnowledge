package com.wtk.audiolibrary.controller

import android.media.*
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors

/**
 * author: WentaoKing
 * created on: 2022/5/27
 * description: 音频录制控制器
 */
class AudioRecordController : IAudioRecordController {

    companion object {
        private const val TAG = "AudioRecordController"

        // 采样率16k
        private const val SAMPLE_RATE_InHz = 16000

        // 录制段时长
        private const val RECORD_PERIOD = 1000L
    }

    private val mExecutorService by lazy { Executors.newSingleThreadExecutor() }
    private var mMinReadBufferSize: Int
    private var mMinWriteBufferSize: Int

    private var mAudioRecord: AudioRecord
    private var mAudioTrack: AudioTrack

    private var mRecordTimerTask: TimerTask? = null

    private var mRecordTimer: Timer? = null

    // 正在录制
    private var isRecording: Boolean = false

    private var mCurRecordFilePath: String? = null

    // 当前录制的时长
    private var mCurRecordDuration = 0L

    private val mMainHandler = Handler(Looper.getMainLooper())

    var mOnRecordListener: IAudioRecordController.OnRecordListener? = null

    // 输入声道
    private val mChannelInConfig = AudioFormat.CHANNEL_IN_MONO

    // 输出声道
    private val mChannelOutConfig = AudioFormat.CHANNEL_OUT_MONO

    // 编码格式
    private val mEncodingFormat = AudioFormat.ENCODING_PCM_16BIT

    init {
        // 获取最小的输入缓存
        mMinReadBufferSize =
            AudioRecord.getMinBufferSize(SAMPLE_RATE_InHz, mChannelInConfig, mEncodingFormat)
        // 创建音频录制对象
        mAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE_InHz,
            mChannelInConfig,
            mEncodingFormat,
            mMinReadBufferSize
        )
        // 获取最小的输出缓存
        mMinWriteBufferSize =
            AudioTrack.getMinBufferSize(SAMPLE_RATE_InHz, mChannelOutConfig, mEncodingFormat)
        // 获取媒体格式
        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE_InHz)
            .setEncoding(mEncodingFormat)
            .setChannelMask(mChannelOutConfig)
            .build()
        // 获取媒体属性
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        // 创建播放媒体流对象
        mAudioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            mMinWriteBufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    override fun startRecord(filePath: String): Boolean {
        if (isRecording) {
            Log.e(TAG, "startRecord: current is recording cant start!")
            return false
        }
        Log.d(TAG, "startRecord: ")
        mAudioRecord.startRecording()
        isRecording = true
        mExecutorService.execute {
            writeRecordDataToFile(filePath)
        }
        mCurRecordFilePath = filePath
        startRecordTimerTask()
        return true
    }

    override fun pauseRecord(): Boolean {
        if (!isRecording) {
            Log.e(TAG, "pauseRecord fail: current is not recording file cant pause!")
            return false
        }
        cancelRecordTimer()
        isRecording = false
        return true
    }

    override fun resumeRecord(): Boolean {
        if (isRecording) {
            Log.e(TAG, "resumeRecord fail: current is recording cant resume!")
            return false
        }
        if (mCurRecordFilePath == null) {
            Log.e(TAG, "cur record file is null")
            return false
        }
        isRecording = true
        startRecordTimerTask()
        mCurRecordFilePath?.let {
            writeRecordDataToFile(it)
        }
        return true
    }

    override fun stopRecord(): Boolean {
        if (!isRecording) {
            Log.e(TAG, "stopRecord: current is not recording file cant stop!")
            return false
        }
        Log.d(TAG, "stopRecord: ")
        mAudioRecord.stop()
        isRecording = false
        cancelRecordTimer()
        mCurRecordDuration = 0
        return true
    }

    private fun startPlay(filePath: String): Boolean {
        if (isRecording) {
            Log.e(TAG, "playRecord: current is not recording file cant play!")
            return false
        }
        if (!File(filePath).exists()) {
            Log.e(TAG, "playRecord: file not exist cant play!")
            return false
        }
        mAudioTrack.play()
        Log.d(TAG, "playRecord: path is $filePath")
        mExecutorService.execute {
            val fileInputStream = FileInputStream(filePath)
            val buffer = ByteArray(mMinWriteBufferSize)
            Log.d(TAG, "playRecord  avalible: ${fileInputStream.available()}")
            try {
                while (fileInputStream.available() > 0) {
                    val readCount = fileInputStream.read(buffer)
                    Log.d(
                        TAG,
                        "playRecord...$readCount ${buffer.size} avalible: ${fileInputStream.available()}"
                    )
                    // 如果读到的数据有问题，则跳过本段
                    if (readCount == AudioTrack.ERROR_INVALID_OPERATION || readCount == AudioTrack.ERROR_BAD_VALUE) {
                        Log.e(TAG, "playRecord: read exception $readCount skip this")
                        continue
                    }
                    if (readCount != 0 && readCount != 1) {
                        mAudioTrack.write(buffer, 0, readCount)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "playRecord exception!")
                e.printStackTrace()
            } finally {
                fileInputStream.close()
            }
        }
        return true
    }

    /**
     * 释放资源，释放资源无法再次播放
     * note：确定不用才能调用该方法，调用后再次播放需要重新创建对象
     */
    override fun release() {
        if (isRecording) {
            stopRecord()
        }
        mAudioRecord.release()
        mAudioTrack.release()
    }

    override fun getCurRecordDuration(): Long {
        return mCurRecordDuration
    }

    private fun writeRecordDataToFile(filePath: String) {
        mExecutorService.execute {
            val fileOutputStream = FileOutputStream(File(filePath), true)
            val buffer = ByteArray(mMinReadBufferSize)
            try {
                while (isRecording) {
                    val read = mAudioRecord.read(buffer, 0, mMinReadBufferSize)
                    if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                        fileOutputStream.write(buffer)
                    }
                    Log.d(TAG, "writeRecordDataToFile: ")
                }
                fileOutputStream.close()
            } catch (e: Exception) {
                Log.e(TAG, "writeRecordDataToFile exception")
                e.printStackTrace()
            } finally {
                Log.d(TAG, "writeRecordDataToFile: finally")
                fileOutputStream.close()
            }
        }
    }

    private fun startRecordTimerTask() {
        val recordTimer = Timer()
        val task = object : TimerTask() {
            override fun run() {
                Log.d(TAG, "run...")
                mCurRecordDuration += 1000
                mMainHandler.post {
                    mOnRecordListener?.onRecording(mCurRecordDuration)
                }
            }
        }
        mRecordTimerTask = task
        recordTimer.schedule(task, 0, RECORD_PERIOD)
        mRecordTimer = recordTimer
    }

    private fun cancelRecordTimer() {
        mRecordTimer?.cancel()
        val result = mRecordTimerTask?.cancel()
        Log.d(TAG, "cancelRecordTimer: cancel $result")
    }

}