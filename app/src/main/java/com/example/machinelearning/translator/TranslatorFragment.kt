package com.example.machinelearning.translator

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.InputQueue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.fragment.app.Fragment
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.example.machinelearning.R
import com.example.machinelearning.databinding.FragmentTranslatorBinding
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

class TranslatorFragment : Fragment() {

    private lateinit var binding: FragmentTranslatorBinding

    private val languageCodes = mapOf(
        "English" to "en",
        "Chinese" to "zh-CN",
        "Korean" to "ko",
        "Japanese" to "ja",
        "German" to "de",
        "French" to "fr",
        "Vietnamese" to "vi"
    )

    private val languages = mapOf(
        "English" to TranslateLanguage.ENGLISH,
        "Chinese" to TranslateLanguage.CHINESE,
        "Korean" to TranslateLanguage.KOREAN,
        "Japanese" to TranslateLanguage.JAPANESE,
        "German" to TranslateLanguage.GERMAN,
        "French" to TranslateLanguage.FRENCH,
        "Vietnamese" to TranslateLanguage.VIETNAMESE,
    )
    private lateinit var adapter: ArrayAdapter<String>

    private lateinit var selectedLanguage: String
    private lateinit var translatedLanguage: String

    private val conditions = DownloadConditions.Builder().requireWifi().build()

    private var translator: Translator? = null

    private var exoPlayer: ExoPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTranslatorBinding.inflate(inflater, container, false)

        adapter = ArrayAdapter(
            requireContext(),
            androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
            languages.keys.toList()
        )

        binding.apply {
            leftSpinner.adapter = adapter
            rightSpinner.adapter = adapter

            setupSpinnerListener(leftSpinner) {
                selectedLanguage = it
            }

            setupSpinnerListener(rightSpinner) {
                translatedLanguage = it
            }

            flCamera.setOnClickListener {
                parentFragmentManager.beginTransaction().apply {
                    replace(R.id.flMainLayout, TextRecognitionFragment())
                    addToBackStack(null)
                    commit()
                }
            }
        }

        setupTextListener()

        exoPlayer = ExoPlayer.Builder(requireContext()).build()

        binding.ivSpeaker.setOnClickListener {
            Log.i("TEST", languageCodes[translatedLanguage]!!)
            playTranslatedTextAudio(
                language = languageCodes[translatedLanguage],
                text = binding.tvTranslatedText.text.toString()
            )
        }

        return binding.root
    }

    @OptIn(UnstableApi::class)
    private fun playTranslatedTextAudio(language: String?, text: String) {
        val url =
            "https://translate.google.com/translate_tts?ie=UTF-8&tl=${language}&client=tw-ob&q=${text}"

        Log.i("TEST", url)

        val headers = mapOf(
            "User-Agent" to "Mozilla/5.0",  // Mimic browser
            "Referer" to ""  // Equivalent to `no-referrer`
        )

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(headers)

        val mediaItem = MediaItem.fromUri(url)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)

        exoPlayer = ExoPlayer.Builder(requireContext()).build()
        exoPlayer?.setMediaSource(mediaSource)
        exoPlayer?.prepare()
        exoPlayer?.play()
    }

    private fun setupTextListener() {
        binding.etInputText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?, start: Int, count: Int, after: Int
            ) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                translateText(binding.etInputText.text.toString())
            }

        })
    }

    private fun translateText(inputText: String) {

        // Create an translator:
        val options = TranslatorOptions.Builder().setSourceLanguage(languages[selectedLanguage]!!)
            .setTargetLanguage(languages[translatedLanguage]!!).build()

        translator = Translation.getClient(options)

        binding.progressBar.visibility = View.VISIBLE

        if (translator != null) {
            translator!!.downloadModelIfNeeded(conditions).addOnSuccessListener {
                translator!!.translate(inputText).addOnSuccessListener { translatedText ->
                    // Translation successful.
                    binding.tvTranslatedText.text = translatedText

                    binding.progressBar.visibility = View.GONE
                }.addOnFailureListener { exception ->
                    // Error.
                    Toast.makeText(
                        requireContext(), exception.message, Toast.LENGTH_LONG
                    ).show()

                    binding.progressBar.visibility = View.GONE
                }
            }.addOnFailureListener {
                Toast.makeText(
                    requireContext(), "Please check your internet connection!", Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun setupSpinnerListener(spinner: Spinner, onItemSelected: (String) -> Unit) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                onItemSelected(selectedItem)
                if (binding.etInputText.text.isNotEmpty()) {
                    translateText(binding.etInputText.text.toString())
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                onItemSelected(parent?.getItemAtPosition(0).toString())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        translator?.close()
    }

}
