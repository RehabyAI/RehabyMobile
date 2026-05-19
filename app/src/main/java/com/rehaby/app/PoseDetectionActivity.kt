package com.rehaby.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import coil.load
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.rehaby.app.data.ExerciseRepository
import com.rehaby.app.data.remote.ApiModule
import com.rehaby.app.data.remote.CreateSessionRequestDto
import com.rehaby.app.data.remote.FrameAnalysisRequestDto
import com.rehaby.app.databinding.ActivityPoseDetectionBinding
import com.rehaby.app.model.SessionResult
import com.rehaby.app.pose.PoseLandmarkerHelper
import com.rehaby.app.pose.PoseValidator
import com.rehaby.app.pose.ValidationOutput
import com.rehaby.app.util.ImageEncoding
import com.rehaby.app.util.MpImageUtils
import com.rehaby.app.util.SessionManager
import com.rehaby.app.util.VoiceFeedbackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

class PoseDetectionActivity : AppCompatActivity(), PoseLandmarkerHelper.LandmarkerListener {

    private lateinit var binding: ActivityPoseDetectionBinding
    private lateinit var inferenceExecutor: ExecutorService
    private lateinit var poseLandmarkerHelper: PoseLandmarkerHelper
    private lateinit var voiceFeedback: VoiceFeedbackManager
    private lateinit var poseValidator: PoseValidator

    private var exerciseId: String = ""
    private var targetReps = 10
    private var holdSeconds = 0
    private var exerciseSets = 1
    private var exerciseName = ""
    private var correctImageRes = 0

    private var sessionStartMs = 0L
    private val feedbackSeen = linkedSetOf<String>()
    private var finished = false
    @Volatile
    private var sessionEnding = false

    private val analysisScores = Collections.synchronizedList(mutableListOf<Int>())
    private val apiFeedbackForErrors = Collections.synchronizedList(mutableListOf<String>())

    private val useFrontCamera = false
    private val lastFrameTimestampNs = AtomicLong(0L)
    private val lastFrameAnalysisMs = AtomicLong(0L)

    private var cameraPermissionGranted = false
    private var landmarkerReady = false
    private var cameraStarted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPoseDetectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        exerciseId = intent.getStringExtra(EXTRA_EXERCISE_ID) ?: run {
            finish()
            return
        }
        val exercise = ExerciseRepository.getById(exerciseId) ?: run {
            finish()
            return
        }

        targetReps = exercise.reps
        holdSeconds = exercise.holdSeconds
        exerciseSets = exercise.sets
        exerciseName = exercise.name
        correctImageRes = exercise.correctPoseImageResId

        poseValidator = PoseValidator(exerciseId, holdSeconds)
        voiceFeedback = VoiceFeedbackManager(this)
        poseLandmarkerHelper = PoseLandmarkerHelper(this, this)
        inferenceExecutor = Executors.newSingleThreadExecutor()
        inferenceExecutor.execute {
            try {
                poseLandmarkerHelper.setupBlocking()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.pose_init_failed, e.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
                return@execute
            }
            runOnUiThread {
                if (isFinishing) return@runOnUiThread
                landmarkerReady = true
                tryStartCamera()
            }
        }

        sessionStartMs = System.currentTimeMillis()

        binding.correctPoseImage.load(exercise.referenceImageUrl) {
            placeholder(correctImageRes)
            error(correctImageRes)
            crossfade(true)
        }
        binding.correctPoseCard.visibility = View.GONE
        binding.alertBanner.visibility = View.GONE

        updateProgressUI(ValidationOutput(true, "", 0, 100, 0L, holdSeconds * 1000L))

