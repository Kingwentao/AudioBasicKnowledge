package com.wtk.audiolibrary.controller

import android.media.*
import android.os.Handler
import android.os.Looper
import android.util.ArraySet
import android.util.Log
import java.io.File
import java.lang.Exception
import java.lang.IllegalStateException
import java.util.*

/**
 * author: WentaoKing
 * created on: 2022/5/27
 * description: 音频录制控制器
 */
class AudioPlayController : IAudioPlayController {

    companion object {
        private const val TAG = "AudioPlayController"

        // 录制段时长
        private const val RECORD_PERIOD = 1000L
    }

    // 录制器
    private var mMediaRecorder = MediaRecorder()

    // 播放器
    private val mMediaPlayer = MediaPlayer()

    // 正在录制
    private var isRecording: Boolean = false

    // 当前录制的时长
    private var mCurRecordDuration = 0L

    private var mPlayTimerTask: TimerTask? = null

    private var mPlayTimer: Timer? = null

    var mOnPlayListenerList = ArraySet<IAudioPlayController.OnPlayListener>()

    private val mMainHandler = Handler(Looper.getMainLooper())

    init {
        mMediaPlayer.setOnCompletionListener {
            Log.d(TAG, "complete play!")
            // 完成会多次回调，判断时长确定是否真的完成
            if (it.currentPosition == it.duration) {
                Log.d(TAG, "currentPosition: ${it.currentPosition}")
                for (listener in mOnPlayListenerList) {
                    listener.onComplete(it.duration)
                }
                cancelPlayTimer()
            }
        }

        mMediaPlayer.setOnBufferingUpdateListener { mp, percent ->
            Log.d(TAG, "play $percent ")
        }

        mMediaPlayer.setOnSeekCompleteListener {
            Log.d(TAG, "Seek Complete currentPosition: ${it.currentPosition}")
        }
    }

    override fun startPlay(filePath: String): Boolean {
        if (isRecording) {
            Log.e(TAG, "playRecord: current is recording cant play!")
            return false
        }
        if (mMediaPlayer.isPlaying) {
            Log.e(TAG, "playRecord: current is playing cant play!")
            return false
        }
        if (!File(filePath).exists()) {
            Log.e(TAG, "playRecord: file not exist cant play!")
            return false
        }
        Log.d(
            TAG,
            "playRecord: path is $filePath currentPosition: ${mMediaPlayer.currentPosition}"
        )
        try {
            mMediaPlayer.reset()
            // 设置数据源
            mMediaPlayer.setDataSource(filePath)
            // 开始前必须准备
            mMediaPlayer.prepare()
            mMediaPlayer.start()
            startPlayTimerTask()
            Log.d(TAG, "playRecord: duration: ${mMediaPlayer.duration}")
        } catch (e: Exception) {
            Log.e(TAG, "play exception: ${e.message}")
            e.printStackTrace()
            return false
        }
        return true
    }

    override fun pausePlay(): Boolean {
        if (isRecording) {
            Log.e(TAG, "playRecord: current is recording cant pause!")
            return false
        }
        if (!mMediaPlayer.isPlaying) {
            Log.e(TAG, "playRecord: current is not playing cant pause!")
            return false
        }
        try {
            mMediaPlayer.pause()
            cancelPlayTimer()
            for (listener in mOnPlayListenerList) {
                listener.onPause(
                    mMediaPlayer.currentPosition,
                    mMediaPlayer.duration
                )
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "pause exception: ${e.message}")
            e.printStackTrace()
            return false
        }
        Log.d(TAG, "pausePlay: pause cur time is ${mMediaPlayer.currentPosition}")
        return true
    }

    override fun resumePlay(): Boolean {
        mMediaPlayer.start()
        startPlayTimerTask()
        return true
    }

    override fun stopPlay(): Boolean {
        if (!mMediaPlayer.isPlaying) {
            Log.e(TAG, "stopPlay: current is not playing cant stop!")
            return false
        }
        try {
            mMediaPlayer.stop()
            cancelPlayTimer()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "stop exception: ${e.message}")
            e.printStackTrace()
            return false
        }
        return true
    }

    override fun seek(milliseconds: Int): Boolean {
        Log.d(TAG, "seek before: $milliseconds cur: ${mMediaPlayer.currentPosition}")
        try {
            mMediaPlayer.seekTo(milliseconds)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "seek exception: ${e.message}")
            return false
        }
        Log.d(TAG, "seek after: $milliseconds cur: ${mMediaPlayer.currentPosition}")
        if (!mMediaPlayer.isPlaying) {
            Log.d(TAG, "seek: start play")
            mMediaPlayer.start()
            startPlayTimerTask()
        }
        return true
    }

    /**
     * 释放资源，释放资源后无法再次播放
     * note：确定不用才能调用该方法，调用后再次播放需要重新创建对象
     */
    override fun release() {
        if (mMediaPlayer.isPlaying) {
            stopPlay()
        }
        mMediaPlayer.release()
        mMediaRecorder.release()
        cancelPlayTimer()
        mCurRecordDuration = 0
    }

    private fun startPlayTimerTask() {
        val playTimer = Timer()
        val task = object : TimerTask() {
            override fun run() {
                mMainHandler.post {
                    for (listener in mOnPlayListenerList) {
                        listener.onPlaying(mMediaPlayer.currentPosition, mMediaPlayer.duration)
                    }
                }
            }
        }
        mPlayTimerTask = task
        playTimer.schedule(task, 0, RECORD_PERIOD)
        mPlayTimer = playTimer
    }

    private fun cancelPlayTimer() {
        mPlayTimerTask?.cancel()
        mPlayTimer?.cancel()
    }

}