package edu.umg.lumifinanzas

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import edu.umg.lumifinanzas.database.LumiDatabaseHelper
import edu.umg.lumifinanzas.database.MetaAhorro
import edu.umg.lumifinanzas.database.MetasDatabaseHelper
import java.util.Locale

class MetasActivity : AppCompatActivity() {

    private lateinit var baseMetas: MetasDatabaseHelper
    private lateinit var baseMovimientos: LumiDatabaseHelper

    private val morado = Color.parseColor("#6741B8")
    private val verde = Color.parseColor("#176B45")
    private val rojo = Color.parseColor("#AE244C")
    private val oscuro = Color.parseColor("#30244D")
    private val gris = Color.parseColor("#777083")

    private val coloresMetas = listOf(
        Color.parseColor("#6741B8"),
        Color.parseColor("#168F87"),
        Color.parseColor("#E0873C"),
        Color.parseColor("#B14D88"),
        Color.parseColor("#3E78BD")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_metas)

        baseMetas = MetasDatabaseHelper(this)
        baseMovimientos = LumiDatabaseHelper(this)

        findViewById<Button>(R.id.btnCrearMeta)
            .setOnClickListener {
                mostrarFormularioMeta()
            }

        findViewById<Button>(R.id.btnVolverMetas)
            .setOnClickListener {
                finish()
            }

