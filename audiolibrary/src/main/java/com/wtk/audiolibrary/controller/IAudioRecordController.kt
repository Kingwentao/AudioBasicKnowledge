package com.wtk.audiolibrary.controller

/**
 * author: WentaoKing
 * created on: 2022/5/30
 * description: 音频录制控制器接口
 */
interface IAudioRecordController {

    /**
     * 开始录制
     * @param filePath 录制文件保存的路径
     */
    fun startRecord(filePath: String): Boolean

    /**
     * 暂停录制
     */
    fun pauseRecord(): Boolean

    /**
     * 恢复录制
     */
    fun resumeRecord(): Boolean

    /**
     * 停止录制
     */
    fun stopRecord(): Boolean

    /**
     * 释放资源
     */
    fun release()

    /**
     * 获取当前录制的时长
     */
    fun getCurRecordDuration(): Long


    interface OnRecordListener {

        /**
         * 录制中
         * @param recordedDuration 已经录制的时长
         */
        fun onRecording(recordedDuration: Long)
    }

}