        binding.closeButton.setOnClickListener { finish() }
        binding.pauseButton.setOnClickListener {
            Toast.makeText(this, R.string.pause_toast, Toast.LENGTH_SHORT).show()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionGranted = true
            tryStartCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQ_CAM)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_CAM && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionGranted = true
            tryStartCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun tryStartCamera() {
        if (!cameraPermissionGranted || !landmarkerReady || cameraStarted || isFinishing) return
        cameraStarted = true
        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()

            analysis.setAnalyzer(inferenceExecutor) { imageProxy ->
                val ts = imageProxy.imageInfo.timestamp
                lastFrameTimestampNs.set(ts)

                val bmp = MpImageUtils.buildPipelineBitmap(imageProxy, useFrontCamera) ?: run {
                    imageProxy.close()
                    return@setAnalyzer
                }
                imageProxy.close()

                val now = System.currentTimeMillis()
                val sendApi = !finished && !sessionEnding &&
                    (now - lastFrameAnalysisMs.get() >= FRAME_ANALYSIS_INTERVAL_MS)
                if (sendApi) {
                    lastFrameAnalysisMs.set(now)
                }

                val apiBitmap = if (sendApi) {
                    bmp.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    null
                }

                val mpImage = BitmapImageBuilder(bmp).build()
                poseLandmarkerHelper.detect(mpImage, ts)
                bmp.recycle()

                if (apiBitmap != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        var toRecycle = apiBitmap
                        try {
                            val b64 = ImageEncoding.toJpegBase64(toRecycle)
                            val resp = ApiModule.rehabApi.analyzeFrame(
                                FrameAnalysisRequestDto(image_base64 = b64)
                            )
                            if (resp.isSuccessful && resp.body() != null) {
                                val body = resp.body()!!
                                synchronized(analysisScores) { analysisScores.add(body.score) }
                                if (!body.status.equals("Good", ignoreCase = true) &&
                                    body.feedback.isNotBlank()
                                ) {
                                    synchronized(apiFeedbackForErrors) {
                                        if (body.feedback !in apiFeedbackForErrors) {
                                            apiFeedbackForErrors.add(body.feedback)
                                        }
                                    }
                                }
                            }
                        } catch (_: Exception) {
                            // ignore individual frame failures
                        } finally {
                            if (!toRecycle.isRecycled) {
                                toRecycle.recycle()
                            }
                        }
                    }
                }
            }