        mostrarMetas()
    }

    // =====================================================
    // SALDO Y DINERO DISPONIBLE
    // =====================================================

    private fun obtenerSaldoTotal(): Double {
        return baseMovimientos.obtenerTotalIngresos() -
                baseMovimientos.obtenerTotalGastos()
    }

    private fun obtenerDisponible(): Double {
        return obtenerSaldoTotal() -
                baseMetas.obtenerTotalAhorrado()
    }

    // =====================================================
    // CREAR NUEVA META
    // =====================================================

    private fun mostrarFormularioMeta() {

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dp(24),
                dp(12),
                dp(24),
                0
            )
        }

        val nombre = EditText(this).apply {
            hint = "Ejemplo: Mi computadora"
            inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        }

        val objetivo = EditText(this).apply {
            hint = "Monto objetivo en Q"
            inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        contenedor.addView(nombre)
        contenedor.addView(objetivo)

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Nueva meta de ahorro")
            .setMessage(
                "Ponle un nombre a tu sueño y establece cuánto necesitas ahorrar."
            )
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Crear meta", null)
            .create()

        dialogo.setOnShowListener {

            dialogo.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val texto = nombre.text.toString().trim()

                val cantidad = objetivo.text.toString()
                    .trim()
                    .toDoubleOrNull()

                if (texto.isEmpty()) {
                    nombre.error = "Escribe un nombre"
                    nombre.requestFocus()
                    return@setOnClickListener
                }

                if (
                    cantidad == null ||
                    !cantidad.isFinite() ||
                    cantidad <= 0.0
                ) {
                    objetivo.error = "Ingresa un monto válido"
                    objetivo.requestFocus()
                    return@setOnClickListener
                }

                if (baseMetas.crearMeta(texto, cantidad)) {

                    dialogo.dismiss()
                    mostrarMetas()

                    Toast.makeText(
                        this,
                        "Meta creada correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "No se pudo crear la meta",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialogo.show()
    }

    // =====================================================
    // MOSTRAR METAS Y RESUMEN
    // =====================================================

    private fun mostrarMetas() {

        val contenedor = findViewById<LinearLayout>(
            R.id.contenedorMetas
        )

        contenedor.removeAllViews()

        val metas = baseMetas.obtenerMetas()

        val totalAhorrado =
            baseMetas.obtenerTotalAhorrado()

        findViewById<TextView>(
            R.id.txtTotalMetas
        ).text = moneda(totalAhorrado)

        val completadas = metas.count {
            it.objetivo > 0.0 &&
                    it.ahorrado >= it.objetivo
        }

        val activas = metas.size - completadas

        findViewById<TextView>(
            R.id.txtMetasActivas
        ).text = activas.toString()

        findViewById<TextView>(
            R.id.txtMetasCompletadas
        ).text = completadas.toString()

        val mensaje = when {

            metas.isEmpty() ->
                "Tu próximo gran logro comienza con una meta. Crea la primera y empieza a avanzar."

            completadas == metas.size ->
                "Has alcanzado todas tus metas actuales. Es un gran momento para plantearte nuevos objetivos."

            totalAhorrado > 0.0 ->
                "Cada aporte cuenta. Tu constancia está convirtiendo tus objetivos en resultados."

            else ->
                "Ya tienes tus objetivos definidos. El siguiente paso es comenzar a ahorrar."
        }

        findViewById<TextView>(
            R.id.txtMensajeMetas
        ).text = mensaje

        if (metas.isEmpty()) {

            val tarjetaVacia = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                setPadding(
                    dp(22),
                    dp(28),
                    dp(22),
                    dp(28)
                )

                background = fondo(
                    Color.WHITE,
                    dp(20)
                )

                gravity = Gravity.CENTER
            }

            val titulo = TextView(this).apply {

                text = "Tu primera meta te espera"

                textSize = 18f

                setTextColor(oscuro)

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                gravity = Gravity.CENTER
            }

            val descripcion = TextView(this).apply {

                text =
                    "Piensa en algo que quieras lograr. " +
                            "Lumi te ayudará a visualizar cada avance."

                textSize = 13f

                setTextColor(gris)

                gravity = Gravity.CENTER

                setPadding(
                    0,
                    dp(10),
                    0,
                    0
                )
            }

            tarjetaVacia.addView(titulo)
            tarjetaVacia.addView(descripcion)

            contenedor.addView(tarjetaVacia)

            return
        }

        metas.forEachIndexed { indice, meta ->

            val color = coloresMetas[
                indice % coloresMetas.size
            ]

            contenedor.addView(
                crearTarjeta(meta, color)
            )
        }
    }

    // =====================================================
    // TARJETAS PREMIUM DE METAS
    // =====================================================

    private fun crearTarjeta(
        meta: MetaAhorro,
        color: Int
    ): LinearLayout {

        val tarjeta = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(17),
                dp(18),
                dp(17),
                dp(18)
            )

            background = fondo(
                Color.WHITE,
                dp(21)
            )

            elevation = dp(3).toFloat()
        }

        tarjeta.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(16)
        }

        val porcentajeReal = if (meta.objetivo > 0.0) {
            (meta.ahorrado / meta.objetivo) * 100.0
        } else {
            0.0
        }

        val porcentajeVisual =
            porcentajeReal.coerceIn(0.0, 100.0).toInt()

        val completada =
            meta.objetivo > 0.0 &&
                    meta.ahorrado >= meta.objetivo

        val restante = (
                meta.objetivo - meta.ahorrado
                ).coerceAtLeast(0.0)

        // ENCABEZADO DE TARJETA
        val encabezado = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val icono = TextView(this).apply {

            text = if (completada) "✓" else "✦"

            textSize = 25f

            gravity = Gravity.CENTER

            setTextColor(color)

            background = fondo(
                colorConTransparencia(color),
                dp(14)
            )
        }

        encabezado.addView(
            icono,
            LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            )
        )

        val informacion = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(12),
                0,
                0,
                0
            )
        }

        val nombre = TextView(this).apply {

            text = meta.nombre

            textSize = 17f

            setTextColor(oscuro)

            setTypeface(
                null,
                Typeface.BOLD
            )

            maxLines = 2
        }

        val estado = TextView(this).apply {

            text = if (completada) {
                "META ALCANZADA"
            } else {
                "EN PROGRESO"
            }

            textSize = 10f

            letterSpacing = 0.08f

            setTypeface(
                null,
                Typeface.BOLD
            )

            setTextColor(
                if (completada) verde else color
            )

            setPadding(
                0,
                dp(6),
                0,
                0
            )
        }

        informacion.addView(nombre)
        informacion.addView(estado)

        encabezado.addView(
            informacion,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        tarjeta.addView(encabezado)

        // PROGRESO CIRCULAR Y CANTIDADES
        val filaProgreso = LinearLayout(this).apply {

            orientation = LinearLayout.HORIZONTAL

            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                0,
                dp(17),
                0,
                dp(13)
            )
        }

        val circulo = CirculoProgreso(
            porcentajeVisual,
            color
        )

        filaProgreso.addView(
            circulo,
            LinearLayout.LayoutParams(
                dp(126),
                dp(126)
            )
        )

        val cantidades = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(15),
                0,
                0,
                0
            )
        }

        cantidades.addView(
            textoSimple(
                "HAS AHORRADO",
                10f,
                gris,
                false
            )
        )

        cantidades.addView(
            textoSimple(
                moneda(meta.ahorrado),
                19f,
                color,
                true
            )
        )

        cantidades.addView(
            textoSimple(
                "OBJETIVO",
                10f,
                gris,
                false
            ).apply {
                setPadding(
                    0,
                    dp(12),
                    0,
                    0
                )
            }
        )

        cantidades.addView(
            textoSimple(
                moneda(meta.objetivo),
                15f,
                oscuro,
                true
            )
        )

        filaProgreso.addView(
            cantidades,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        tarjeta.addView(filaProgreso)

        // MENSAJE DE PROGRESO
        val mensajeProgreso = TextView(this).apply {

            text = if (completada) {

                "¡Lo lograste! Tu meta está completa."

            } else {

                "Te faltan ${moneda(restante)} para cumplir tu sueño."
            }

            textSize = 12f

            setTextColor(
                if (completada) verde else gris
            )

            setPadding(
                0,
                0,
                0,
                dp(15)
            )
        }

        tarjeta.addView(mensajeProgreso)

        // BOTÓN AGREGAR AHORRO
        val botonAgregar = Button(this).apply {

            text = if (completada) {
                "Meta completada"
            } else {
                "+ Agregar ahorro"
            }

            isAllCaps = false

            textSize = 14f

            setTypeface(
                null,
                Typeface.BOLD
            )

            setTextColor(Color.WHITE)

            backgroundTintList =
                ColorStateList.valueOf(color)

            isEnabled = !completada

            setOnClickListener {
                mostrarFormularioAhorro(meta)
            }
        }

        tarjeta.addView(
            botonAgregar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(51)
            )
        )

        // BOTÓN RETIRAR AHORRO
        val botonRetirar = Button(this).apply {

            text = "Retirar ahorro"

            isAllCaps = false

            setTextColor(morado)

            backgroundTintList =
                ColorStateList.valueOf(
                    Color.parseColor("#EDE5FB")
                )

            isEnabled = meta.ahorrado > 0.0

            setOnClickListener {
                mostrarFormularioRetiro(meta)
            }
        }

        tarjeta.addView(
            botonRetirar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(49)
            ).apply {
                topMargin = dp(7)
            }
        )

        // BOTÓN ELIMINAR META
        val botonEliminar = Button(this).apply {

            text = "Eliminar meta"

            isAllCaps = false

            setTextColor(rojo)

            backgroundTintList =
                ColorStateList.valueOf(
                    Color.parseColor("#FCE5E9")
                )

            setOnClickListener {

                AlertDialog.Builder(this@MetasActivity)
                    .setTitle("Eliminar meta")
                    .setMessage(
                        "¿Eliminar '${meta.nombre}'?\n\n" +
                                "Los ${moneda(meta.ahorrado)} " +
                                "reservados volverán a estar disponibles."
                    )
                    .setNegativeButton(
                        "Cancelar",
                        null
                    )
                    .setPositiveButton(
                        "Eliminar"
                    ) { _, _ ->

                        if (
                            baseMetas.eliminarMeta(meta.id)
                        ) {

                            mostrarMetas()

                            Toast.makeText(
                                this@MetasActivity,
                                "Meta eliminada",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            Toast.makeText(
                                this@MetasActivity,
                                "No se pudo eliminar la meta",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    .show()
            }
        }

        tarjeta.addView(
            botonEliminar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(49)
            ).apply {
                topMargin = dp(7)
            }
        )

        return tarjeta
    }

    // =====================================================
    // AGREGAR AHORRO
    // =====================================================

    private fun mostrarFormularioAhorro(
        meta: MetaAhorro
    ) {

        val disponible = obtenerDisponible()

        val restante = (
                meta.objetivo - meta.ahorrado
                ).coerceAtLeast(0.0)

        val campo = crearCampoMonto()

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Ahorrar para ${meta.nombre}")
            .setMessage(
                "Dinero disponible: ${moneda(disponible)}\n" +
                        "Falta para tu meta: ${moneda(restante)}"
            )
            .setView(crearContenedor(campo))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Agregar", null)
            .create()

        dialogo.setOnShowListener {

            dialogo.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val cantidad = campo.text.toString()
                    .trim()
                    .toDoubleOrNull()

                val disponibleActual =
                    obtenerDisponible()

                if (
                    cantidad == null ||
                    !cantidad.isFinite() ||
                    cantidad <= 0.0 ||
                    cantidad > restante ||
                    cantidad > disponibleActual
                ) {

                    campo.error =
                        "El monto debe ser positivo y no superar " +
                                "tu dinero disponible ni lo que falta para la meta."

                    campo.requestFocus()

                    return@setOnClickListener
                }

                if (
                    baseMetas.agregarAhorro(
                        meta.id,
                        cantidad,
                        obtenerSaldoTotal()
                    )
                ) {

                    dialogo.dismiss()
                    mostrarMetas()

                    Toast.makeText(
                        this,
                        "Ahorro registrado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "No se pudo agregar el ahorro",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialogo.show()
    }

    // =====================================================
    // RETIRAR AHORRO
    // =====================================================

    private fun mostrarFormularioRetiro(
        meta: MetaAhorro
    ) {

        val campo = crearCampoMonto()

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Retirar ahorro")
            .setMessage(
                "Tienes ${moneda(meta.ahorrado)} " +
                        "reservados para ${meta.nombre}."
            )
            .setView(crearContenedor(campo))
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Retirar", null)
            .create()

        dialogo.setOnShowListener {

            dialogo.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                val cantidad = campo.text.toString()
                    .trim()
                    .toDoubleOrNull()

                if (
                    cantidad == null ||
                    !cantidad.isFinite() ||
                    cantidad <= 0.0 ||
                    cantidad > meta.ahorrado
                ) {

                    campo.error =
                        "Ingresa una cantidad válida"

                    campo.requestFocus()

                    return@setOnClickListener
                }

                if (
                    baseMetas.retirarAhorro(
                        meta.id,
                        cantidad
                    )
                ) {

                    dialogo.dismiss()
                    mostrarMetas()

                    Toast.makeText(
                        this,
                        "Dinero liberado correctamente",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    Toast.makeText(
                        this,
                        "No se pudo retirar el ahorro",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        dialogo.show()
    }

    // =====================================================
    // PROGRESO CIRCULAR PERSONALIZADO
    // =====================================================

    private inner class CirculoProgreso(
        private val porcentaje: Int,
        private val color: Int
    ) : View(this@MetasActivity) {

        private val pintura = Paint(
            Paint.ANTI_ALIAS_FLAG
        )

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            val centroX = width / 2f
            val centroY = height / 2f

            val radio = (
                    minOf(width, height) / 2f
                    ) - dp(13)

            val rectangulo = RectF(
                centroX - radio,
                centroY - radio,
                centroX + radio,
                centroY + radio
            )

            pintura.style = Paint.Style.STROKE
            pintura.strokeWidth = dp(12).toFloat()
            pintura.strokeCap = Paint.Cap.ROUND
            pintura.color = Color.parseColor("#EEE9FB")

            canvas.drawArc(
                rectangulo,
                0f,
                360f,
                false,
                pintura
            )

            pintura.color = color

            canvas.drawArc(
                rectangulo,
                -90f,
                porcentaje * 3.6f,
                false,
                pintura
            )

            pintura.style = Paint.Style.FILL
            pintura.textAlign = Paint.Align.CENTER
            pintura.typeface = Typeface.create(
                Typeface.DEFAULT,
                Typeface.BOLD
            )

            pintura.color = color
            pintura.textSize = dp(24).toFloat()

            canvas.drawText(
                "$porcentaje%",
                centroX,
                centroY + dp(3),
                pintura
            )

            pintura.typeface = Typeface.DEFAULT
            pintura.color = gris
            pintura.textSize = dp(10).toFloat()

            canvas.drawText(
                "completado",
                centroX,
                centroY + dp(20),
                pintura
            )
        }
    }

    // =====================================================
    // FUNCIONES AUXILIARES
    // =====================================================

    private fun crearCampoMonto(): EditText {

        return EditText(this).apply {

            hint = "Cantidad en Q"

            inputType =
                InputType.TYPE_CLASS_NUMBER or
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
    }

    private fun crearContenedor(
        campo: EditText
    ): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(24),
                dp(12),
                dp(24),
                0
            )

            addView(campo)
        }
    }

    private fun textoSimple(
        contenido: String,
        tamano: Float,
        color: Int,
        negrita: Boolean
    ): TextView {

        return TextView(this).apply {

            text = contenido

            textSize = tamano

            setTextColor(color)

            if (negrita) {
                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

            setPadding(
                0,
                dp(3),
                0,
                0
            )
        }
    }

    private fun fondo(
        color: Int,
        radio: Int
    ): GradientDrawable {

        return GradientDrawable().apply {

            setColor(color)

            cornerRadius = radio.toFloat()
        }
    }

    private fun colorConTransparencia(
        color: Int
    ): Int {

        return Color.argb(
            35,
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    private fun moneda(valor: Double): String {

        return String.format(
            Locale.US,
            "Q %.2f",
            valor
        )
    }

    private fun dp(valor: Int): Int {

        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }

    override fun onResume() {
        super.onResume()

        if (::baseMetas.isInitialized) {
            mostrarMetas()
        }
    }

    override fun onDestroy() {

        if (::baseMetas.isInitialized) {
            baseMetas.close()
        }

        if (::baseMovimientos.isInitialized) {
            baseMovimientos.close()
        }

        super.onDestroy()
    }
}
