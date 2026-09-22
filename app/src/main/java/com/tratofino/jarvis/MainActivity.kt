package com.tratofino.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var statusText: TextView
    private lateinit var resultText: TextView
    private lateinit var talkButton: Button

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var textToSpeech: TextToSpeech

    private val microphoneRequestCode = 1001

    private val jarvisUrl =
        "https://jarvis-assistente.netlify.app/.netlify/functions/jarvis"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(createInterface())

        textToSpeech = TextToSpeech(this, this)

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                microphoneRequestCode
            )
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            statusText.text =
                "Reconhecimento de voz não disponível."
            talkButton.isEnabled = false
            return
        }

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

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
                    statusText.text =
                        "Não consegui entender. Tente novamente."
                    talkButton.isEnabled = true
                }

                override fun onResults(results: Bundle?) {

                    val matches =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val spokenText =
                        matches?.firstOrNull()

                    if (!spokenText.isNullOrBlank()) {
                        resultText.text =
                            "Você: $spokenText"

                        processCommand(spokenText)
                    }

                    talkButton.isEnabled = true
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {}

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {}
            }
        )

        talkButton.setOnClickListener {
            startListening()
        }
    }

    private fun startListening() {

        if (
            ContextCompat.checkSelfPermission(
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

        talkButton.isEnabled = false

        val intent =
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE,
            "pt-BR"
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
            "pt-BR"
        )

        intent.putExtra(
            RecognizerIntent.EXTRA_PROMPT,
            "Fale com o Jarvis"
        )

        speechRecognizer?.startListening(intent)
    }

    private fun processCommand(command: String) {

        val text =
            command.lowercase(Locale("pt", "BR"))

        when {

            text.contains("youtube") -> {
                speak("Abrindo o YouTube.")
                openUrl("https://www.youtube.com")
            }

            text.contains("google") -> {
                speak("Abrindo o Google.")
                openUrl("https://www.google.com")
            }

            text.contains("whatsapp") -> {
                speak("Abrindo o WhatsApp.")
                openApp("com.whatsapp")
            }

            text.contains("instagram") -> {
                speak("Abrindo o Instagram.")
                openApp("com.instagram.android")
            }

            text.contains("facebook") -> {
                speak("Abrindo o Facebook.")
                openApp("com.facebook.katana")
            }

            text.contains("configurações") ||
            text.contains("configuração") -> {

                speak("Abrindo as configurações.")

                startActivity(
                    Intent(Settings.ACTION_SETTINGS)
                )
            }

            text.contains("câmera") ||
            text.contains("camera") -> {

                speak("Abrindo a câmera.")

                startActivity(
                    Intent(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE
                    )
                )
            }

            text.contains("telefone") ||
            text.contains("ligar") -> {

                speak("Abrindo o telefone.")

                startActivity(
                    Intent(Intent.ACTION_DIAL)
                )
            }

            text.contains("mapa") ||
            text.contains("maps") -> {

                speak("Abrindo o mapa.")

                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q=Brasil")
                    )
                )
            }

            else -> {
                askJarvis(command)
            }
        }
    }

    private fun askJarvis(command: String) {

        runOnUiThread {
            statusText.text = "Jarvis: conectando..."
            talkButton.isEnabled = false
        }

        Thread {

            var connection: HttpURLConnection? = null

            try {

                val url = URL(jarvisUrl)

                connection =
                    url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.doOutput = true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val json = JSONObject()

                json.put(
                    "mensagem",
                    command
                )

                OutputStreamWriter(
                    connection.outputStream
                ).use { writer ->
                    writer.write(json.toString())
                    writer.flush()
                }

                val responseCode =
                    connection.responseCode

                val inputStream =
                    if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    }

                val response =
                    BufferedReader(
                        InputStreamReader(inputStream)
                    ).use { reader ->
                        reader.readText()
                    }

                val responseJson =
                    JSONObject(response)

                if (responseCode in 200..299) {

                    val resposta =
                        responseJson.optString(
                            "resposta",
                            "O servidor respondeu sem texto."
                        )

                    runOnUiThread {

                        resultText.text =
                            "Você: $command"

                        speak(resposta)

                        talkButton.isEnabled = true
                    }

                } else {

                    val erro =
                        responseJson.optString(
                            "error",
                            "Erro desconhecido no servidor."
                        )

                    runOnUiThread {

                        resultText.text =
                            "Erro do servidor: $erro"

                        speak(
                            "O servidor respondeu com um erro."
                        )

                        talkButton.isEnabled = true
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    resultText.text =
                        "Erro de conexão: ${e.message}"

                    speak(
                        "Ocorreu um erro de conexão."
                    )

                    talkButton.isEnabled = true
                }

            } finally {

                connection?.disconnect()
            }

        }.start()
    }

    private fun openApp(packageName: String) {

        try {

            val intent =
                packageManager.getLaunchIntentForPackage(
                    packageName
                )

            if (intent != null) {
                startActivity(intent)
            } else {
                speak("Esse aplicativo não está instalado.")
            }

        } catch (e: Exception) {
            speak("Não consegui abrir esse aplicativo.")
        }
    }

    private fun openUrl(url: String) {

        try {

            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
                )
            )

        } catch (e: Exception) {
            speak("Não consegui abrir esse endereço.")
        }
    }

    private fun speak(text: String) {

        runOnUiThread {

            statusText.text =
                "Jarvis: $text"

            textToSpeech.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "JARVIS_RESPONSE"
            )
        }
    }

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language =
                Locale("pt", "BR")
        }
    }

    private fun createInterface(): LinearLayout {

        val layout = LinearLayout(this)

        layout.orientation =
            LinearLayout.VERTICAL

        layout.setPadding(
            40,
            80,
            40,
            40
        )

        statusText = TextView(this)

        statusText.text =
            "Jarvis: pronto para ouvir."

        statusText.textSize = 22f

        resultText = TextView(this)

        resultText.text =
            "Fale comigo."

        resultText.textSize = 18f

        talkButton = Button(this)

        talkButton.text =
            "🎤 FALAR COM JARVIS"

        talkButton.textSize = 18f

        layout.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        layout.addView(
            resultText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        layout.addView(
            talkButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        return layout
    }

    override fun onDestroy() {

        speechRecognizer?.destroy()

        textToSpeech.stop()
        textToSpeech.shutdown()

        super.onDestroy()
    }
}
