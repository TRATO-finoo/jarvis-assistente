package com.tratofino.jarvis

import android.Manifest
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var conversation: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var orb: TextView
    private lateinit var continuousSwitch: Switch
    private lateinit var tts: TextToSpeech

    private var speechRecognizer: SpeechRecognizer? = null
    private var continuousMode = false
    private var orbAnimator: ValueAnimator? = null

    private val serverUrl =
        "https://jarvis-assistente.netlify.app/.netlify/functions/jarvis"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)

        criarInterface()
        configurarReconhecimento()
    }

    private fun criarInterface() {

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Color.rgb(8, 10, 18))

        val title = TextView(this)
        title.text = "JARVIS"
        title.textSize = 28f
        title.setTextColor(Color.WHITE)
        title.gravity = Gravity.CENTER
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setPadding(0, 28, 0, 8)

        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        status = TextView(this)
        status.text = "Pronto para ajudar"
        status.textSize = 14f
        status.setTextColor(Color.LTGRAY)
        status.gravity = Gravity.CENTER
        status.setPadding(0, 0, 0, 10)

        root.addView(
            status,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        orb = TextView(this)
        orb.text = "J"
        orb.textSize = 42f
        orb.setTextColor(Color.WHITE)
        orb.gravity = Gravity.CENTER
        orb.background = criarFundoRedondo(Color.rgb(20, 110, 255))

        val orbParams = LinearLayout.LayoutParams(150, 150)
        orbParams.gravity = Gravity.CENTER
        orbParams.setMargins(0, 8, 0, 15)

        root.addView(orb, orbParams)

        scroll = ScrollView(this)

        conversation = LinearLayout(this)
        conversation.orientation = LinearLayout.VERTICAL
        conversation.setPadding(18, 10, 18, 10)

        scroll.addView(conversation)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val inputLayout = LinearLayout(this)
        inputLayout.orientation = LinearLayout.HORIZONTAL
        inputLayout.setPadding(10, 8, 10, 5)

        input = EditText(this)
        input.hint = "Digite para o Jarvis..."
        input.setHintTextColor(Color.GRAY)
        input.setTextColor(Color.WHITE)
        input.setSingleLine(true)
        input.background =
            criarFundoArredondado(Color.rgb(28, 31, 42), 30)

        inputLayout.addView(
            input,
            LinearLayout.LayoutParams(
                0,
                55,
                1f
            )
        )

        val sendButton = Button(this)
        sendButton.text = "➤"
        sendButton.setTextColor(Color.WHITE)
        sendButton.background =
            criarFundoRedondo(Color.rgb(25, 105, 230))

        val sendParams = LinearLayout.LayoutParams(60, 55)
        sendParams.setMargins(8, 0, 0, 0)

        inputLayout.addView(sendButton, sendParams)

        root.addView(
            inputLayout,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val buttonsLayout = LinearLayout(this)
        buttonsLayout.orientation = LinearLayout.HORIZONTAL
        buttonsLayout.gravity = Gravity.CENTER
        buttonsLayout.setPadding(10, 4, 10, 12)

        val micButton = Button(this)
        micButton.text = "🎤 FALAR"
        micButton.setTextColor(Color.WHITE)

        val micParams = LinearLayout.LayoutParams(
            0,
            55,
            1f
        )
        micParams.setMargins(0, 0, 5, 0)

        buttonsLayout.addView(micButton, micParams)

        continuousSwitch = Switch(this)
        continuousSwitch.text = "Contínuo"
        continuousSwitch.setTextColor(Color.WHITE)

        buttonsLayout.addView(
            continuousSwitch,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                55
            )
        )

        root.addView(
            buttonsLayout,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)

        sendButton.setOnClickListener {
            enviarTexto()
        }

        input.setOnEditorActionListener { _, _, _ ->
            enviarTexto()
            true
        }

        micButton.setOnClickListener {
            iniciarMicrofone()
        }

        continuousSwitch.setOnCheckedChangeListener { _, checked ->
            continuousMode = checked

            if (checked) {
                status.text = "Modo contínuo ativado"
                iniciarMicrofone()
            } else {
                status.text = "Modo contínuo desativado"
                pararMicrofone()
            }
        }

        addMessage(
            "Jarvis",
            "Olá! Estou pronto. Você pode falar comigo ou digitar uma mensagem."
        )
    }

    private fun criarFundoRedondo(cor: Int): GradientDrawable {
        val fundo = GradientDrawable()
        fundo.shape = GradientDrawable.OVAL
        fundo.setColor(cor)
        return fundo
    }

    private fun criarFundoArredondado(
        cor: Int,
        raio: Int
    ): GradientDrawable {
        val fundo = GradientDrawable()
        fundo.shape = GradientDrawable.RECTANGLE
        fundo.cornerRadius = raio.toFloat()
        fundo.setColor(cor)
        return fundo
    }

    private fun addMessage(
        remetente: String,
        mensagem: String
    ) {
        val texto = TextView(this)

        texto.text = "$remetente:\n$mensagem"
        texto.textSize = 16f
        texto.setTextColor(Color.WHITE)
        texto.setPadding(18, 14, 18, 14)
        texto.background =
            criarFundoArredondado(Color.rgb(24, 27, 38), 22)

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 5, 0, 5)

        conversation.addView(texto, params)

        scroll.post {
            scroll.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun enviarTexto() {

        val mensagem = input.text.toString().trim()

        if (mensagem.isEmpty()) {
            return
        }

        input.setText("")

        addMessage("Você", mensagem)

        executarComandoOuIA(mensagem)
    }

    private fun executarComandoOuIA(mensagem: String) {

        val texto = mensagem.lowercase(Locale.getDefault())

        when {
            texto.contains("youtube") -> {
                abrirUrl("https://www.youtube.com")
            }

            texto.contains("google") -> {
                abrirUrl("https://www.google.com")
            }

            texto.contains("instagram") -> {
                abrirApp(
                    "com.instagram.android",
                    "https://www.instagram.com"
                )
            }

            texto.contains("whatsapp") -> {
                abrirApp(
                    "com.whatsapp",
                    "https://www.whatsapp.com"
                )
            }

            texto.contains("facebook") -> {
                abrirApp(
                    "com.facebook.katana",
                    "https://www.facebook.com"
                )
            }

            texto.contains("câmera") ||
            texto.contains("camera") -> {

                try {
                    val intent =
                        Intent("android.media.action.IMAGE_CAPTURE")

                    startActivity(intent)
                    falar("Abrindo a câmera.")
                } catch (e: Exception) {
                    falar("Não consegui abrir a câmera.")
                }
            }

            texto.contains("configurações") ||
            texto.contains("configuracoes") -> {

                try {
                    startActivity(
                        Intent(
                            android.provider.Settings.ACTION_SETTINGS
                        )
                    )

                    falar("Abrindo as configurações.")
                } catch (e: Exception) {
                    falar("Não consegui abrir as configurações.")
                }
            }

            texto.contains("mapa") ||
            texto.contains("maps") -> {
                abrirUrl("https://maps.google.com")
            }

            else -> {
                perguntarParaServidor(mensagem)
            }
        }
    }

    private fun abrirUrl(url: String) {

        try {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse(url)
            )

            startActivity(intent)
            falar("Abrindo agora.")

        } catch (e: Exception) {
            falar("Não consegui abrir.")
        }
    }

    private fun abrirApp(
        pacote: String,
        fallback: String
    ) {

        try {

            val intent =
                packageManager.getLaunchIntentForPackage(pacote)

            if (intent != null) {
                startActivity(intent)
                falar("Abrindo.")
            } else {
                abrirUrl(fallback)
            }

        } catch (e: Exception) {
            abrirUrl(fallback)
        }
    }

    private fun perguntarParaServidor(mensagem: String) {

        status.text = "Jarvis está pensando..."
        startOrbAnimation()

        thread {

            var conexao: HttpURLConnection? = null

            try {

                val url = URL(serverUrl)

                conexao =
                    url.openConnection() as HttpURLConnection

                conexao.requestMethod = "POST"
                conexao.connectTimeout = 15000
                conexao.readTimeout = 30000
                conexao.doOutput = true

                conexao.setRequestProperty(
                    "Content-Type",
                    "application/json"
                )

                val json = JSONObject()
                json.put("mensagem", mensagem)

                conexao.outputStream.use { output ->
                    output.write(
                        json.toString()
                            .toByteArray(Charsets.UTF_8)
                    )
                }

                val codigo = conexao.responseCode

                val respostaTexto =
                    if (codigo in 200..299) {
                        conexao.inputStream
                            .bufferedReader()
                            .use { it.readText() }
                    } else {
                        conexao.errorStream
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            ?: "Erro HTTP $codigo"
                    }

                runOnUiThread {

                    stopOrbAnimation()

                    try {

                        val respostaJson =
                            JSONObject(respostaTexto)

                        if (codigo in 200..299) {

                            val resposta =
                                respostaJson.optString(
                                    "resposta",
                                    "Não recebi uma resposta."
                                )

                            addMessage("Jarvis", resposta)
                            falar(resposta)

                            status.text = "Pronto"

                        } else {

                            val erro =
                                respostaJson.optString(
                                    "error",
                                    "Erro no servidor."
                                )

                            addMessage(
                                "Jarvis",
                                "Erro do servidor: $erro"
                            )

                            falar(
                                "Encontrei um problema no servidor."
                            )

                            status.text = "Erro no servidor"
                        }

                    } catch (e: Exception) {

                        addMessage(
                            "Jarvis",
                            "Resposta recebida:\n$respostaTexto"
                        )

                        status.text = "Resposta recebida"
                    }
                }

            } catch (e: Exception) {

                runOnUiThread {

                    stopOrbAnimation()

                    val erro =
                        e.message ?: "Erro desconhecido"

                    addMessage(
                        "Jarvis",
                        "Não consegui conectar ao meu servidor.\n\n$erro"
                    )

                    status.text = "Falha na conexão"

                    falar(
                        "Não consegui conectar ao meu servidor."
                    )
                }

            } finally {
                conexao?.disconnect()
            }
        }
    }

    private fun configurarReconhecimento() {

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "Reconhecimento de voz indisponível"
            return
        }

        speechRecognizer =
            SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer?.setRecognitionListener(
            object : RecognitionListener {

                override fun onReadyForSpeech(
                    params: Bundle?
                ) {
                    status.text = "Pode falar..."
                    startOrbAnimation()
                }

                override fun onBeginningOfSpeech() {
                    status.text = "Estou ouvindo..."
                }

                override fun onRmsChanged(
                    rmsdB: Float
                ) {
                }

                override fun onBufferReceived(
                    buffer: ByteArray?
                ) {
                }

                override fun onEndOfSpeech() {
                    status.text = "Processando..."
                }

                override fun onError(error: Int) {

                    stopOrbAnimation()

                    if (continuousMode) {

                        status.text =
                            "Tentando ouvir novamente..."

                        window.decorView.postDelayed(
                            {
                                iniciarMicrofone()
                            },
                            700
                        )

                    } else {
                        status.text = "Pronto"
                    }
                }

                override fun onResults(
                    results: Bundle?
                ) {

                    stopOrbAnimation()

                    val resultados =
                        results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                    val texto =
                        resultados?.firstOrNull()

                    if (!texto.isNullOrBlank()) {

                        input.setText(texto)

                        addMessage("Você", texto)

                        executarComandoOuIA(texto)

                    } else {
                        status.text = "Não entendi."
                    }

                    if (continuousMode) {

                        window.decorView.postDelayed(
                            {
                                iniciarMicrofone()
                            },
                            700
                        )
                    }
                }

                override fun onPartialResults(
                    partialResults: Bundle?
                ) {
                }

                override fun onEvent(
                    eventType: Int,
                    params: Bundle?
                ) {
                }
            }
        )
    }

    private fun iniciarMicrofone() {

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                100
            )

            return
        }

        try {

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
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                false
            )

            speechRecognizer?.startListening(intent)

        } catch (e: Exception) {

            status.text = "Erro ao iniciar microfone"
        }
    }

    private fun pararMicrofone() {

        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }

        stopOrbAnimation()
    }

    override fun onInit(statusCode: Int) {

        if (statusCode == TextToSpeech.SUCCESS) {

            tts.language = Locale("pt", "BR")
            tts.setSpeechRate(1.0f)
        }
    }

    private fun falar(texto: String) {

        if (!::tts.isInitialized) {
            return
        }

        tts.speak(
            texto,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "jarvis"
        )
    }

    private fun startOrbAnimation() {

        if (orbAnimator != null) {
            return
        }

        orbAnimator =
            ValueAnimator.ofFloat(1.0f, 1.10f)

        orbAnimator?.duration = 700

        orbAnimator?.repeatCount =
            ValueAnimator.INFINITE

        orbAnimator?.repeatMode =
            ValueAnimator.REVERSE

        orbAnimator?.addUpdateListener { animation ->

            val valor =
                animation.animatedValue as Float

            orb.scaleX = valor
            orb.scaleY = valor
        }

        orbAnimator?.start()
    }

    private fun stopOrbAnimation() {

        orbAnimator?.cancel()
        orbAnimator = null

        orb.scaleX = 1f
        orb.scaleY = 1f
    }

    override fun onDestroy() {

        stopOrbAnimation()

        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }

        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Exception) {
        }

        super.onDestroy()
    }
}
