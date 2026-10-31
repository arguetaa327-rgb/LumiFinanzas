package edu.umg.lumifinanzas.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class MetaAhorro(
    val id: Int,
    val nombre: String,
    val objetivo: Double,
    val ahorrado: Double
)

class MetasDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "lumi_metas.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE metas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL,
                objetivo REAL NOT NULL,
                ahorrado REAL NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // No eliminar datos existentes.
    }

    fun crearMeta(nombre: String, objetivo: Double): Boolean {
        if (
            nombre.isBlank() ||
            !objetivo.isFinite() ||
            objetivo <= 0.0
        ) return false

        val valores = ContentValues().apply {
            put("nombre", nombre.trim())
            put("objetivo", objetivo)
            put("ahorrado", 0.0)
        }

        return writableDatabase.insert(
            "metas",
            null,
            valores
        ) != -1L
    }

    fun obtenerMetas(): List<MetaAhorro> {
        val lista = mutableListOf<MetaAhorro>()

        readableDatabase.rawQuery(
            """
            SELECT id, nombre, objetivo, ahorrado
            FROM metas
            ORDER BY id DESC
            """.trimIndent(),
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                lista.add(
                    MetaAhorro(
                        id = cursor.getInt(0),
                        nombre = cursor.getString(1),
                        objetivo = cursor.getDouble(2),
                        ahorrado = cursor.getDouble(3)
                    )
                )
            }
        }

        return lista
    }

    fun obtenerTotalAhorrado(): Double {
        readableDatabase.rawQuery(
            "SELECT COALESCE(SUM(ahorrado), 0) FROM metas",
            null
        ).use { cursor ->
            return if (cursor.moveToFirst()) {
                cursor.getDouble(0)
            } else {
                0.0
            }
        }
    }

    fun agregarAhorro(
        id: Int,
        cantidad: Double,
        saldoTotal: Double
    ): Boolean {

        if (
            !cantidad.isFinite() ||
            cantidad <= 0.0 ||
            !saldoTotal.isFinite()
        ) return false

        val disponible =
            saldoTotal - obtenerTotalAhorrado()

        if (cantidad > disponible + 0.000001) {
            return false
        }

        val db = writableDatabase

        db.execSQL(
            """
            UPDATE metas
            SET ahorrado = ahorrado + ?
            WHERE id = ?
              AND ahorrado + ? <= objetivo
            """.trimIndent(),
            arrayOf(cantidad, id, cantidad)
        )

        return android.database.DatabaseUtils.longForQuery(
            db,
            "SELECT changes()",
            null
        ) > 0
    }

    fun retirarAhorro(
        id: Int,
        cantidad: Double
    ): Boolean {

        if (
            !cantidad.isFinite() ||
            cantidad <= 0.0
        ) return false

        val db = writableDatabase

        db.execSQL(
            """
            UPDATE metas
            SET ahorrado = ahorrado - ?
            WHERE id = ?
              AND ahorrado >= ?
            """.trimIndent(),
            arrayOf(cantidad, id, cantidad)
        )

        return android.database.DatabaseUtils.longForQuery(
            db,
            "SELECT changes()",
            null
        ) > 0
    }

    fun eliminarMeta(id: Int): Boolean {
        return writableDatabase.delete(
            "metas",
            "id = ?",
            arrayOf(id.toString())
        ) > 0
    }
}
