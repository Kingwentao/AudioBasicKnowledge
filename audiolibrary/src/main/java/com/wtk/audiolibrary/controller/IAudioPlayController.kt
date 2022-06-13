package com.wtk.audiolibrary.controller

/**
 * author: WentaoKing
 * created on: 2022/5/30
 * description: 音频播放控制器接口层
 */
interface IAudioPlayController {

    /**
     * 开始播放
     * @param filePath 播放的文件路径
     */
    fun startPlay(filePath: String): Boolean

    /**
     * 暂停播放
     */
    fun pausePlay(): Boolean

    /**
     * 恢复播放
     */
    fun resumePlay(): Boolean

    /**
     * 停止播放
     */
    fun stopPlay(): Boolean

    /**
     * 跳转到目的地播放
     * @param milliseconds 毫秒
     */
    fun seek(milliseconds: Int): Boolean

    /**
     * 释放资源
     */
    fun release()


    interface OnPlayListener {
        /**
         * 播放中
         * @param curPosition 当前位置
         * @param duration 总时长
         */
        fun onPlaying(curPosition: Int, duration: Int)

        /**
         * 暂停播放
         * @param curPosition 当前位置
         * @param duration 总时长
         */
        fun onPause(curPosition: Int, duration: Int)

        /**
         * 完成播放
         * @param duration 总时长
         */
        fun onComplete(duration: Int)
    }

}