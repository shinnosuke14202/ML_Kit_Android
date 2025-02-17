package com.example.machinelearning.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment


object PermissionUtils {

    fun checkCameraPermission(fragment: Fragment): Boolean {
        return ContextCompat.checkSelfPermission(
            fragment.requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestCameraPermission(fragment: Fragment) {
        ActivityCompat.requestPermissions(
            fragment.requireActivity(),
            arrayOf(Manifest.permission.CAMERA),
            1
        )
    }
}
