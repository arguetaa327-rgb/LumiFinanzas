
package edu.umg.lumifinanzas

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import edu.umg.lumifinanzas.database.LumiDatabaseHelper
import java.util.Locale

class EstadisticasActivity : AppCompatActivity() {

    private lateinit var baseDatos: LumiDatabaseHelper

    private val morado = Color.parseColor("#6741B8")
    private val verde = Color.parseColor("#176B45")
    private val rosado = Color.parseColor("#AE244C")
    private val oscuro = Color.parseColor("#30244D")
    private val gris = Color.parseColor("#777083")

    private val coloresCategorias = listOf(
        "#6741B8",
        "#27A467",
        "#E65B78",
        "#3F86C9",
        "#E9A23B",
        "#8B55D1",
        "#20A6A0",
        "#C875A3"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_estadisticas)

        baseDatos = LumiDatabaseHelper(this)

        findViewById<Button>(R.id.btnVolverEstadisticas)
            .setOnClickListener {
                finish()
            }

        actualizarEstadisticas()
    }

    private fun actualizarEstadisticas() {
        mostrarEstadisticas()
        mostrarGastosPorCategoria()
    }

    // =====================================================
    // ESTADÍSTICAS GENERALES
    // =====================================================

    private fun mostrarEstadisticas() {

        val ingresos = baseDatos.obtenerTotalIngresos()
        val gastos = baseDatos.obtenerTotalGastos()

        val saldo = ingresos - gastos
        val totalMovimientos = ingresos + gastos

        findViewById<TextView>(R.id.txtEstIngresos).text =
            formato(ingresos)

        findViewById<TextView>(R.id.txtEstGastos).text =
            formato(gastos)

        findViewById<TextView>(R.id.txtEstSaldo).text =
            formato(saldo)

        // Distribución relativa de ingresos y gastos
        val proporcionIngresos = if (totalMovimientos > 0.0) {
            ingresos / totalMovimientos
        } else {
            0.0
        }

        val proporcionGastos = if (totalMovimientos > 0.0) {
            gastos / totalMovimientos
        } else {
            0.0
        }

        val barraIngresos = findViewById<LinearLayout>(
            R.id.barraIngresos
        )

        val barraGastos = findViewById<LinearLayout>(
            R.id.barraGastos
        )

        val parametrosIngresos =
            barraIngresos.layoutParams as LinearLayout.LayoutParams

        parametrosIngresos.weight =
            proporcionIngresos.toFloat()

        barraIngresos.layoutParams = parametrosIngresos

        val parametrosGastos =
            barraGastos.layoutParams as LinearLayout.LayoutParams

        parametrosGastos.weight =
            proporcionGastos.toFloat()

        barraGastos.layoutParams = parametrosGastos

        findViewById<TextView>(
            R.id.txtPorcentajeIngresos
        ).text = String.format(
            Locale.US,
            "Ingresos: %.1f%%",
            proporcionIngresos * 100
        )

        findViewById<TextView>(
            R.id.txtPorcentajeGastos
        ).text = String.format(
            Locale.US,
            "Gastos: %.1f%%",
            proporcionGastos * 100
        )

        // Porcentaje de ingresos utilizados en gastos
        val porcentajeGastado = if (ingresos > 0.0) {
            (gastos / ingresos) * 100.0
        } else {
            0.0
        }

        val txtPorcentaje = findViewById<TextView>(
            R.id.txtPorcentajeGastado
        )

        val progreso = findViewById<ProgressBar>(
            R.id.progresoIngresosGastados
        )

        val mensajePorcentaje = findViewById<TextView>(
            R.id.txtMensajePorcentaje
        )

        txtPorcentaje.text = if (ingresos > 0.0) {
            String.format(
                Locale.US,
                "%.1f%%",
                porcentajeGastado
            )
        } else {
            "Sin ingresos"
        }

        progreso.progress = porcentajeGastado
            .coerceIn(0.0, 100.0)
            .toInt()

        when {

            ingresos <= 0.0 -> {

                mensajePorcentaje.text =
                    "Registra ingresos para calcular qué porcentaje has gastado."

                mensajePorcentaje.setTextColor(gris)

                progreso.progressTintList =
                    ColorStateList.valueOf(morado)
            }

            porcentajeGastado >= 100.0 -> {

                mensajePorcentaje.text =
                    "Tus gastos alcanzaron o superaron tus ingresos."

                mensajePorcentaje.setTextColor(rosado)

                progreso.progressTintList =
                    ColorStateList.valueOf(rosado)
            }

            porcentajeGastado >= 80.0 -> {

                mensajePorcentaje.text =
                    "Has utilizado el 80% o más de tus ingresos."

                mensajePorcentaje.setTextColor(
                    Color.parseColor("#B36A00")
                )

                progreso.progressTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#E9A23B")
                    )
            }

            else -> {

                mensajePorcentaje.text =
                    "Tus gastos están por debajo de tus ingresos."

                mensajePorcentaje.setTextColor(verde)

                progreso.progressTintList =
                    ColorStateList.valueOf(morado)
            }
        }

        // Consejo financiero
        val consejo = when {

            ingresos == 0.0 && gastos == 0.0 ->
                "Comienza registrando tus ingresos y gastos para conocer tus hábitos financieros."

            ingresos <= 0.0 && gastos > 0.0 ->
                "Tienes gastos registrados, pero todavía no has registrado ingresos. Revisa tus movimientos."

            gastos > ingresos ->
                "Tus gastos superan tus ingresos. Revisa las categorías con mayor consumo y considera establecer un presupuesto."

            gastos == ingresos && ingresos > 0.0 ->
                "Tus ingresos y gastos están equilibrados. Intenta reservar una parte de tus próximos ingresos para tus metas."

            gastos == 0.0 && ingresos > 0.0 ->
                "Ya tienes ingresos registrados. Puedes establecer un presupuesto y comenzar a organizar tus gastos."

            porcentajeGastado >= 80.0 ->
                "Has utilizado gran parte de tus ingresos. Identifica tus gastos prioritarios y evita compras innecesarias."

            else ->
                "Tus ingresos superan tus gastos. Continúa administrando tu dinero y reservando una parte para tus metas de ahorro."
        }

        findViewById<TextView>(
            R.id.txtConsejoFinanciero
        ).text = consejo
    }

    // =====================================================
    // GASTOS POR CATEGORÍA
    // =====================================================

    private fun mostrarGastosPorCategoria() {

        val contenedor = findViewById<LinearLayout>(
            R.id.contenedorCategorias
        )

        contenedor.removeAllViews()

        val movimientos =
            baseDatos.obtenerTodosLosMovimientos()

        val gastosPorCategoria = movimientos
            .filter {
                it.tipo.equals(
                    "GASTO",
                    ignoreCase = true
                )
            }
            .groupBy {
                it.categoria
            }
            .mapValues { (_, lista) ->
                lista.sumOf { it.monto }
            }
            .toList()
            .sortedByDescending {
                it.second
            }

        if (gastosPorCategoria.isEmpty()) {

            val mensaje = TextView(this).apply {

                text =
                    "Todavía no tienes gastos registrados. Cuando registres uno, aquí aparecerá su categoría."

                textSize = 13f
                setTextColor(gris)

                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(12)
                )
            }

            contenedor.addView(mensaje)
            return
        }

        val totalGastosCategorias =
            gastosPorCategoria.sumOf { it.second }

        val gastoMayor =
            gastosPorCategoria.maxOf { it.second }

        for ((indice, elemento) in gastosPorCategoria.withIndex()) {

            val categoria = elemento.first
            val monto = elemento.second

            val porcentaje = if (totalGastosCategorias > 0.0) {
                (monto / totalGastosCategorias) * 100.0
            } else {
                0.0
            }

            val proporcionBarra = if (gastoMayor > 0.0) {
                (monto / gastoMayor).toFloat()
                    .coerceIn(0f, 1f)
            } else {
                0f
            }

            val color = Color.parseColor(
                coloresCategorias[indice % coloresCategorias.size]
            )

            // Contenedor de cada categoría
            val tarjetaCategoria = LinearLayout(this).apply {

                orientation = LinearLayout.VERTICAL

                setPadding(
                    0,
                    dp(12),
                    0,
                    dp(12)
                )
            }

            // Fila con nombre y cantidad
            val fila = LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val indicador = TextView(this).apply {

                background = GradientDrawable().apply {
                    setColor(color)
                    cornerRadius = dp(4).toFloat()
                }
            }

            fila.addView(
                indicador,
                LinearLayout.LayoutParams(
                    dp(11),
                    dp(11)
                )
            )

            val nombre = TextView(this).apply {

                text = categoria
                textSize = 13f
                setTextColor(oscuro)
                setTypeface(null, Typeface.BOLD)

                setPadding(
                    dp(9),
                    0,
                    dp(6),
                    0
                )
            }

            fila.addView(
                nombre,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            val cantidad = TextView(this).apply {

                text = formato(monto)
                textSize = 13f
                setTextColor(oscuro)
                setTypeface(null, Typeface.BOLD)
            }

            fila.addView(cantidad)
            tarjetaCategoria.addView(fila)

            // Barra de la categoría
            val barraFondo = LinearLayout(this).apply {

                orientation = LinearLayout.HORIZONTAL

                background = GradientDrawable().apply {
                    setColor(
                        Color.parseColor("#EDE5FB")
                    )
                    cornerRadius = dp(6).toFloat()
                }

                clipToOutline = true
            }

            val barraColor = TextView(this).apply {

                background = GradientDrawable().apply {
                    setColor(color)
                    cornerRadius = dp(6).toFloat()
                }
            }

            barraFondo.addView(
                barraColor,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    proporcionBarra
                )
            )

            barraFondo.addView(
                TextView(this),
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f - proporcionBarra
                )
            )

            tarjetaCategoria.addView(
                barraFondo,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(10)
                ).apply {
                    topMargin = dp(11)
                }
            )

            // Porcentaje de la categoría
            val porcentajeTexto = TextView(this).apply {

                text = String.format(
                    Locale.US,
                    "%.1f%% del total de gastos",
                    porcentaje
                )

                textSize = 11f
                setTextColor(gris)

                setPadding(
                    0,
                    dp(7),
                    0,
                    0
                )
            }

            tarjetaCategoria.addView(porcentajeTexto)

            contenedor.addView(tarjetaCategoria)

            // Separador
            if (indice < gastosPorCategoria.size - 1) {

                val separador = TextView(this).apply {
                    setBackgroundColor(
                        Color.parseColor("#F0EBF7")
                    )
                }

                contenedor.addView(
                    separador,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(1)
                    )
                )
            }
        }
    }

    // =====================================================
    // FUNCIONES AUXILIARES
    // =====================================================

    private fun dp(valor: Int): Int {
        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }

    private fun formato(valor: Double): String {
        return String.format(
            Locale.US,
            "Q %.2f",
            valor
        )
    }

    override fun onResume() {
        super.onResume()

        if (::baseDatos.isInitialized) {
            actualizarEstadisticas()
        }
    }

    override fun onDestroy() {

        if (::baseDatos.isInitialized) {
            baseDatos.close()
        }

        super.onDestroy()
    }
}
