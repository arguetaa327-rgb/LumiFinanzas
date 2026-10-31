package edu.umg.lumifinanzas.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Locale

data class Movimiento(
    val id: Int,
    val descripcion: String,
    val monto: Double,
    val categoria: String,
    val fecha: String,
    val tipo: String,
    val monedaOriginal: String = "GTQ",
    val montoOriginal: Double = monto,
    val tasaCambio: Double = 1.0
)

class LumiDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "lumi_finanzas.db", null, 3) {

    private val contextoAplicacion = context.applicationContext

    companion object {
        const val TABLA_GASTOS = "gastos"
        const val COLUMNA_ID = "id"
        const val COLUMNA_DESCRIPCION = "descripcion"
        const val COLUMNA_MONTO = "monto"
        const val COLUMNA_CATEGORIA = "categoria"
        const val COLUMNA_FECHA = "fecha"
        const val COLUMNA_TIPO = "tipo"
        const val COLUMNA_MONEDA = "moneda_original"
        const val COLUMNA_MONTO_ORIGINAL = "monto_original"
        const val COLUMNA_TASA = "tasa_cambio"

        private const val TOLERANCIA = 0.000001
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLA_GASTOS (
                $COLUMNA_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMNA_DESCRIPCION TEXT NOT NULL,
                $COLUMNA_MONTO REAL NOT NULL,
                $COLUMNA_CATEGORIA TEXT NOT NULL,
                $COLUMNA_FECHA TEXT NOT NULL,
                $COLUMNA_TIPO TEXT NOT NULL DEFAULT 'GASTO',
                $COLUMNA_MONEDA TEXT NOT NULL DEFAULT 'GTQ',
                $COLUMNA_MONTO_ORIGINAL REAL,
                $COLUMNA_TASA REAL NOT NULL DEFAULT 1.0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE $TABLA_GASTOS " +
                        "ADD COLUMN $COLUMNA_TIPO TEXT NOT NULL DEFAULT 'GASTO'"
            )
        }

