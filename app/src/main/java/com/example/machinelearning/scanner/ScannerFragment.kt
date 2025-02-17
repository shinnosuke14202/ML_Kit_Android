package com.example.machinelearning.scanner

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraExecutor
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.machinelearning.R
import com.example.machinelearning.databinding.FragmentScannerBinding
import com.example.machinelearning.utils.PermissionUtils
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScannerFragment : Fragment() {

    private lateinit var binding: FragmentScannerBinding

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentScannerBinding.inflate(inflater, container, false)

        if (PermissionUtils.checkCameraPermission(this@ScannerFragment)) {
            startCamera()
        } else {
            val permissionLauncher =
                registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                    if (isGranted) {
                        startCamera()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Camera permission is required to capture images",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()

        return binding.root
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        val screenSize = Size(1280, 720)
        val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(
            ResolutionStrategy(screenSize, ResolutionStrategy.FALLBACK_RULE_NONE)
        ).build()

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().setResolutionSelector(resolutionSelector).build().also {
                it.surfaceProvider = binding.previewView.surfaceProvider
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider?.bindToLifecycle(
                this, cameraSelector, preview, imageAnalyzer
            )
        }, ContextCompat.getMainExecutor(requireContext()))

    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image).addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    handleBarcode(barcode)
                }
            }.addOnFailureListener {
                val failureText = "Failed to scan QR codes!"
                binding.tvResult.text = failureText
            }.addOnCompleteListener {
                imageProxy.close()
            }
        }
    }

    private fun handleBarcode(barcode: Barcode) {
        val url = barcode.url?.url ?: barcode.displayValue
        if (isValidUrl(url)) {
            binding.tvResult.text = url
            binding.tvResult.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction().apply {
                    replace(R.id.flMainLayout, WebViewFragment().apply {
                        arguments = Bundle().apply {
                            putString("URL", url)
                        }
                    })
                    addToBackStack(this@ScannerFragment::class.java.simpleName)
                    commit()
                }
            }
        } else {
            binding.tvResult.text = url
        }
    }

    private fun isValidUrl(url: String?): Boolean {
        return url != null && Patterns.WEB_URL.matcher(url).matches()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdownNow()
        cameraProvider?.unbindAll()
        barcodeScanner.close()
    }

}
