package edu.umg.lumifinanzas

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import edu.umg.lumifinanzas.database.LumiDatabaseHelper
import edu.umg.lumifinanzas.database.MetasDatabaseHelper
import java.util.Locale

class HistorialActivity : AppCompatActivity() {

    private lateinit var baseDatos: LumiDatabaseHelper
    private lateinit var baseMetas: MetasDatabaseHelper

    private var filtroActual = "TODOS"

    private val morado = Color.parseColor("#6741B8")
    private val verde = Color.parseColor("#176B45")
    private val rojo = Color.parseColor("#AE244C")
    private val textoOscuro = Color.parseColor("#30244D")
    private val textoSecundario = Color.parseColor("#777083")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_historial)

        baseDatos = LumiDatabaseHelper(this)
        baseMetas = MetasDatabaseHelper(this)

        findViewById<Button>(
            R.id.btnVolverInicio
        ).setOnClickListener {
            finish()
        }

        findViewById<Button>(
            R.id.btnFiltroTodos
        ).setOnClickListener {
            cambiarFiltro("TODOS")
        }

        findViewById<Button>(
            R.id.btnFiltroIngresos
        ).setOnClickListener {
            cambiarFiltro("INGRESO")
        }

        findViewById<Button>(
            R.id.btnFiltroGastos
        ).setOnClickListener {
            cambiarFiltro("GASTO")
        }

        mostrarHistorial()
    }

    private fun cambiarFiltro(filtro: String) {
        filtroActual = filtro
        mostrarHistorial()
    }

    private fun formatoQuetzales(monto: Double): String {
        return String.format(
            Locale.US,
            "Q %.2f",
            monto
        )
    }

    private fun mostrarHistorial() {

        val contenedor =
            findViewById<LinearLayout>(
                R.id.contenedorMovimientos
            )

        val txtTotal =
            findViewById<TextView>(
                R.id.txtTotalMovimientos
            )

        val txtIngresos =
            findViewById<TextView>(
                R.id.txtIngresosHistorial
            )

        val txtGastos =
            findViewById<TextView>(
                R.id.txtGastosHistorial
            )

        contenedor.removeAllViews()

        val movimientos =
            baseDatos.obtenerTodosLosMovimientos()

        val totalIngresos = movimientos
            .filter {
                it.tipo.equals(
                    "INGRESO",
                    ignoreCase = true
                )
            }
            .sumOf { it.monto }

        val totalGastos = movimientos
            .filter {
                it.tipo.equals(
                    "GASTO",
                    ignoreCase = true
                )
            }
            .sumOf { it.monto }

        txtIngresos.text =
            formatoQuetzales(totalIngresos)

        txtGastos.text =
            formatoQuetzales(totalGastos)

        val movimientosFiltrados =
            movimientos.filter { movimiento ->
                filtroActual == "TODOS" ||
                        movimiento.tipo.equals(
                            filtroActual,
                            ignoreCase = true
                        )
            }

        txtTotal.text =
            "Movimientos registrados: ${movimientosFiltrados.size}"

        actualizarBotonesFiltro()

        if (movimientosFiltrados.isEmpty()) {

            val tarjetaVacia =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER

                    setPadding(
                        dp(18),
                        dp(28),
                        dp(18),
                        dp(28)
                    )

                    background = fondoTarjeta(
                        Color.WHITE,
                        "#E8DDF5"
                    )
                }

            val titulo = TextView(this).apply {
                text = "Sin movimientos"
                textSize = 17f
                setTextColor(textoOscuro)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.CENTER
            }

            val descripcion = TextView(this).apply {
                text = if (filtroActual == "TODOS") {
                    "Aún no tienes movimientos registrados."
                } else {
                    "No hay movimientos para este filtro."
                }

                textSize = 13f
                setTextColor(textoSecundario)
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            }

            tarjetaVacia.addView(titulo)
            tarjetaVacia.addView(descripcion)
            contenedor.addView(tarjetaVacia)

            return
        }

        for (movimiento in movimientosFiltrados) {

            val esIngreso = movimiento.tipo.equals(
                "INGRESO",
                ignoreCase = true
            )

            val colorMonto =
                if (esIngreso) verde else rojo

            val colorFondoIcono =
                if (esIngreso) "#E8F6EE" else "#FFF0F3"

            val simbolo =
                if (esIngreso) "+" else "-"

            val tarjeta = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL

                setPadding(
                    dp(16),
                    dp(16),
                    dp(16),
                    dp(14)
                )

                background = fondoTarjeta(
                    Color.WHITE,
                    "#E8DDF5"
                )
            }

            val encabezado = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val icono = TextView(this).apply {
                text = if (esIngreso) "↓" else "↑"
                textSize = 24f
                gravity = Gravity.CENTER
                setTextColor(colorMonto)
                setTypeface(null, Typeface.BOLD)

                background = GradientDrawable().apply {
                    setColor(
                        Color.parseColor(colorFondoIcono)
                    )
                    cornerRadius = dp(12).toFloat()
                }
            }

            encabezado.addView(
                icono,
                LinearLayout.LayoutParams(
                    dp(44),
                    dp(44)
                )
            )

            val informacion = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(6), 0)
            }

            val descripcion = TextView(this).apply {
                text = movimiento.descripcion
                textSize = 15f
                setTextColor(textoOscuro)
                setTypeface(null, Typeface.BOLD)
                maxLines = 2
            }

            val categoria = TextView(this).apply {
                text = movimiento.categoria
                textSize = 12f
                setTextColor(textoSecundario)
                setPadding(0, dp(4), 0, 0)
            }

            informacion.addView(descripcion)
            informacion.addView(categoria)

            encabezado.addView(
                informacion,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            val monto = TextView(this).apply {
                text = String.format(
                    Locale.US,
                    "%s Q %.2f",
                    simbolo,
                    movimiento.monto
                )

                textSize = 15f
                setTextColor(colorMonto)
                setTypeface(null, Typeface.BOLD)
                gravity = Gravity.END
            }

            encabezado.addView(monto)
            tarjeta.addView(encabezado)

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
                    topMargin = dp(14)
                    bottomMargin = dp(12)
                }
            )

            val pie = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val fecha = TextView(this).apply {
                text = movimiento.fecha
                textSize = 12f
                setTextColor(textoSecundario)
            }

            pie.addView(
                fecha,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            val botonEliminar = Button(this).apply {
                text = "Eliminar"
                isAllCaps = false
                textSize = 12f
                minHeight = 0
                minimumHeight = 0
                minWidth = 0
                minimumWidth = 0
                setPadding(dp(12), 0, dp(12), 0)
                setTextColor(rojo)

                backgroundTintList =
                    ColorStateList.valueOf(
                        Color.parseColor("#FFF0F3")
                    )

                setOnClickListener {

                    AlertDialog.Builder(
                        this@HistorialActivity
                    )
                        .setTitle("Eliminar movimiento")
                        .setMessage(
                            "¿Deseas eliminar '${movimiento.descripcion}'?\n\n" +
                                    "Esta acción no se puede deshacer."
                        )
                        .setNegativeButton(
                            "Cancelar",
                            null
                        )
                        .setPositiveButton(
                            "Eliminar"
                        ) { _, _ ->

                            eliminarMovimientoSeguro(
                                movimiento.id,
                                movimiento.tipo,
                                movimiento.monto
                            )
                        }
                        .show()
                }
            }

            pie.addView(
                botonEliminar,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(42)
                )
            )

            tarjeta.addView(pie)

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

    private fun actualizarBotonesFiltro() {

        configurarFiltro(
            R.id.btnFiltroTodos,
            filtroActual == "TODOS"
        )

        configurarFiltro(
            R.id.btnFiltroIngresos,
            filtroActual == "INGRESO"
        )

        configurarFiltro(
            R.id.btnFiltroGastos,
            filtroActual == "GASTO"
        )
    }

    private fun configurarFiltro(
        idBoton: Int,
        seleccionado: Boolean
    ) {

        val boton = findViewById<Button>(idBoton)

        boton.backgroundTintList =
            ColorStateList.valueOf(
                if (seleccionado) {
                    morado
                } else {
                    Color.parseColor("#EDE5FB")
                }
            )

        boton.setTextColor(
            if (seleccionado) {
                Color.WHITE
            } else {
                morado
            }
        )
    }

    private fun fondoTarjeta(
        colorFondo: Int,
        colorBorde: String
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(colorFondo)
            cornerRadius = dp(16).toFloat()

            setStroke(
                dp(1),
                Color.parseColor(colorBorde)
            )
        }
    }

    // ELIMINAR MOVIMIENTO SIN AFECTAR AHORROS

    private fun eliminarMovimientoSeguro(
        id: Int,
        tipo: String,
        monto: Double
    ) {

        val esIngreso = tipo.equals(
            "INGRESO",
            ignoreCase = true
        )

        if (esIngreso) {

            val saldoActual =
                baseDatos.obtenerTotalIngresos() -
                        baseDatos.obtenerTotalGastos()

            val ahorroReservado =
                baseMetas.obtenerTotalAhorrado()

            val saldoDespuesDeEliminar =
                saldoActual - monto

            if (
                saldoDespuesDeEliminar + 0.000001 <
                ahorroReservado
            ) {

                AlertDialog.Builder(this)
                    .setTitle("Ahorros protegidos")
                    .setMessage(
                        "No puedes eliminar este ingreso porque " +
                                "el saldo restante sería insuficiente " +
                                "para respaldar tus metas de ahorro.\n\n" +
                                "Primero retira dinero de tus metas " +
                                "o registra otro ingreso."
                    )
                    .setPositiveButton(
                        "Entendido",
                        null
                    )
                    .show()

                return
            }
        }

        val eliminado =
            baseDatos.eliminarMovimiento(id)

        if (eliminado) {

            Toast.makeText(
                this,
                "Movimiento eliminado correctamente",
                Toast.LENGTH_SHORT
            ).show()

            mostrarHistorial()

        } else {

            Toast.makeText(
                this,
                "No se pudo eliminar el movimiento",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun dp(valor: Int): Int {
        return (
                valor * resources.displayMetrics.density
                ).toInt()
    }

    override fun onResume() {
        super.onResume()

        if (::baseDatos.isInitialized) {
            mostrarHistorial()
        }
    }

    override fun onDestroy() {

        if (::baseDatos.isInitialized) {
            baseDatos.close()
        }

        if (::baseMetas.isInitialized) {
            baseMetas.close()
        }

        super.onDestroy()
    }
}
