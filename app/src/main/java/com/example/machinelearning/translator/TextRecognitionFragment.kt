package com.example.machinelearning.translator

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.machinelearning.MainActivity
import com.example.machinelearning.databinding.FragmentTextRecognitionBinding
import com.example.machinelearning.utils.PermissionUtils
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume


class TextRecognitionFragment : Fragment() {

    private lateinit var binding: FragmentTextRecognitionBinding
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    private var photoUri: Uri? = null

    private val languages = listOf("English", "Chinese", "Korean", "Japanese", "Vietnamese")
    private lateinit var arrayAdapter: ArrayAdapter<String>

    private var selectedLanguage: String? = null
    private var translatedLanguage: String? = null

    private var translator : Translator? = null
    private val conditions = DownloadConditions.Builder().requireWifi().build()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTextRecognitionBinding.inflate(inflater, container, false)

        (requireActivity() as MainActivity).showOrHideBottomNavigation(0)

        binding.ibBack.setOnClickListener {
            deleteCapturedImage()
            parentFragmentManager.popBackStack()
        }

        setupPermissionLauncher()
        setupCameraLauncher()

        binding.llCaptureImage.setOnClickListener {
            if (PermissionUtils.checkCameraPermission(this@TextRecognitionFragment)) {
                captureImage()
            } else {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        arrayAdapter = ArrayAdapter(
            requireContext(),
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            languages
        )

        binding.leftSpinner.adapter = arrayAdapter
        binding.rightSpinner.adapter = arrayAdapter

        setupSpinnerListener(binding.leftSpinner) {
            selectedLanguage = it
        }

        setupSpinnerListener(binding.rightSpinner) {
            translatedLanguage = it
        }

        return binding.root
    }

    private fun setupSpinnerListener(spinner: Spinner, onItemSelected: (String) -> Unit) {
        spinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                onItemSelected(selectedItem)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                val selectedItem = parent?.getItemAtPosition(0).toString()
                onItemSelected(selectedItem)
            }
        }
    }

    private fun captureImage() {
        val capturePicture = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        val photoFile: File = createImageFile()

        photoUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireActivity().applicationContext.packageName}.provider",
            photoFile
        )

        if (capturePicture.resolveActivity(requireActivity().packageManager) != null) {
            capturePicture.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            cameraLauncher.launch(capturePicture)
        }
    }

    private fun deleteCapturedImage() {
        photoUri?.let {
            val file = File(it.path!!)
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun setupCameraLauncher() {
        cameraLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    binding.llCaptureImage.visibility = View.GONE
                    binding.ivCapture.setImageURI(photoUri)

                    recognizeText()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Unable to capture image",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setupPermissionLauncher() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    captureImage()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Camera permission is required to capture images",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun createImageFile(): File {
        val timestamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timestamp}_", ".jpg", storageDir)
    }

    private fun recognizeText() {
        if (photoUri != null) {
            try {
                val image = InputImage.fromFilePath(requireContext(), photoUri!!)
                val recognizer = TextRecognition.getClient(
                    when (selectedLanguage) {
                        "Chinese" -> ChineseTextRecognizerOptions.Builder().build()
                        "Japanese" -> JapaneseTextRecognizerOptions.Builder().build()
                        "Korean" -> KoreanTextRecognizerOptions.Builder().build()
                        else -> TextRecognizerOptions.DEFAULT_OPTIONS
                    }
                )

                createTranslatorModel()

                recognizer.process(image).addOnSuccessListener {

                    val translatedLines = mutableListOf<String>()

                    lifecycleScope.launch {
                        for (block in it.textBlocks) {
                            for (line in block.lines) {
                                val translatedLine = translateText(line.text)
                                translatedLines.add("${line.text} \n $translatedLine \n \n")
                            }
                        }

                        binding.tvImageText.text = translatedLines.toString()
                        binding.progressBar.visibility = View.GONE
                    }
                }.addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "No texts founded!",
                        Toast.LENGTH_LONG
                    ).show()
                }


            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun createTranslatorModel() {
        // Create an translator:
        val options = TranslatorOptions.Builder().setSourceLanguage(
            when (selectedLanguage) {
                "Chinese" -> TranslateLanguage.CHINESE
                "Korean" -> TranslateLanguage.KOREAN
                "Vietnamese" -> TranslateLanguage.VIETNAMESE
                "Japanese" -> TranslateLanguage.JAPANESE
                else -> {
                    TranslateLanguage.ENGLISH
                }
            }
        )
            .setTargetLanguage(
                when (translatedLanguage) {
                    "Chinese" -> TranslateLanguage.CHINESE
                    "Korean" -> TranslateLanguage.KOREAN
                    "Vietnamese" -> TranslateLanguage.VIETNAMESE
                    "Japanese" -> TranslateLanguage.JAPANESE
                    else -> {
                        TranslateLanguage.ENGLISH
                    }
                }
            ).build()

        translator = Translation.getClient(options)

        binding.progressBar.visibility = View.VISIBLE
    }

    private suspend fun translateText(inputText: String): String {
        return suspendCancellableCoroutine { continuation ->
            translator?.downloadModelIfNeeded(conditions)?.addOnSuccessListener {
                translator?.translate(inputText)?.addOnSuccessListener {
                    // Translation successful.
                    if (continuation.isActive) {
                        continuation.resume(it)
                    }

                }?.addOnFailureListener { exception ->
                    // Handle translation error
                    if (continuation.isActive) {
                        if (isAdded) {
                            Toast.makeText(
                                requireContext(), exception.message, Toast.LENGTH_LONG
                            ).show()
                        }
                        continuation.cancel(Throwable("Error in translating text!", exception))
                    }
                }
            }?.addOnFailureListener {
                // Handle model download error
                if (continuation.isActive) {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Please check your internet connection!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    continuation.cancel(Throwable("No internet connection!", it))
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deleteCapturedImage()
        translator?.close()
        (requireActivity() as MainActivity).showOrHideBottomNavigation(1)
    }

}
