package edu.umg.lumifinanzas

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Xml
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class CambioActivity : AppCompatActivity() {

    private lateinit var txtTasa: TextView
    private lateinit var txtFecha: TextView
    private lateinit var txtResultado: TextView
    private lateinit var txtEstado: TextView
    private lateinit var edtCantidad: EditText
    private lateinit var spinner: Spinner

    // Preferencias exclusivas de Banguat
    private val preferencias by lazy {
        getSharedPreferences(
            "LUMI_CAMBIO_BANGUAT",
            MODE_PRIVATE
        )
    }

    private var tasa: Double = 0.0
    private var fechaTasa: String = ""
    private var actualizando = false

    private val urlApi =
        "https://www.banguat.gob.gt/variables/ws/TipoCambio.asmx"

    private val accionSoap =
        "http://www.banguat.gob.gt/variables/ws/TipoCambioDia"

    private val urlFuente =
        "https://www.banguat.gob.gt/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cambio)

        // ==========================================
        // AJUSTE DE BARRAS DEL SISTEMA
        // Evita que la pantalla se superponga
        // con la hora y los iconos de Android.
        // ==========================================

        val contenido = findViewById<View>(
            android.R.id.content
        )

        ViewCompat.setOnApplyWindowInsetsListener(
            contenido
        ) { vista, insets ->

            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            vista.setPadding(
                barras.left,
                barras.top,
                barras.right,
                barras.bottom
            )

            insets
        }

        ViewCompat.requestApplyInsets(contenido)

        // ==========================================
        // VINCULAR COMPONENTES DE LA INTERFAZ
        // ==========================================

        txtTasa = findViewById(R.id.txtTasaCambio)
        txtFecha = findViewById(R.id.txtFechaCambio)
        txtResultado = findViewById(R.id.txtResultadoCambio)
        txtEstado = findViewById(R.id.txtEstadoCambio)
        edtCantidad = findViewById(R.id.edtCantidadCambio)
        spinner = findViewById(R.id.spinnerConversion)

        // ==========================================
        // OPCIONES DE CONVERSIÓN
        // ==========================================

        val opciones = arrayOf(
            "Dólares (USD) → Quetzales (GTQ)",
            "Quetzales (GTQ) → Dólares (USD)"
        )

        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opciones
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter = adaptador

        // ==========================================
        // BOTONES
        // ==========================================

        findViewById<Button>(R.id.btnConvertir)
            .setOnClickListener {
                convertir()
            }

        findViewById<Button>(R.id.btnActualizarCambio)
            .setOnClickListener {
                consultarTipoCambio()
            }

        findViewById<Button>(R.id.btnVolverCambio)
            .setOnClickListener {
                finish()
            }

        // ==========================================
        // FUENTE OFICIAL DE BANGUAT
        // ==========================================

        findViewById<TextView>(
            R.id.txtFuenteCambio
        ).apply {

            text = "Fuente: Banco de Guatemala (Banguat)"

            setOnClickListener {

                val enlace = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(urlFuente)
                )

                startActivity(enlace)
            }
        }

        // Cargar la última tasa guardada
        cargarUltimaTasa()

        // Consultar tasa actual
        consultarTipoCambio()
    }

    // ==========================================
    // CARGAR ÚLTIMA TASA GUARDADA
    // ==========================================

    private fun cargarUltimaTasa() {

        val tasaGuardada = preferencias.getString(
            "tasa_gtq",
            null
        )?.toDoubleOrNull()

        if (
            tasaGuardada != null &&
            tasaGuardada.isFinite() &&
            tasaGuardada > 0.0
        ) {

            tasa = tasaGuardada

            fechaTasa = preferencias.getString(
                "fecha_tasa",
                "Fecha no disponible"
            ) ?: "Fecha no disponible"

            mostrarTasa()

            txtEstado.text =
                "Mostrando la última tasa guardada de Banguat"

        } else {

            txtTasa.text =
                "Esperando tipo de cambio..."

            txtFecha.text =
                "Conéctate para consultar al Banco de Guatemala"

            txtEstado.text =
                "Aún no hay una tasa de Banguat guardada"
        }
    }

    // ==========================================
    // CONSULTAR TIPO DE CAMBIO EN BANGUAT
    // ==========================================

    private fun consultarTipoCambio() {

        if (actualizando) return

        actualizando = true

        txtEstado.text =
            "Consultando al Banco de Guatemala..."

        Thread {

            var conexion: HttpURLConnection? = null

            try {

                val solicitudSoap = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <soap:Envelope
                        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                        xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
                        <soap:Body>
                            <TipoCambioDia
                                xmlns="http://www.banguat.gob.gt/variables/ws/" />
                        </soap:Body>
                    </soap:Envelope>
                """.trimIndent()

                conexion = URL(urlApi).openConnection()
                        as HttpURLConnection

                conexion.requestMethod = "POST"
                conexion.connectTimeout = 15000
                conexion.readTimeout = 15000
                conexion.doOutput = true

                conexion.setRequestProperty(
                    "Content-Type",
                    "text/xml; charset=utf-8"
                )

                conexion.setRequestProperty(
                    "SOAPAction",
                    "\"$accionSoap\""
                )

                conexion.setRequestProperty(
                    "Accept",
                    "text/xml"
                )

                val datos = solicitudSoap.toByteArray(
                    Charsets.UTF_8
                )

                conexion.setFixedLengthStreamingMode(
                    datos.size
                )

                conexion.outputStream.use { salida ->
                    salida.write(datos)
                }

                val codigo = conexion.responseCode

                if (
                    codigo != HttpURLConnection.HTTP_OK
                ) {
                    throw Exception(
                        "Respuesta HTTP: $codigo"
                    )
                }

                val respuesta = conexion.inputStream
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }

                val (nuevaTasa, nuevaFecha) =
                    interpretarRespuestaBanguat(respuesta)

                if (
                    !nuevaTasa.isFinite() ||
                    nuevaTasa <= 0.0
                ) {
                    throw Exception(
                        "El Banco de Guatemala devolvió una tasa inválida"
                    )
                }

                // Guardar la tasa oficial
                preferencias.edit()
                    .putString(
                        "tasa_gtq",
                        nuevaTasa.toString()
                    )
                    .putString(
                        "fecha_tasa",
                        nuevaFecha
                    )
                    .apply()

                runOnUiThread {

                    tasa = nuevaTasa
                    fechaTasa = nuevaFecha

                    mostrarTasa()

                    txtEstado.text =
                        "Tipo de cambio de Banguat consultado correctamente"

                    actualizando = false
                }

            } catch (e: Exception) {

                Log.e(
                    "LUMI_BANGUAT",
                    "Error consultando tipo de cambio",
                    e
                )

                runOnUiThread {

                    txtEstado.text = if (tasa > 0.0) {
                        "No se pudo actualizar. Usando la última tasa guardada de Banguat."
                    } else {
                        "No se pudo consultar Banguat. Verifica internet e inténtalo de nuevo."
                    }

                    Toast.makeText(
                        this,
                        "Error de consulta: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()

                    actualizando = false
                }

            } finally {

                conexion?.disconnect()
            }

        }.start()
    }

    // ==========================================
    // INTERPRETAR RESPUESTA XML DE BANGUAT
    // ==========================================

    private fun interpretarRespuestaBanguat(
        xml: String
    ): Pair<Double, String> {

        val parser = Xml.newPullParser()

        parser.setFeature(
            XmlPullParser.FEATURE_PROCESS_NAMESPACES,
            true
        )

        parser.setInput(StringReader(xml))

        var dentroCambioDolar = false
        var dentroVarDolar = false

        var referencia: Double? = null
        var fecha = ""

        var evento = parser.eventType

        while (
            evento != XmlPullParser.END_DOCUMENT
        ) {

            if (
                evento == XmlPullParser.START_TAG
            ) {

                when (parser.name) {

                    "Fault" -> {
                        throw Exception(
                            "El servicio Banguat devolvió un error SOAP"
                        )
                    }

                    "CambioDolar" -> {
                        dentroCambioDolar = true
                    }

                    "VarDolar" -> {

                        if (dentroCambioDolar) {

                            dentroVarDolar = true
                            referencia = null
                            fecha = ""
                        }
                    }

                    "referencia" -> {

                        if (dentroVarDolar) {

                            referencia = parser.nextText()
                                .trim()
                                .toDoubleOrNull()
                        }
                    }

                    "fecha" -> {

                        if (dentroVarDolar) {

                            fecha = parser.nextText()
                                .trim()
                        }
                    }
                }

            } else if (
                evento == XmlPullParser.END_TAG
            ) {

                when (parser.name) {

                    "VarDolar" -> {

                        if (dentroVarDolar) {

                            val valor = referencia

                            if (
                                valor != null &&
                                valor.isFinite() &&
                                valor > 0.0 &&
                                fecha.isNotBlank()
                            ) {

                                return Pair(
                                    valor,
                                    fecha
                                )
                            }

                            dentroVarDolar = false
                        }
                    }

                    "CambioDolar" -> {
                        dentroCambioDolar = false
                    }
                }
            }

            evento = parser.next()
        }

        throw Exception(
            "No se encontró una tasa válida en la respuesta de Banguat"
        )
    }

    // ==========================================
    // MOSTRAR TASA OFICIAL
    // ==========================================

    private fun mostrarTasa() {

        txtTasa.text = String.format(
            Locale.US,
            "1 USD = Q %.5f",
            tasa
        )

        txtFecha.text =
            "Fecha de referencia Banguat: $fechaTasa"
    }

    // ==========================================
    // CONVERTIR MONEDAS
    // ==========================================

    private fun convertir() {

        val cantidad = edtCantidad.text
            .toString()
            .trim()
            .toDoubleOrNull()

        if (
            cantidad == null ||
            !cantidad.isFinite() ||
            cantidad <= 0.0
        ) {

            edtCantidad.error =
                "Ingresa una cantidad válida mayor que cero"

            edtCantidad.requestFocus()

            return
        }

        if (tasa <= 0.0) {

            Toast.makeText(
                this,
                "Primero consulta el tipo de cambio de Banguat",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val resultado: Double
        val moneda: String

        if (
            spinner.selectedItemPosition == 0
        ) {

            resultado = cantidad * tasa
            moneda = "GTQ"

        } else {

            resultado = cantidad / tasa
            moneda = "USD"
        }

        if (!resultado.isFinite()) {

            txtResultado.text =
                "La cantidad es demasiado grande"

            return
        }

        txtResultado.text = String.format(
            Locale.US,
            "%,.2f %s",
            resultado,
            moneda
        )
    }
}
