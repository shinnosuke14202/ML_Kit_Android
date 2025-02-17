package com.example.machinelearning.scanner

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebViewClient
import com.example.machinelearning.R
import com.example.machinelearning.databinding.FragmentScannerBinding
import com.example.machinelearning.databinding.FragmentWebViewBinding

class WebViewFragment : Fragment() {

    private lateinit var binding: FragmentWebViewBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = FragmentWebViewBinding.inflate(inflater, container, false)

        val url = arguments?.getString("URL")

        binding.webView.webViewClient = WebViewClient()
        binding.webView.settings.javaScriptEnabled = true

        if (url != null) {
            binding.webView.loadUrl(url)
        }

        return binding.root
    }

}
