package com.wtk.audiolibrary.controller

import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.*

/**
 * author: WentaoKing
 * created on: 2022/6/9
 * description: MediaPlay实现录制
 */
class MediaRecordController : IAudioRecordController {

    companion object {
        private const val TAG = "MediaRecordController"

        // 录制段时长
        private const val RECORD_PERIOD = 1000L
    }

    // 录制器
    private var mMediaRecorder = MediaRecorder()

    // 正在录制
    private var isRecording: Boolean = false

    // 当前录制的时长
    private var mCurRecordDuration = 0L

    private var mRecordTimerTask: TimerTask? = null

    private var mRecordTimer: Timer? = null

    var mOnRecordListener: IAudioRecordController.OnRecordListener? = null

    private val mMainHandler = Handler(Looper.getMainLooper())

    override fun startRecord(filePath: String): Boolean {
        if (isRecording) {
            Log.e(TAG, "startRecord: current is recording cant start!")
            return false
        }
        Log.d(TAG, "startRecord: $filePath")
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
        mMediaRecorder.setOutputFile(filePath)
        try {
            mMediaRecorder.prepare()
            mMediaRecorder.start()
            isRecording = true
            startRecordTimerTask()
        } catch (e: Exception) {
            Log.e(TAG, "startRecord exception: ${e.message}")
            e.printStackTrace()
            return false
        }
        return true
    }

    override fun pauseRecord(): Boolean {
        if (!isRecording) {
            Log.e(TAG, "pauseRecord fail: current is not recording file cant pause!")
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder.pause()
        } else {
            return false
        }
        Log.d(TAG, "pauseRecord: ")
        isRecording = false
        cancelRecordTimer()
        return true
    }

    override fun resumeRecord(): Boolean {
        if (isRecording) {
            Log.e(TAG, "resumeRecord fail: current is recording cant resume!")
            return false
        }
        Log.d(TAG, "resumeRecord: ")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mMediaRecorder.resume()
        } else {
            return false
        }
        isRecording = true
        startRecordTimerTask()
        return true
    }

    override fun stopRecord(): Boolean {
        if (!isRecording) {
            Log.e(TAG, "stopRecord: current is not recording file cant stop!")
            return false
        }
        Log.d(TAG, "stopRecord: ")
        try {
            mMediaRecorder.stop()
            mMediaRecorder.reset()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "stopRecord exception")
            e.printStackTrace()
            return false
        }
        isRecording = false
        cancelRecordTimer()
        mCurRecordDuration = 0
        return true
    }

    override fun release() {
        if (isRecording) {
            stopRecord()
        }
        mMediaRecorder.release()
        cancelRecordTimer()
        mCurRecordDuration = 0
    }

    override fun getCurRecordDuration(): Long {
        return mCurRecordDuration
    }

    private fun startRecordTimerTask() {
        val recordTimer = Timer()
        val task = object : TimerTask() {
            override fun run() {
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
        mRecordTimerTask?.cancel()
        mRecordTimer?.cancel()
    }
}