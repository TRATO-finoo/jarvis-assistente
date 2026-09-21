package com.tratofino.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var talkButton: Button

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var textToSpeech: TextToSpeech

    private val microphoneRequestCode = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(createInterface())

        textToSpeech = TextToSpeech(this, this)

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text = "Reconhecimento de voz não disponível neste aparelho."
            talkButton.isEnabled = false
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                statusText.text = "Estou ouvindo..."
            }

            override fun onBeginningOfSpeech() {
                statusText.text = "Pode falar..."
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                statusText.text = "Entendi. Processando..."
            }

            override fun onError(error: Int) {
                statusText.text = "Não consegui entender. Tente novamente."
                talkButton.isEnabled = true
            }

            override fun onResults(results: Bundle?) {
                val matches =
                    results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val spokenText = matches?.firstOrNull()

                if (!spokenText.isNullOrBlank()) {
                    resultText.text = "Você: $spokenText"

                    respondToUser(spokenText)
                }

                talkButton.isEnabled = true
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        talkButton.setOnClickListener {
            startListening()
        }
    }

    private fun createInterface(): android.view.View {

        val layout = android.widget.LinearLayout(this)

        layout.orientation = android.widget.LinearLayout.VERTICAL
        layout.setPadding(40, 60, 40, 40)

        statusText = TextView(this)
        statusText.text = "Olá! Eu sou o Jarvis."
        statusText.textSize = 26f

        resultText = TextView(this)
        resultText.text = "Fale comigo."
        resultText.textSize = 20f
        resultText.setPadding(0, 40, 0, 40)

        talkButton = Button(this)
        talkButton.text = "🎤 FALAR COM JARVIS"
        talkButton.textSize = 18f

        layout.addView(statusText)
        layout.addView(resultText)
        layout.addView(talkButton)

        return layout
    }

    private fun startListening() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                microphoneRequestCode
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "pt-BR"
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Fale com o Jarvis"
        )

        talkButton.isEnabled = false
        speechRecognizer?.startListening(intent)
    }

    private fun respondToUser(text: String) {

        val normalized = text.lowercase(Locale.getDefault())

        val response = when {
            normalized.contains("oi") ||
            normalized.contains("olá") -> {
                "Olá! Estou aqui. Como posso ajudar você?"
            }

            normalized.contains("quem é você") -> {
                "Eu sou o Jarvis, seu assistente pessoal."
            }

            normalized.contains("como você está") -> {
                "Estou funcionando e pronto para ajudar."
            }

            else -> {
                "Entendi você dizer: $text. Ainda estou aprendendo novas funções."
            }
        }

        statusText.text = "Jarvis:"
        resultText.text = response

        speak(response)
    }

    private fun speak(text: String) {

        if (::textToSpeech.isInitialized) {
            textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "jarvis-response"
            )
        }
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {

            val result = textToSpeech.setLanguage(
                Locale("pt", "BR")
            )

            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                statusText.text = "Voz em português não disponível."
            }
        }
    }

    override fun onDestroy() {

        speechRecognizer?.destroy()

        if (::textToSpeech.isInitialized) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }

        super.onDestroy()
    }
}