        if (oldVersion < 3) {
            db.execSQL(
                "ALTER TABLE $TABLA_GASTOS " +
                        "ADD COLUMN $COLUMNA_MONEDA TEXT NOT NULL DEFAULT 'GTQ'"
            )

            db.execSQL(
                "ALTER TABLE $TABLA_GASTOS " +
                        "ADD COLUMN $COLUMNA_MONTO_ORIGINAL REAL"
            )

            db.execSQL(
                "ALTER TABLE $TABLA_GASTOS " +
                        "ADD COLUMN $COLUMNA_TASA REAL NOT NULL DEFAULT 1.0"
            )

            db.execSQL(
                "UPDATE $TABLA_GASTOS " +
                        "SET $COLUMNA_MONTO_ORIGINAL = $COLUMNA_MONTO " +
                        "WHERE $COLUMNA_MONTO_ORIGINAL IS NULL"
            )
        }
    }

    private fun obtenerAhorroReservado(): Double {
        val db = MetasDatabaseHelper(contextoAplicacion)

        return try {
            db.obtenerTotalAhorrado()
        } finally {
            db.close()
        }
    }

    fun obtenerDineroLibre(): Double {
        return obtenerTotalIngresos() -
                obtenerTotalGastos() -
                obtenerAhorroReservado()
    }

    fun guardarMovimiento(
        descripcion: String,
        monto: Double,
        categoria: String,
        fecha: String,
        tipo: String,
        monedaOriginal: String = "GTQ",
        montoOriginal: Double = monto,
        tasaCambio: Double = 1.0
    ): Long {

        require(descripcion.isNotBlank()) {
            "La descripción es obligatoria"
        }

        require(monto.isFinite() && monto > 0.0) {
            "El monto debe ser mayor que cero"
        }

        require(tipo == "GASTO" || tipo == "INGRESO") {
            "Tipo de movimiento inválido"
        }

        require(
            monedaOriginal == "GTQ" ||
                    monedaOriginal == "USD"
        ) {
            "Moneda inválida"
        }

        require(
            montoOriginal.isFinite() &&
                    montoOriginal > 0.0
        ) {
            "Monto original inválido"
        }

        require(
            tasaCambio.isFinite() &&
                    tasaCambio > 0.0
        ) {
            "Tasa inválida"
        }

        if (
            tipo == "GASTO" &&
            monto > obtenerDineroLibre() + TOLERANCIA
        ) {
            throw IllegalArgumentException(
                "Fondos insuficientes. Disponible: " +
                        String.format(
                            Locale.US,
                            "Q %.2f",
                            obtenerDineroLibre()
                        )
            )
        }

        val valores = ContentValues().apply {
            put(COLUMNA_DESCRIPCION, descripcion.trim())
            put(COLUMNA_MONTO, monto)
            put(COLUMNA_CATEGORIA, categoria)
            put(COLUMNA_FECHA, fecha)
            put(COLUMNA_TIPO, tipo)
            put(COLUMNA_MONEDA, monedaOriginal)
            put(COLUMNA_MONTO_ORIGINAL, montoOriginal)
            put(COLUMNA_TASA, tasaCambio)
        }

        return writableDatabase.insert(
            TABLA_GASTOS,
            null,
            valores
        )
    }

    fun guardarGasto(
        descripcion: String,
        monto: Double,
        categoria: String,
        fecha: String
    ): Long {
        return guardarMovimiento(
            descripcion,
            monto,
            categoria,
            fecha,
            "GASTO"
        )
    }

    fun obtenerTotalGastos(): Double {
        return obtenerTotalPorTipo("GASTO")
    }

    fun obtenerTotalIngresos(): Double {
        return obtenerTotalPorTipo("INGRESO")
    }

    fun obtenerSaldoDisponible(): Double {
        return obtenerTotalIngresos() - obtenerTotalGastos()
    }

    private fun obtenerTotalPorTipo(tipo: String): Double {
        return readableDatabase.rawQuery(
            """
            SELECT COALESCE(SUM($COLUMNA_MONTO), 0)
            FROM $TABLA_GASTOS
            WHERE $COLUMNA_TIPO = ?
            """.trimIndent(),
            arrayOf(tipo)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getDouble(0)
            } else {
                0.0
            }
        }
    }

    fun obtenerTodosLosMovimientos(): List<Movimiento> {

        val movimientos = mutableListOf<Movimiento>()

        readableDatabase.query(
            TABLA_GASTOS,
            arrayOf(
                COLUMNA_ID,
                COLUMNA_DESCRIPCION,
                COLUMNA_MONTO,
                COLUMNA_CATEGORIA,
                COLUMNA_FECHA,
                COLUMNA_TIPO,
                COLUMNA_MONEDA,
                COLUMNA_MONTO_ORIGINAL,
                COLUMNA_TASA
            ),
            null,
            null,
            null,
            null,
            "$COLUMNA_ID DESC"
        ).use { cursor ->

            while (cursor.moveToNext()) {

                val monto = cursor.getDouble(2)

                movimientos.add(
                    Movimiento(
                        id = cursor.getInt(0),
                        descripcion = cursor.getString(1),
                        monto = monto,
                        categoria = cursor.getString(3),
                        fecha = cursor.getString(4),
                        tipo = cursor.getString(5),
                        monedaOriginal =
                            cursor.getString(6) ?: "GTQ",
                        montoOriginal =
                            if (cursor.isNull(7)) {
                                monto
                            } else {
                                cursor.getDouble(7)
                            },
                        tasaCambio = cursor.getDouble(8)
                    )
                )
            }
        }

        return movimientos
    }

    fun eliminarMovimiento(id: Int): Boolean {

        val movimiento =
            obtenerTodosLosMovimientos()
                .firstOrNull { it.id == id }
                ?: return false

        if (movimiento.tipo.equals("INGRESO", true)) {

            val saldoDespues =
                obtenerTotalIngresos() -
                        obtenerTotalGastos() -
                        movimiento.monto

            if (
                saldoDespues + TOLERANCIA <
                obtenerAhorroReservado()
            ) {
                return false
            }
        }

        return writableDatabase.delete(
            TABLA_GASTOS,
            "$COLUMNA_ID = ?",
            arrayOf(id.toString())
        ) > 0
    }

    fun obtenerGastosRecientes(): String {

        val movimientos =
            obtenerTodosLosMovimientos().take(5)

        if (movimientos.isEmpty()) {
            return "Aún no tienes movimientos registrados."
        }

        return movimientos.joinToString("\n") { movimiento ->

            val simbolo =
                if (
                    movimiento.tipo.equals(
                        "INGRESO",
                        true
                    )
                ) "+" else "-"

            val montoFormateado =
                String.format(
                    Locale.US,
                    "Q %.2f",
                    movimiento.monto
                )

            "• ${movimiento.descripcion} — " +
                    "$simbolo $montoFormateado"
        }
    }

    fun contarMovimientos(): Int {
        return obtenerTodosLosMovimientos().size
    }

    fun obtenerTotalPorCategoria(
        categoria: String,
        tipo: String = "GASTO"
    ): Double {

        return readableDatabase.rawQuery(
            """
            SELECT COALESCE(SUM($COLUMNA_MONTO), 0)
            FROM $TABLA_GASTOS
            WHERE $COLUMNA_CATEGORIA = ?
              AND $COLUMNA_TIPO = ?
            """.trimIndent(),
            arrayOf(categoria, tipo)
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getDouble(0)
            } else {
                0.0
            }
        }
    }
}
