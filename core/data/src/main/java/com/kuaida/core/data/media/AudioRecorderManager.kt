package com.kuaida.core.data.media

import android.content.Context
import android.media.MediaRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorderManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null

    fun startRecording(): File {
        discardCurrent()
        val file = File(context.cacheDir, "qa_recording_${System.currentTimeMillis()}.m4a")
        @Suppress("DEPRECATION")
        val mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        currentFile = file
        return file
    }

    fun pauseRecording() {
        requireNotNull(recorder) { "录音尚未开始。" }.pause()
    }

    fun resumeRecording() {
        requireNotNull(recorder) { "录音尚未开始。" }.resume()
    }

    fun stopRecording(): File? {
        val file = currentFile
        runCatching { recorder?.stop() }
        stopAndRelease()
        return file
    }

    fun discardCurrent() {
        runCatching { recorder?.stop() }
        stopAndRelease()
        currentFile?.delete()
        currentFile = null
    }

    private fun stopAndRelease() {
        recorder?.release()
        recorder = null
    }
}
