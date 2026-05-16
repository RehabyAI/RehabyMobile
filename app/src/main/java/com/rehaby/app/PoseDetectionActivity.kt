package com.rehaby.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.rehaby.app.data.ExerciseRepository
import com.rehaby.app.databinding.ActivityPoseDetectionBinding
import com.rehaby.app.model.SessionResult
import com.rehaby.app.pose.PoseLandmarkerHelper
import com.rehaby.app.pose.PoseValidator
import com.rehaby.app.pose.ValidationOutput
import com.rehaby.app.util.MpImageUtils
import com.rehaby.app.util.VoiceFeedbackManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

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

    private val useFrontCamera = false
    private val lastFrameTimestampNs = AtomicLong(0L)

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

        binding.correctPoseImage.setImageResource(correctImageRes)
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
                lastFrameTimestampNs.set(imageProxy.imageInfo.timestamp)
                val mpImage = MpImageUtils.imageProxyToMpImage(imageProxy, useFrontCamera)
                if (mpImage != null) {
                    poseLandmarkerHelper.detect(mpImage, imageProxy.imageInfo.timestamp)
                }
                imageProxy.close()
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

        startActivity(
            Intent(this, SessionSummaryActivity::class.java).apply {
                putExtra(SessionSummaryActivity.EXTRA_EXERCISE_NAME, result.exerciseName)
                putExtra(SessionSummaryActivity.EXTRA_FORM_ACCURACY, result.formAccuracyPercent)
                putExtra(SessionSummaryActivity.EXTRA_REPS_COMPLETED, result.totalReps)
                putExtra(SessionSummaryActivity.EXTRA_DURATION_SEC, result.durationSeconds)
            }
        )
        finish()
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
