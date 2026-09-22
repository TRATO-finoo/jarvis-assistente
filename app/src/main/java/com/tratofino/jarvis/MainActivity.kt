package com.tratofino.jarvis

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
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

    private lateinit var orb: TextView
    private lateinit var statusText: TextView
    private lateinit var conversation: LinearLayout
    private lateinit var input: EditText
    private lateinit var micButton: TextView
    private lateinit var continuousButton: TextView

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var textToSpeech: TextToSpeech

    private var continuousMode = false
    private var isSpeaking = false
    private var orbAnimator: ObjectAnimator? = null

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

        setupSpeechRecognizer()

        micButton.setOnClickListener {
            startListening()
        }

        continuousButton.setOnClickListener {
            continuousMode = !continuousMode
            updateContinuousButton()

            if (continuousMode) {
                statusText.text = "Modo contínuo ativado"
                startListening()
            } else {
                speechRecognizer?.stopListening()
                stopOrbAnimation()
                statusText.text = "Pronto para conversar"
            }
        }
    }

    private fun createInterface(): View {

        val root = LinearLayout(this)

        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 28, 20, 18)
        root.setBackgroundColor(Color.rgb(7, 9, 14))

        val title = TextView(this)

        title.text = "JARVIS"
        title.textSize = 25f
        title.setTextColor(Color.WHITE)
        title.typeface = Typeface.DEFAULT_BOLD
        title.gravity = Gravity.CENTER

        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                45
            )
        )

        val subtitle = TextView(this)

        subtitle.text = "Seu assistente pessoal"
        subtitle.textSize = 14f
        subtitle.setTextColor(Color.rgb(145, 153, 170))
        subtitle.gravity = Gravity.CENTER

        root.addView(
            subtitle,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                30
            )
        )

        val orbContainer = FrameLayout(this)

        orb = TextView(this)

        orb.text = "J"
        orb.textSize = 40f
        orb.gravity = Gravity.CENTER
        orb.setTextColor(Color.WHITE)
        orb.typeface = Typeface.DEFAULT_BOLD

        orb.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(
                Color.rgb(65, 170, 255),
                Color.rgb(30, 105, 235),
                Color.rgb(12, 42, 125)
            )
        ).apply {
            shape = GradientDrawable.OVAL
        }

        orbContainer.addView(
            orb,
            FrameLayout.LayoutParams(
                175,
                175,
                Gravity.CENTER
            )
        )

        root.addView(
            orbContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                210
            )
        )

        statusText = TextView(this)

        statusText.text = "Pronto para conversar"
        statusText.textSize = 15f
        statusText.setTextColor(Color.rgb(180, 188, 205))
        statusText.gravity = Gravity.CENTER

        root.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                40
            )
        )

        val scrollView = ScrollView(this)

        conversation = LinearLayout(this)

        conversation.orientation =
            LinearLayout.VERTICAL

        conversation.setPadding(
            4,
            10,
            4,
            10
        )

        scrollView.addView(conversation)

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        continuousButton = TextView(this)

        continuousButton.text =
            "◉  Modo contínuo: DESLIGADO"

        continuousButton.textSize = 14f
        continuousButton.gravity = Gravity.CENTER
        continuousButton.setTextColor(Color.WHITE)

        continuousButton.background =
            roundedBackground(
                Color.rgb(24, 29, 40),
                30f
            )

        root.addView(
            continuousButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                52
            )
        )

        val composer = LinearLayout(this)

        composer.gravity =
            Gravity.CENTER_VERTICAL

        composer.setPadding(
            0,
            10,
            0,
            0
        )

        input = EditText(this)

        input.hint =
            "Mensagem para o Jarvis..."

        input.setHintTextColor(
            Color.rgb(120, 128, 145)
        )

        input.setTextColor(Color.WHITE)

        input.textSize = 16f

        input.maxLines = 3

        input.background =
            roundedBackground(
                Color.rgb(24, 29, 40),
                28f
            )

        input.setPadding(
            20,
            8,
            15,
            8
        )

        composer.addView(
            input,
            LinearLayout.LayoutParams(
                0,
                58,
                1f
            )
        )

        micButton = TextView(this)

        micButton.text = "🎙"

        micButton.textSize = 24f
        micButton.gravity = Gravity.CENTER
        micButton.setTextColor(Color.WHITE)

        micButton.background =
            roundedBackground(
                Color.rgb(30, 110, 235),
                60f
            )

        composer.addView(
            micButton,
            LinearLayout.LayoutParams(
                58,
                58
            ).apply {
                setMargins(8, 0, 0, 0)
            }
        )

        val sendButton = TextView(this)

        sendButton.text = "➤"

        sendButton.textSize = 23f
        sendButton.gravity = Gravity.CENTER
        sendButton.setTextColor(Color.WHITE)

        sendButton.background =
            roundedBackground(
                Color.rgb(18, 70, 165),
                60f
            )

        sendButton.setOnClickListener {

            val message =
                input.text.toString().trim()

            if (message.isNotEmpty()) {

                addMessage(
                    "Você",
                    message
                )

                input.text.clear()

                processCommand(message)
            }
        }

        composer.addView(
            sendButton,
            LinearLayout.LayoutParams(
                58,
                58
            ).apply {
                setMargins(8, 0, 0, 0)
            }
        )

        root.addView(composer)

        return root
    }

    private fun setupSpeechRecognizer() {

        if (
            !SpeechRecognizer.isRecognitionAvailable(
                this
            )
        ) {

            statusText.text =
                "Reconhecimento de voz indisponível."

            return
        }

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(
                this
            )

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {

                    statusText.text =
                        "Ouvindo..."

                    startOrbAnimation()
                }

                override fun onBeginningOfSpeech() {

                    statusText.text =
                        "Pode falar..."
                }

                override fun onRmsChanged(
                    rmsdB: Float
                ) {}

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {}

                override fun onEndOfSpeech() {

                    statusText.text =
                        "Processando..."
                }

                override fun onError(
                    error: Int
                ) {

                    stopOrbAnimation()

                    statusText.text =
                        "Não consegui entender."

                    if (
                        continuousMode &&
                        !isSpeaking
                    ) {

                        orb.postDelayed(
                            {
                                startListening()
                            },
                            700
                        )
                    }
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    stopOrbAnimation()

                    val spokenText =
                        results
                            ?.getStringArrayList(
                                SpeechRecognizer.RESULTS_RECOGNITION
                            )
                            ?.firstOrNull()

                    if (
                        !spokenText.isNullOrBlank()
                    ) {

                        addMessage(
                            "Você",
                            spokenText
                        )

                        processCommand(
                            spokenText
                        )

                    } else if (
                        continuousMode &&
                        !isSpeaking
                    ) {

                        startListening()
                    }
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
                arrayOf(
                    Manifest.permission.RECORD_AUDIO
                ),
                microphoneRequestCode
            )

            return
        }

        val intent =
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            )

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

        speechRecognizer?.startListening(
            intent
        )
    }

    private fun processCommand(
        command: String
    ) {

        val text =
            command.lowercase(
                Locale("pt", "BR")
            )

        when {

            text.contains("youtube") -> {

                speak(
                    "Abrindo o YouTube."
                )

                openUrl(
                    "https://www.youtube.com"
                )
            }

            text.contains("google") -> {

                speak(
                    "Abrindo o Google."
                )

                openUrl(
                    "https://www.google.com"
                )
            }

            text.contains("whatsapp") -> {

                speak(
                    "Abrindo o WhatsApp."
                )

                openApp(
                    "com.whatsapp"
                )
            }

            text.contains("instagram") -> {

                speak(
                    "Abrindo o Instagram."
                )

                openApp(
                    "com.instagram.android"
                )
            }

            text.contains("facebook") -> {

                speak(
                    "Abrindo o Facebook."
                )

                openApp(
                    "com.facebook.katana"
                )
            }

            text.contains("configurações") ||
            text.contains("configuração") -> {

                speak(
                    "Abrindo as configurações."
                )

                startActivity(
                    Intent(
                        Settings.ACTION_SETTINGS
                    )
                )
            }

            text.contains("câmera") ||
            text.contains("camera") -> {

                speak(
                    "Abrindo a câmera."
                )

                startActivity(
                    Intent(
                        android.provider.MediaStore
                            .ACTION_IMAGE_CAPTURE
                    )
                )
            }

            text.contains("telefone") ||
            text.contains("ligar") -> {

                speak(
                    "Abrindo o telefone."
                )

                startActivity(
                    Intent(
                        Intent.ACTION_DIAL
                    )
                )
            }

            text.contains("mapa") ||
            text.contains("maps") -> {

                speak(
                    "Abrindo o mapa."
                )

                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "geo:0,0?q=Brasil"
                        )
                    )
                )
            }

            else -> {

                askJarvis(command)
            }
        }
    }

    private fun askJarvis(
        command: String
    ) {

        runOnUiThread {

            statusText.text =
                "Jarvis está pensando..."

            startOrbAnimation()
        }

        Thread {

            var connection:
                HttpURLConnection? = null

            try {

                connection =
                    URL(jarvisUrl)
                        .openConnection()
                            as HttpURLConnection

                connection.requestMethod =
                    "POST"

                connection.connectTimeout =
                    15000

                connection.readTimeout =
                    30000

                connection.doOutput =
                    true

                connection.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                connection.setRequestProperty(
                    "Accept",
                    "application/json"
                )

                val json =
                    JSONObject()

                json.put(
                    "mensagem",
                    command
                )

                OutputStreamWriter(
                    connection.outputStream
                ).use { writer ->

                    writer.write(
                        json.toString()
                    )

                    writer.flush()
                }

                val responseCode =
                    connection.responseCode

                val stream =
                    if (
                        responseCode in 200..299
                    ) {

                        connection.inputStream

                    } else {

                        connection.errorStream
                    }

                val response =
                    BufferedReader(
                        InputStreamReader(
                            stream
                        )
                    ).use {

                        it.readText()
                    }

                val responseJson =
                    JSONObject(response)

                runOnUiThread {

                    stopOrbAnimation()

                    if (
                        responseCode in 200..299
                    ) {

                        val answer =
                            responseJson.optString(
                                "resposta",
                                "Não consegui gerar uma resposta."
                            )

                        speak(answer)

                    } else {

                        val error =
                            responseJson.optString(
                                "error",
                                "Erro desconhecido no servidor."
                            )

                        addMessage(
                            "Sistema",
                            "Erro do servidor: $error"
                        )

                        speak(
                            "O servidor respondeu com um erro."
                        )
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    stopOrbAnimation()

                    addMessage(
                        "Sistema",
                        "Erro de conexão: " +
                            (e.message
                                ?: "erro desconhecido")
                    )

                    speak(
                        "Ocorreu um erro de conexão."
                    )
                }

            } finally {

                connection?.disconnect()
            }

        }.start()
    }

    private fun speak(
        text: String
    ) {

        runOnUiThread {

            statusText.text =
                "Jarvis está falando..."

            isSpeaking = true

            startOrbAnimation()

            addMessage(
 
