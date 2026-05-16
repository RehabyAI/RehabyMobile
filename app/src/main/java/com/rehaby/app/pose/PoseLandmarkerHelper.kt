package com.rehaby.app.pose

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.io.File
import java.io.RandomAccessFile
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

/**
 * Copies the .task model from assets to internal storage, memory-maps it, and passes the buffer
 * to MediaPipe. Some devices crash in nativeStartRunningGraph when loading the model from APK
 * assets even with noCompress; a mapped file buffer is more reliable.
 */
class PoseLandmarkerHelper(
    private val context: Context,
    private val listener: LandmarkerListener
) {
    interface LandmarkerListener {
        fun onResults(result: PoseLandmarkerResult, imageWidth: Int, imageHeight: Int)
        fun onError(error: String)
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var poseLandmarker: PoseLandmarker? = null
    private var modelMappedBuffer: MappedByteBuffer? = null

    /**
     * Must run on the same single-thread executor that calls [detect].
     */
    fun setupBlocking() {
        val appContext = context.applicationContext
        val mapped = mapModelFromAssets(appContext)
        modelMappedBuffer = mapped

        val baseOptions = BaseOptions.builder()
            .setModelAssetBuffer(mapped)
            .setDelegate(Delegate.CPU)
            .build()

        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setResultListener { result, image ->
                listener.onResults(result, image.width, image.height)
            }
            .setErrorListener { error ->
                val msg = error.message ?: "Unknown error"
                mainHandler.post { listener.onError(msg) }
            }
            .build()

        poseLandmarker = PoseLandmarker.createFromOptions(appContext, options)
    }

    private fun mapModelFromAssets(appContext: Context): MappedByteBuffer {
        val modelFile = File(appContext.filesDir, "pose_landmarker_lite.task")
        copyModelFromApkAssets(appContext, modelFile)

        val size = modelFile.length()
        require(size > 0L) { "Pose model file is empty" }

        return RandomAccessFile(modelFile, "r").use { raf ->
            val channel = raf.channel
            channel.map(FileChannel.MapMode.READ_ONLY, 0, size)
        }
    }

    /**
     * Tries both packaged paths so older builds (model at assets root) still work.
     */
    private fun copyModelFromApkAssets(appContext: Context, modelFile: File) {
        if (modelFile.exists() && modelFile.length() > 500_000L) {
            return
        }
        if (modelFile.exists()) {
            modelFile.delete()
        }

        val candidates = listOf(
            "models/pose_landmarker_lite.task",
            "pose_landmarker_lite.task"
        )
        var lastError: Throwable? = null
        for (path in candidates) {
            try {
                appContext.assets.open(path).use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                return
            } catch (e: Throwable) {
                lastError = e
            }
        }

        throw IllegalStateException(
            "Pose model not packaged in APK (tried: ${candidates.joinToString()}). " +
                "Sync project and rebuild so Gradle can download it, or place the file under " +
                "app/src/main/assets/models/pose_landmarker_lite.task",
            lastError
        )
    }

    fun detect(image: MPImage, frameTimeNs: Long) {
        val landmarker = poseLandmarker ?: return
        val timestampMs = frameTimeNs / 1_000_000
        try {
            landmarker.detectAsync(image, timestampMs)
        } catch (e: Exception) {
            mainHandler.post {
                listener.onError(e.message ?: "detectAsync failed")
            }
        }
    }

    fun close() {
        poseLandmarker?.close()
        poseLandmarker = null
        modelMappedBuffer = null
    }
}
