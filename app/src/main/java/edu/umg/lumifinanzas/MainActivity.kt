package edu.umg.lumifinanzas

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import edu.umg.lumifinanzas.database.LumiDatabaseHelper
import edu.umg.lumifinanzas.database.MetasDatabaseHelper
import edu.umg.programacion2.gastos.modelo.Categoria
import edu.umg.programacion2.gastos.modelo.Gasto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var baseDatos: LumiDatabaseHelper

    private val preferencias by lazy {
        getSharedPreferences("LUMI_PRESUPUESTOS", MODE_PRIVATE)
    }

    private val preferenciasCambio by lazy {
        getSharedPreferences("LUMI_CAMBIO_BANGUAT", MODE_PRIVATE)
    }

    private val morado = Color.parseColor("#6741B8")
    private val verde = Color.parseColor("#176B45")
    private val rojo = Color.parseColor("#AE244C")
    private val oscuro = Color.parseColor("#30244D")
    private val gris = Color.parseColor("#777083")

    // =====================================================
    // INICIO
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
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

        baseDatos = LumiDatabaseHelper(this)

        val gastoPrueba = Gasto(
            descripcion = "Prueba de conexión Kotlin",
            monto = 25.0,
            categoria = Categoria.ALIMENTACION,
            fecha = "2026-10-28"
        )

        Log.d(
            "LUMI",
            "Librería Kotlin conectada: ${gastoPrueba.descripcion}"
        )

        configurarBotones()
        actualizarPantalla()
    }

    // =====================================================
    // BOTONES PRINCIPALES
    // =====================================================

    private fun configurarBotones() {

        findViewById<Button>(
            R.id.btnAgregarMovimiento
        ).setOnClickListener {
            mostrarFormularioMovimiento()
        }

        findViewById<Button>(
            R.id.btnConfigurarPresupuesto
        ).setOnClickListener {
            mostrarDialogoPresupuesto()
        }

        findViewById<Button>(
            R.id.btnVerHistorial
        ).setOnClickListener {
            abrirHistorial()
        }

        findViewById<Button>(
            R.id.btnVerEstadisticas
        ).setOnClickListener {
            abrirEstadisticas()
        }

        findViewById<Button>(
            R.id.btnAbrirCambio
        ).setOnClickListener {
            abrirCambio()
        }

        findViewById<Button>(
            R.id.navInicio
        ).setOnClickListener {
            findViewById<ScrollView>(
                R.id.mainScroll
            ).smoothScrollTo(0, 0)
        }

        findViewById<Button>(
            R.id.navHistorial
        ).setOnClickListener {
            abrirHistorial()
        }

        findViewById<Button>(
            R.id.navEstadisticas
        ).setOnClickListener {
            abrirEstadisticas()
        }

        findViewById<Button>(
            R.id.navMetas
        ).setOnClickListener {
            abrirMetas()
        }
    }

    // =====================================================
    // NAVEGACIÓN
    // =====================================================

    private fun abrirHistorial() {
        startActivity(
            Intent(this, HistorialActivity::class.java)
        )
    }

    private fun abrirEstadisticas() {
        startActivity(
            Intent(this, EstadisticasActivity::class.java)
        )
    }

    private fun abrirMetas() {
        startActivity(
            Intent(this, MetasActivity::class.java)
        )
    }

    private fun abrirCambio() {
        startActivity(
            Intent(this, CambioActivity::class.java)
        )
    }

    // =====================================================
    // AHORROS RESERVADOS
    // =====================================================

    private fun totalAhorrado(): Double {
        val db = MetasDatabaseHelper(this)

        return try {
            db.obtenerMetas().sumOf { it.ahorrado }
        } finally {
            db.close()
        }
    }

    // =====================================================
    // TIPO DE CAMBIO GUARDADO POR LUMI CAMBIO
    // =====================================================

    private fun obtenerTasaGuardada(): Double {
        val tasa = preferenciasCambio
            .getString("tasa_gtq", null)
            ?.toDoubleOrNull()

        return if (
            tasa != null &&
            tasa.isFinite() &&
            tasa > 0.0
        ) {
            tasa
        } else {
            0.0
        }
    }

    private fun obtenerFechaTasa(): String {
        return preferenciasCambio.getString(
            "fecha_tasa",
            "Fecha no disponible"
        ) ?: "Fecha no disponible"
    }

    // =====================================================
    // FORMULARIO NUEVO MOVIMIENTO
    // =====================================================

    private fun mostrarFormularioMovimiento() {

        val vista = layoutInflater.inflate(
            R.layout.dialog_registrar_movimiento,
            null
        )

        val dialogo = AlertDialog.Builder(this)
            .setView(vista)
            .create()

        val botonCerrar = vista.findViewById<TextView>(
            R.id.btnCerrarMovimiento
        )

        botonCerrar.setOnClickListener {
            dialogo.dismiss()
        }

        val descripcion = vista.findViewById<EditText>(
            R.id.edtDescripcion
        )

        val monto = vista.findViewById<EditText>(
            R.id.edtMonto
        )

        val tipoMovimiento = vista.findViewById<RadioGroup>(
            R.id.radioTipoMovimiento
        )

        val radioIngreso = vista.findViewById<RadioButton>(
            R.id.radioIngreso
        )

        val radioGasto = vista.findViewById<RadioButton>(
            R.id.radioGasto
        )

        val categorias = vista.findViewById<Spinner>(
            R.id.spinnerCategoria
        )

        val botonGuardar = vista.findViewById<Button>(
            R.id.btnGuardarMovimiento
        )

        val ayudaMovimiento = vista.findViewById<TextView>(
            R.id.txtAyudaMovimiento
        )

        val contenedorCategorias = vista.findViewById<GridLayout>(
            R.id.contenedorCategoriasMovimiento
        )

        // =================================================
        // SELECCIÓN DE MONEDA: GTQ / USD
        // Los controles ya existen en dialog_registrar_movimiento.xml
        // =================================================

        val selectorMoneda = vista.findViewById<Spinner>(
            R.id.spinnerMonedaMovimiento
        )
        val simboloMoneda = vista.findViewById<TextView>(
            R.id.txtSimboloMonedaMovimiento
        )
        val tarjetaConversion = vista.findViewById<LinearLayout>(
            R.id.tarjetaConversionMovimiento
        )
        val equivalente = vista.findViewById<TextView>(
            R.id.txtEquivalenteMovimiento
        )
        val detalleConversion = vista.findViewById<TextView>(
            R.id.txtDetalleConversionMovimiento
        )

        val opcionesMoneda = arrayOf("GTQ (Q)", "USD ($)")
        val adaptadorMoneda = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcionesMoneda
        )
        adaptadorMoneda.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        selectorMoneda.adapter = adaptadorMoneda

        fun actualizarConversion() {
            val esDolar = selectorMoneda.selectedItemPosition == 1
            simboloMoneda.text = if (esDolar) "$" else "Q"
            tarjetaConversion.visibility = if (esDolar) View.VISIBLE else View.GONE
            monto.hint = "0.00"
            if (!esDolar) return

            val tasa = obtenerTasaGuardada()
            val fecha = obtenerFechaTasa()
            val cantidad = monto.text.toString().trim().toDoubleOrNull()

            if (tasa <= 0.0) {
                equivalente.text = "Sin tasa disponible"
                equivalente.setTextColor(rojo)
                detalleConversion.text =
                    "Abre Lumi Cambio y actualiza la tasa del Banco de Guatemala."
                return
            }

            equivalente.setTextColor(morado)
            val convertido = if (cantidad != null && cantidad.isFinite() && cantidad > 0.0) {
                cantidad * tasa
            } else {
                0.0
            }
            equivalente.text = if (convertido.isFinite()) {
                formatoQuetzales(convertido)
            } else {
                "Cantidad demasiado grande"
            }
            detalleConversion.text =
                "1 USD = Q ${String.format(Locale.US, "%.5f", tasa)}\n" +
                        "Fecha de referencia: $fecha\nFuente: Banco de Guatemala"
        }

        selectorMoneda.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                actualizarConversion()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        monto.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                actualizarConversion()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        // =================================================
        // CAMPO PERSONALIZADO OTROS
        // =================================================

        val contenedorOtros = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(0, dp(12), 0, 0)
        }

        val tituloOtros = TextView(this).apply {
            text = "Especifica el tipo de ingreso o gasto"
            textSize = 14f
            setTextColor(oscuro)
            setTypeface(null, Typeface.BOLD)
        }

        val campoOtros = EditText(this).apply {
            hint = "Ej. Mascotas, reparación, regalo..."
            textSize = 14f
            setTextColor(oscuro)
            setHintTextColor(gris)

            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES

            isSingleLine = true

            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )

            background = GradientDrawable().apply {
                setColor(Color.parseColor("#F0EAFB"))
                cornerRadius = dp(14).toFloat()
            }
        }

        contenedorOtros.addView(tituloOtros)

        contenedorOtros.addView(
            campoOtros,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                topMargin = dp(8)
            }
        )

        val padreCategorias =
            contenedorCategorias.parent as LinearLayout

        val posicionCategorias =
            padreCategorias.indexOfChild(contenedorCategorias)

        padreCategorias.addView(
            contenedorOtros,
            posicionCategorias + 1
        )

        // =================================================
        // SELECCIÓN DE INGRESO O GASTO
        // =================================================

        fun actualizarEstiloMovimiento() {

            val esIngreso = radioIngreso.isChecked

            if (esIngreso) {

                ayudaMovimiento.text =
                    "Registra el dinero que recibes y " +
                            "aumenta tu saldo disponible."

                ayudaMovimiento.setTextColor(verde)

                botonGuardar.text = "Guardar ingreso"

                botonGuardar.backgroundTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#168F70")
                    )

                descripcion.hint =
                    "Ej. Salario, venta o regalo"

            } else {

                ayudaMovimiento.text =
                    "Tus gastos se descontarán del " +
                            "dinero disponible, sin utilizar " +
                            "tus ahorros reservados."

                ayudaMovimiento.setTextColor(rojo)

                botonGuardar.text = "Guardar gasto"

                botonGuardar.backgroundTintList =
                    ColorStateList.valueOf(morado)

                descripcion.hint =
                    "Ej. Compra de alimentos"
            }

            radioIngreso.background = fondoSeleccion(
                Color.parseColor("#E5F6ED"),
                Color.parseColor("#27A467"),
                esIngreso
            )

            radioGasto.background = fondoSeleccion(
                Color.parseColor("#FDECF1"),
                Color.parseColor("#D95D7B"),
                !esIngreso
            )
        }

        tipoMovimiento.setOnCheckedChangeListener { _, _ ->
            actualizarEstiloMovimiento()
        }

        if (tipoMovimiento.checkedRadioButtonId == -1) {
            radioGasto.isChecked = true
        }

        actualizarEstiloMovimiento()

        // =================================================
        // CATEGORÍAS
        // =================================================

        val listaCategorias = arrayOf(
            "Alimentación",
            "Transporte",
            "Estudios",
            "Entretenimiento",
            "Salud",
            "Hogar",
            "Compras",
            "Otros"
        )

        val adaptador = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            listaCategorias
        )

        adaptador.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        categorias.adapter = adaptador

        val etiquetas = arrayOf(
            "Comida",
            "Transporte",
            "Estudios",
            "Diversión",
            "Salud",
            "Hogar",
            "Compras",
            "Otros"
        )

        val colores = intArrayOf(
            Color.parseColor("#E8A04A"),
            Color.parseColor("#477BC8"),
            Color.parseColor("#6741B8"),
            Color.parseColor("#E08C50"),
            Color.parseColor("#D95D7B"),
            Color.parseColor("#168F87"),
            Color.parseColor("#AE70D0"),
            Color.parseColor("#777083")
        )

        val tarjetas = mutableListOf<LinearLayout>()

        fun seleccionarCategoria(indice: Int) {

            categorias.setSelection(indice)

            contenedorOtros.visibility =
                if (indice == 7) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            if (indice != 7) {
                campoOtros.error = null
            }

            tarjetas.forEachIndexed { posicion, tarjeta ->

                val seleccionada = posicion == indice

                tarjeta.background = fondoSeleccion(
                    if (seleccionada) {
                        Color.parseColor("#EDE5FB")
                    } else {
                        Color.parseColor("#F7F4FC")
                    },
                    morado,
                    seleccionada
                )
            }
        }

        contenedorCategorias.removeAllViews()

        listaCategorias.forEachIndexed { indice, _ ->

            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER

                setPadding(
                    dp(2),
                    dp(7),
                    dp(2),
                    dp(7)
                )

                isClickable = true
                isFocusable = true

                contentDescription =
                    "Categoría ${etiquetas[indice]}"
            }

            val icono = ImageView(this).apply {
                setImageDrawable(
                    crearIconoCategoria(
                        indice,
                        colores[indice]
                    )
                )

                scaleType =
                    ImageView.ScaleType.FIT_CENTER

                contentDescription = etiquetas[indice]
            }

            tarjeta.addView(
                icono,
                LinearLayout.LayoutParams(
                    dp(25),
                    dp(25)
                )
            )

            val nombre = TextView(this).apply {
                text = etiquetas[indice]
                textSize = 10f
                gravity = Gravity.CENTER

                setTextColor(
                    Color.parseColor("#625B73")
                )

                maxLines = 1
            }

            tarjeta.addView(
                nombre,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(5)
                }
            )

            val parametros =
                GridLayout.LayoutParams().apply {

                    width = 0
                    height = dp(76)

                    columnSpec = GridLayout.spec(
                        indice % 4,
                        1,
                        1f
                    )

                    rowSpec = GridLayout.spec(
                        indice / 4
                    )

                    setMargins(
                        dp(3),
                        dp(3),
                        dp(3),
                        dp(3)
                    )
                }

            contenedorCategorias.addView(
                tarjeta,
                parametros
            )

            tarjetas.add(tarjeta)

            tarjeta.setOnClickListener {
                seleccionarCategoria(indice)
            }
        }

        seleccionarCategoria(0)

        // =================================================
        // GUARDAR MOVIMIENTO
        // =================================================

        botonGuardar.setOnClickListener {

            val textoDescripcion =
                descripcion.text.toString().trim()

            val cantidadOriginal =
                monto.text.toString()
                    .trim()
                    .toDoubleOrNull()

            if (textoDescripcion.isEmpty()) {

                descripcion.error =
                    "Escribe una descripción"

                descripcion.requestFocus()
                return@setOnClickListener
            }

            if (
                cantidadOriginal == null ||
                !cantidadOriginal.isFinite() ||
                cantidadOriginal <= 0.0
            ) {

                monto.error =
                    "Ingresa un monto válido mayor que cero"

                monto.requestFocus()
                return@setOnClickListener
            }

            if (
                tipoMovimiento.checkedRadioButtonId == -1
            ) {

                Toast.makeText(
                    this,
                    "Selecciona ingreso o gasto",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            val tipo =
                if (radioIngreso.isChecked) {
                    "INGRESO"
                } else {
                    "GASTO"
                }

            // MONEDA ORIGINAL

            val monedaOriginal =
                if (selectorMoneda.selectedItemPosition == 1) {
                    "USD"
                } else {
                    "GTQ"
                }

            // TASA UTILIZADA

            val tasaCambio =
                if (monedaOriginal == "USD") {
                    obtenerTasaGuardada()
                } else {
                    1.0
                }

            if (
                monedaOriginal == "USD" &&
                tasaCambio <= 0.0
            ) {

                monto.error =
                    "Actualiza la tasa en Lumi Cambio"

                monto.requestFocus()

                Toast.makeText(
                    this,
                    "Abre Lumi Cambio y consulta la tasa.",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            // CONVERSIÓN A QUETZALES

            val montoQuetzales =
                cantidadOriginal * tasaCambio

            if (
                !montoQuetzales.isFinite() ||
                montoQuetzales <= 0.0
            ) {

                monto.error =
                    "Monto convertido inválido"

                monto.requestFocus()
                return@setOnClickListener
            }

            // PROTEGER AHORROS RESERVADOS

            if (tipo == "GASTO") {

                val disponible =
                    baseDatos.obtenerTotalIngresos() -
                            baseDatos.obtenerTotalGastos() -
                            totalAhorrado()

                if (
                    montoQuetzales > disponible + 0.000001
                ) {

                    monto.error =
                        "Dinero libre insuficiente. " +
                                "Disponible: " +
                                formatoQuetzales(
                                    disponible.coerceAtLeast(0.0)
                                )

                    monto.requestFocus()
                    return@setOnClickListener
                }
            }

            val categoriaSeleccionada =
                categorias.selectedItem.toString()

            // VALIDAR OTROS

            val detalleOtro =
                campoOtros.text.toString().trim()

            if (
                categoriaSeleccionada == "Otros" &&
                detalleOtro.isEmpty()
            ) {

                campoOtros.error =
                    "Especifica el tipo de movimiento"

                campoOtros.requestFocus()
                return@setOnClickListener
            }

            val descripcionBase =
                if (categoriaSeleccionada == "Otros") {
                    "$textoDescripcion · $detalleOtro"
                } else {
                    textoDescripcion
                }

            // IDENTIFICAR LOS DÓLARES EN EL HISTORIAL

            val descripcionFinal =
                if (monedaOriginal == "USD") {

                    val originalFormateado = String.format(
                        Locale.US,
                        "%.2f",
                        cantidadOriginal
                    )

                    "$descripcionBase · US$$originalFormateado"

                } else {
                    descripcionBase
                }

            val fechaActual =
                SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.US
                ).format(Date())

            try {

                val resultado =
                    baseDatos.guardarMovimiento(
                        descripcion = descripcionFinal,
                        monto = montoQuetzales,
                        categoria = categoriaSeleccionada,
                        fecha = fechaActual,
                        tipo = tipo,
                        monedaOriginal = monedaOriginal,
                        montoOriginal = cantidadOriginal,
                        tasaCambio = tasaCambio
                    )

                if (resultado != -1L) {

                    Toast.makeText(
                        this,
                        if (tipo == "INGRESO") {
                            "Ingreso guardado correctamente"
                        } else {
                            "Gasto guardado correctamente"
                        },
                        Toast.LENGTH_SHORT
                    ).show()

                    Log.d(
                        "LUMI_DB",
                        "Movimiento guardado. ID: $resultado"
                    )

                    dialogo.dismiss()
                    actualizarPantalla()

                } else {

                    Toast.makeText(
                        this,
                        "Error al guardar el movimiento",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {

                Log.e(
                    "LUMI_DB",
                    "Error al guardar movimiento",
                    e
                )

                Toast.makeText(
                    this,
                    e.message ?: "No se pudo guardar el movimiento",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // MOSTRAR DIÁLOGO

        dialogo.show()

        dialogo.window?.setSoftInputMode(
            WindowManager.LayoutParams
                .SOFT_INPUT_ADJUST_RESIZE
        )

        val anchoPantalla =
            resources.displayMetrics.widthPixels

        val altoPantalla =
            resources.displayMetrics.heightPixels

        dialogo.window?.setLayout(
            (anchoPantalla * 0.96).toInt(),
            (altoPantalla * 0.88).toInt()
        )
    }

    // =====================================================
    // ICONOS DE CATEGORÍAS
    // =====================================================

    private fun crearIconoCategoria(
        indice: Int,
        colorIcono: Int
    ): Drawable {

        return object : Drawable() {

            private val pincel = Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {
                color = colorIcono
                style = Paint.Style.STROKE
                strokeWidth = 2.3f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }

            override fun draw(canvas: Canvas) {

                val area = bounds
                val guardado = canvas.save()

                canvas.translate(
                    area.left.toFloat(),
                    area.top.toFloat()
                )

                canvas.scale(
                    area.width() / 32f,
                    area.height() / 32f
                )

                fun linea(
                    x1: Float,
                    y1: Float,
                    x2: Float,
                    y2: Float
                ) {
                    canvas.drawLine(
                        x1, y1, x2, y2, pincel
                    )
                }

                when (indice) {

                    0 -> {
                        // ALIMENTACIÓN
                        linea(8f, 4f, 8f, 16f)
                        linea(5f, 4f, 5f, 12f)
                        linea(11f, 4f, 11f, 12f)
                        linea(5f, 12f, 11f, 12f)
                        linea(8f, 16f, 8f, 28f)

                        val cuchillo = Path().apply {
                            moveTo(23f, 4f)
                            lineTo(23f, 28f)
                            moveTo(23f, 4f)
                            cubicTo(
                                14f, 10f,
                                15f, 18f,
                                23f, 18f
                            )
                        }

                        canvas.drawPath(cuchillo, pincel)
                    }

                    1 -> {
                        // TRANSPORTE
                        canvas.drawRoundRect(
                            RectF(4f, 12f, 28f, 23f),
                            3f, 3f, pincel
                        )

                        val techo = Path().apply {
                            moveTo(8f, 12f)
                            lineTo(12f, 6f)
                            lineTo(22f, 6f)
                            lineTo(26f, 12f)
                        }

                        canvas.drawPath(techo, pincel)

                        canvas.drawCircle(
                            10f, 23f, 2.5f, pincel
                        )

                        canvas.drawCircle(
                            23f, 23f, 2.5f, pincel
                        )
                    }

                    2 -> {
                        // ESTUDIOS
                        val birrete = Path().apply {
                            moveTo(3f, 12f)
                            lineTo(16f, 5f)
                            lineTo(29f, 12f)
                            lineTo(16f, 19f)
                            close()
                        }

                        canvas.drawPath(birrete, pincel)

                        linea(9f, 17f, 9f, 23f)
                        linea(9f, 23f, 16f, 27f)
                        linea(16f, 27f, 23f, 23f)
                        linea(23f, 23f, 23f, 17f)
                        linea(29f, 12f, 29f, 23f)
                    }

                    3 -> {
                        // ENTRETENIMIENTO
                        canvas.drawRoundRect(
                            RectF(3f, 11f, 29f, 24f),
                            6f, 6f, pincel
                        )

                        linea(10f, 14f, 10f, 21f)
                        linea(6.5f, 17.5f, 13.5f, 17.5f)

                        canvas.drawCircle(
                            21f, 15f, 1.2f, pincel
                        )

                        canvas.drawCircle(
                            25f, 19f, 1.2f, pincel
                        )
                    }

                    4 -> {
                        // SALUD
                        val corazon = Path().apply {
                            moveTo(16f, 27f)

                            cubicTo(
                                11f, 22f,
                                3f, 17f,
                                3f, 11f
                            )

                            cubicTo(
                                3f, 3f,
                                12f, 2f,
                                16f, 9f
                            )

                            cubicTo(
                                20f, 2f,
                                29f, 3f,
                                29f, 11f
                            )

                            cubicTo(
                                29f, 17f,
                                21f, 22f,
                                16f, 27f
                            )
                        }

                        canvas.drawPath(corazon, pincel)
                    }

                    5 -> {
                        // HOGAR
                        val casa = Path().apply {
                            moveTo(4f, 15f)
                            lineTo(16f, 5f)
                            lineTo(28f, 15f)
                            moveTo(7f, 13f)
                            lineTo(7f, 27f)
                            lineTo(25f, 27f)
                            lineTo(25f, 13f)
                        }

                        canvas.drawPath(casa, pincel)

                        canvas.drawRect(
                            13f, 19f,
                            19f, 27f,
                            pincel
                        )
                    }

                    6 -> {
                        // COMPRAS
                        canvas.drawRoundRect(
                            RectF(7f, 11f, 25f, 28f),
                            2f, 2f, pincel
                        )

                        val asa = Path().apply {
                            moveTo(12f, 13f)
                            lineTo(12f, 9f)

                            cubicTo(
                                12f, 2f,
                                20f, 2f,
                                20f, 9f
                            )

                            lineTo(20f, 13f)
                        }

                        canvas.drawPath(asa, pincel)
                    }

                    else -> {
                        // OTROS
                        pincel.style = Paint.Style.FILL

                        canvas.drawCircle(
                            7f, 16f, 2.5f, pincel
                        )

                        canvas.drawCircle(
                            16f, 16f, 2.5f, pincel
                        )

                        canvas.drawCircle(
                            25f, 16f, 2.5f, pincel
                        )

                        pincel.style = Paint.Style.STROKE
                    }
                }

                canvas.restoreToCount(guardado)
            }

            override fun setAlpha(alpha: Int) {
                pincel.alpha = alpha
                invalidateSelf()
            }

            override fun setColorFilter(
                colorFilter: ColorFilter?
            ) {
                pincel.colorFilter = colorFilter
                invalidateSelf()
            }

            @Deprecated("Deprecated in Java")
            override fun getOpacity(): Int {
                return PixelFormat.TRANSLUCENT
            }

            override fun getIntrinsicWidth(): Int = 32

            override fun getIntrinsicHeight(): Int = 32
        }
    }

    // =====================================================
    // FONDO DE SELECCIÓN
    // =====================================================

    private fun fondoSeleccion(
        colorFondo: Int,
        colorBorde: Int,
        seleccionado: Boolean
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(colorFondo)
            cornerRadius = dp(14).toFloat()

            if (seleccionado) {
                setStroke(
                    dp(2),
                    colorBorde
                )
            }
        }
    }

    // =====================================================
    // CONFIGURAR PRESUPUESTO
    // =====================================================

    private fun mostrarDialogoPresupuesto() {

        val mesActual = obtenerClaveMes()
        val presupuestoActual = obtenerPresupuestoMes()

        val campo = EditText(this).apply {

            hint = "Ejemplo: 1000.00"

            inputType =
                InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL

            if (presupuestoActual > 0.0) {
                setText(
                    String.format(
                        Locale.US,
                        "%.2f",
                        presupuestoActual
                    )
                )
            }

            setSelectAllOnFocus(true)
        }

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(24),
                dp(12),
                dp(24),
                0
            )

            addView(campo)
        }

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Presupuesto mensual")
            .setMessage(
                "Establece el límite de gastos para $mesActual."
            )
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialogo.setOnShowListener {

            dialogo.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val cantidad =
                    campo.text.toString()
                        .trim()
                        .toDoubleOrNull()

                if (
                    cantidad == null ||
                    !cantidad.isFinite() ||
                    cantidad <= 0.0
                ) {

                    campo.error =
                        "Ingresa un presupuesto mayor que cero"

                    campo.requestFocus()
                    return@setOnClickListener
                }

                preferencias.edit()
                    .putLong(
                        "presupuesto_$mesActual",
                        java.lang.Double.doubleToRawLongBits(
                            cantidad
                        )
                    )
                    .apply()

                actualizarPresupuesto()

                Toast.makeText(
                    this,
                    "Presupuesto mensual guardado",
                    Toast.LENGTH_SHORT
                ).show()

                dialogo.dismiss()
            }
        }

        dialogo.show()
    }

    private fun obtenerClaveMes(): String {

        return SimpleDateFormat(
            "yyyy-MM",
            Locale.US
        ).format(Date())
    }

    private fun obtenerPresupuestoMes(): Double {

        val clave =
            "presupuesto_${obtenerClaveMes()}"

        if (!preferencias.contains(clave)) {
            return 0.0
        }

        return java.lang.Double.longBitsToDouble(
            preferencias.getLong(clave, 0L)
        )
    }

    // =====================================================
    // ACTUALIZAR PRESUPUESTO
    // =====================================================

    private fun actualizarPresupuesto() {

        val presupuesto = obtenerPresupuestoMes()
        val claveMes = obtenerClaveMes()

        val gastosMes = baseDatos
            .obtenerTodosLosMovimientos()
            .filter {
                it.tipo.equals(
                    "GASTO",
                    ignoreCase = true
                ) && it.fecha.startsWith(claveMes)
            }
            .sumOf { it.monto }

        val txtMes = findViewById<TextView>(
            R.id.txtMesPresupuesto
        )

        val txtPresupuesto = findViewById<TextView>(
            R.id.txtPresupuesto
        )

        val txtGastado = findViewById<TextView>(
            R.id.txtGastadoMes
        )

        val txtDisponible = findViewById<TextView>(
            R.id.txtDisponiblePresupuesto
        )

        val txtAlerta = findViewById<TextView>(
            R.id.txtAlertaPresupuesto
        )

        val progreso = findViewById<ProgressBar>(
            R.id.progresoPresupuesto
        )

        val idioma = Locale("es", "GT")

        val nombreMes =
            SimpleDateFormat(
                "MMMM yyyy",
                idioma
            ).format(Date())
                .replaceFirstChar {
                    it.titlecase(idioma)
                }

        txtMes.text = nombreMes

        txtGastado.text =
            formatoQuetzales(gastosMes)

        if (presupuesto <= 0.0) {

            txtPresupuesto.text = "Sin configurar"
            txtDisponible.text = "Q 0.00"

            txtAlerta.text =
                "Establece un límite para controlar tus gastos."

            txtAlerta.setTextColor(morado)

            progreso.progress = 0

            progreso.progressTintList =
                ColorStateList.valueOf(morado)

            return
        }

        txtPresupuesto.text =
            formatoQuetzales(presupuesto)

        val disponible = presupuesto - gastosMes

        val porcentaje =
            (gastosMes / presupuesto) * 100.0

        progreso.progress =
            porcentaje.coerceIn(0.0, 100.0).toInt()

        txtDisponible.text =
            if (disponible >= 0.0) {
                formatoQuetzales(disponible)
            } else {
                "Excedido: " +
                        formatoQuetzales(-disponible)
            }

        when {

            porcentaje >= 100.0 -> {

                txtAlerta.text =
                    "Atención: alcanzaste o superaste tu presupuesto."

                txtAlerta.setTextColor(rojo)

                progreso.progressTintList =
                    ColorStateList.valueOf(rojo)
            }

            porcentaje >= 80.0 -> {

                txtAlerta.text =
                    "Cuidado: utilizaste el 80% o más de tu presupuesto."

                txtAlerta.setTextColor(
                    Color.parseColor("#B36A00")
                )

                progreso.progressTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#E9A23B")
                    )
            }

            else -> {

                txtAlerta.text =
                    "Tus gastos están dentro del presupuesto."

                txtAlerta.setTextColor(verde)

                progreso.progressTintList =
                    ColorStateList.valueOf(morado)
            }
        }
    }

    // =====================================================
    // ACTIVIDAD RECIENTE
    // =====================================================

    private fun mostrarMovimientosRecientes() {

        val contenedor = findViewById<LinearLayout>(
            R.id.contenedorMovimientosRecientes
        )

        contenedor.removeAllViews()

        val movimientos = baseDatos
            .obtenerTodosLosMovimientos()
            .sortedByDescending { it.fecha }
            .take(3)

        if (movimientos.isEmpty()) {

            val tarjetaVacia = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                setPadding(
                    dp(20),
                    dp(24),
                    dp(20),
                    dp(24)
                )

                background =
                    crearFondoTarjeta(Color.WHITE)
            }

            val mensaje = TextView(this).apply {
                text =
                    "Todavía no tienes movimientos registrados."

                textSize = 14f
                setTextColor(gris)
                gravity = Gravity.CENTER
            }

            tarjetaVacia.addView(mensaje)
            contenedor.addView(tarjetaVacia)
            return
        }

        for (movimiento in movimientos) {

            val esIngreso =
                movimiento.tipo.equals(
                    "INGRESO",
                    ignoreCase = true
                )

            val colorPrincipal =
                if (esIngreso) verde else rojo

            val colorFondoIcono =
                if (esIngreso) {
                    Color.parseColor("#E5F6ED")
                } else {
                    Color.parseColor("#FDECF1")
                }

            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                setPadding(
                    dp(15),
                    dp(15),
                    dp(15),
                    dp(15)
                )

                background =
                    crearFondoTarjeta(Color.WHITE)

                elevation = dp(2).toFloat()
            }

            val filaPrincipal = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val icono = TextView(this).apply {

                text = if (esIngreso) "↓" else "↑"
                textSize = 24f
                gravity = Gravity.CENTER
                setTextColor(colorPrincipal)

                background = GradientDrawable().apply {
                    setColor(colorFondoIcono)
                    cornerRadius = dp(13).toFloat()
                }
            }

            filaPrincipal.addView(
                icono,
                LinearLayout.LayoutParams(
                    dp(44),
                    dp(44)
                )
            )

            val informacion = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                setPadding(
                    dp(11),
                    0,
                    dp(6),
                    0
                )
            }

            val descripcion = TextView(this).apply {
                text = movimiento.descripcion
                textSize = 13f
                setTextColor(oscuro)
                setTypeface(null, Typeface.BOLD)
                maxLines = 2
            }

            val categoria = TextView(this).apply {
                text = movimiento.categoria
                textSize = 11f
                setTextColor(gris)

                setPadding(
                    0,
                    dp(4),
                    0,
                    0
                )
            }

            informacion.addView(descripcion)
            informacion.addView(categoria)

            filaPrincipal.addView(
                informacion,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            val simbolo =
                if (esIngreso) "+" else "-"

            val cantidad = TextView(this).apply {

                text =
                    "$simbolo " +
                            formatoQuetzales(movimiento.monto)

                textSize = 13f
                setTypeface(null, Typeface.BOLD)
                setTextColor(colorPrincipal)
                gravity = Gravity.END
            }

            filaPrincipal.addView(cantidad)
            tarjeta.addView(filaPrincipal)

            val separador = View(this).apply {
                setBackgroundColor(
                    Color.parseColor("#F0EBF7")
                )
            }

            tarjeta.addView(
                separador,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply {
                    topMargin = dp(13)
                    bottomMargin = dp(10)
                }
            )

            val fecha = TextView(this).apply {
                text = movimiento.fecha
                textSize = 11f
                setTextColor(gris)
            }

            tarjeta.addView(fecha)

            contenedor.addView(
                tarjeta,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(12)
                }
            )
        }
    }

    // =====================================================
    // FONDOS REDONDEADOS
    // =====================================================

    private fun crearFondoTarjeta(
        color: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(18).toFloat()

            setStroke(
                dp(1),
                Color.parseColor("#EEE8F5")
            )
        }
    }

    private fun dp(valor: Int): Int {
        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }

    // =====================================================
    // ACTUALIZAR PANTALLA PRINCIPAL
    // =====================================================

    private fun actualizarPantalla() {

        val totalIngresos =
            baseDatos.obtenerTotalIngresos()

        val totalGastos =
            baseDatos.obtenerTotalGastos()

        val saldoTotal =
            totalIngresos - totalGastos

        val reservado = totalAhorrado()

        val libre = saldoTotal - reservado

        findViewById<TextView>(
            R.id.txtSaldo
        ).text = formatoQuetzales(saldoTotal)

        findViewById<TextView>(
            R.id.txtAhorroReservado
        ).text = formatoQuetzales(reservado)

        findViewById<TextView>(
            R.id.txtDineroDisponible
        ).text = formatoQuetzales(libre)

        val aviso = findViewById<TextView>(
            R.id.txtAvisoDisponible
        )

        if (libre < -0.000001) {

            aviso.text =
                "Tus movimientos superan el dinero libre. " +
                        "Revisa los gastos y tus metas."

            aviso.setTextColor(rojo)

        } else {

            aviso.text =
                "Tus ahorros se mantienen separados de tus gastos."

            aviso.setTextColor(
                Color.parseColor("#625B73")
            )
        }

        findViewById<TextView>(
            R.id.txtIngresos
        ).text = formatoQuetzales(totalIngresos)

        findViewById<TextView>(
            R.id.txtGastos
        ).text = formatoQuetzales(totalGastos)

        mostrarMovimientosRecientes()
        actualizarPresupuesto()

        Log.d(
            "LUMI_DB",
            "Saldo total: $saldoTotal, " +
                    "Reservado: $reservado, " +
                    "Libre: $libre"
        )
    }

    // =====================================================
    // ACTUALIZAR AL REGRESAR A INICIO
    // =====================================================

    override fun onResume() {
        super.onResume()

        if (::baseDatos.isInitialized) {
            actualizarPantalla()
        }
    }

    // =====================================================
    // FORMATO MONETARIO
    // =====================================================

    private fun formatoQuetzales(
        monto: Double
    ): String {

        return String.format(
            Locale.US,
            "Q %.2f",
            monto
        )
    }

    // =====================================================
    // CERRAR CONEXIÓN
    // =====================================================

    override fun onDestroy() {

        if (::baseDatos.isInitialized) {
            baseDatos.close()
        }

        super.onDestroy()
    }
}