            val selector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, selector, preview, analysis)
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, getString(R.string.camera_failed, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onResults(result: PoseLandmarkerResult, imageWidth: Int, imageHeight: Int) {
        val landmarks = result.landmarks().firstOrNull() ?: return
        val output = poseValidator.validate(landmarks, lastFrameTimestampNs.get())

        runOnUiThread {
            if (isFinishing || finished) return@runOnUiThread

            val badJoints = badJointsFor(exerciseId, output.isCorrect)
            binding.skeletonOverlay.update(
                result,
                imageWidth,
                imageHeight,
                badJoints,
                mirrorOverlayX = useFrontCamera
            )

            if (!output.isCorrect) {
                binding.alertBanner.visibility = View.VISIBLE
                binding.alertText.text = output.feedbackMessage
                binding.correctPoseCard.visibility = View.VISIBLE
                voiceFeedback.speak(output.feedbackMessage)
                if (output.feedbackMessage.isNotBlank()) {
                    feedbackSeen.add(output.feedbackMessage)
                }
            } else {
                binding.alertBanner.visibility = View.GONE
                binding.correctPoseCard.visibility = View.GONE
            }

            updateProgressUI(output)

            val repsDone = holdSeconds == 0 && output.repCount >= targetReps
            val holdDone = holdSeconds > 0 && output.accumulatedCorrectHoldMs >= output.holdTargetMs
            if (repsDone || holdDone) {
                finishSession(output)
            }
        }
    }

    override fun onError(error: String) {
        runOnUiThread {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateProgressUI(output: ValidationOutput) {
        if (holdSeconds > 0) {
            val sec = (output.accumulatedCorrectHoldMs / 1000L).toInt().coerceAtMost(holdSeconds)
            binding.repCounterText.text = getString(R.string.hold_progress, sec, holdSeconds)
            binding.repSubtext.text = getString(R.string.hold_subtitle, exerciseSets)
        } else {
            binding.repCounterText.text = getString(R.string.rep_progress, output.repCount, targetReps)
            binding.repSubtext.text = getString(R.string.rep_subtitle, exerciseSets)
        }
        binding.formAccuracyText.text = getString(R.string.percent_value, output.formAccuracy)
    }

    private fun finishSession(output: ValidationOutput) {
        if (finished) return
        finished = true
        sessionEnding = true

        val durationSec = (System.currentTimeMillis() - sessionStartMs) / 1000L
        val totalRepsMetric = if (holdSeconds > 0) holdSeconds else output.repCount
        val correctReps = (totalRepsMetric * output.formAccuracy / 100).coerceAtLeast(0)

        val result = SessionResult(
            exerciseId = exerciseId,
            exerciseName = exerciseName,
            totalReps = totalRepsMetric,
            correctReps = correctReps,
            formAccuracyPercent = output.formAccuracy,
            durationSeconds = durationSec,
            feedbackGiven = feedbackSeen.toList()
        )

        lifecycleScope.launch {
            val apiAvg = synchronized(analysisScores) {
                if (analysisScores.isEmpty()) {
                    0
                } else {
                    analysisScores.average().roundToInt().coerceIn(0, 100)
                }
            }

            val errorsDetected = synchronized(apiFeedbackForErrors) {
                (apiFeedbackForErrors + feedbackSeen).distinct().take(50)
            }

            val feedbackSummary = buildString {
                append(getString(R.string.session_feedback_avg_api, apiAvg))
                if (feedbackSeen.isNotEmpty()) {
                    append(" ")
                    append(feedbackSeen.joinToString("; ").take(400))
                }
            }.take(2000)

            withContext(Dispatchers.IO) {
                try {
                    ApiModule.rehabApi.createSession(
                        CreateSessionRequestDto(
                            patient_id = SessionManager.getPatientId(this@PoseDetectionActivity).orEmpty(),
                            exercise_name = exerciseName,
                            rep_count = totalRepsMetric,
                            average_score = apiAvg,
                            errors_detected = errorsDetected,
                            feedback_summary = feedbackSummary
                        )
                    )
                } catch (_: Exception) {
                    // session sync optional — still show summary
                }
            }

            val displayAccuracy = if (apiAvg > 0) {
                apiAvg
            } else {
                output.formAccuracy
            }

            startActivity(
                Intent(this@PoseDetectionActivity, SessionSummaryActivity::class.java).apply {
                    putExtra(SessionSummaryActivity.EXTRA_EXERCISE_NAME, result.exerciseName)
                    putExtra(SessionSummaryActivity.EXTRA_FORM_ACCURACY, displayAccuracy)
                    putExtra(SessionSummaryActivity.EXTRA_REPS_COMPLETED, result.totalReps)
                    putExtra(SessionSummaryActivity.EXTRA_DURATION_SEC, result.durationSeconds)
                }
            )
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inferenceExecutor.execute { poseLandmarkerHelper.close() }
        inferenceExecutor.shutdown()
        voiceFeedback.shutdown()
    }

    companion object {
        const val EXTRA_EXERCISE_ID = "exerciseId"
        private const val REQ_CAM = 1001
        private const val FRAME_ANALYSIS_INTERVAL_MS = 500L
    }
}

private fun badJointsFor(exerciseId: String, isCorrect: Boolean): Set<Int> {
    if (isCorrect) return emptySet()
    return when (exerciseId) {
        "knee_extension" -> setOf(24, 26, 28)
        "hip_abduction" -> setOf(23, 24, 25, 26)
        "wall_squat" -> setOf(12, 24, 26, 28)
        else -> setOf(25, 26, 27, 28)
    }
}
