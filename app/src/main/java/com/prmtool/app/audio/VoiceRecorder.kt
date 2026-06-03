package com.prmtool.app.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/**
 * Records a single voice note to an .m4a (AAC) file.
 * Recording starts on [start] and only stops when [stop] is called.
 */
class VoiceRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean get() = recorder != null

    fun start(clientId: String): File {
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val file = File(dir, "$clientId.m4a")

        @Suppress("DEPRECATION")
        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        rec.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128_000)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = rec
        outputFile = file
        return file
    }

    /** Stops recording. Returns the finished file, or null if it failed (and cleans up). */
    fun stop(): File? {
        val rec = recorder ?: return null
        return try {
            rec.stop()
            outputFile
        } catch (e: RuntimeException) {
            // stop() throws if no valid audio was captured (e.g. too short).
            outputFile?.delete()
            null
        } finally {
            rec.release()
            recorder = null
            outputFile = null
        }
    }

    /** Abort without keeping the file. */
    fun cancel() {
        try {
            recorder?.stop()
        } catch (_: RuntimeException) {
        } finally {
            recorder?.release()
            recorder = null
            outputFile?.delete()
            outputFile = null
        }
    }
}
