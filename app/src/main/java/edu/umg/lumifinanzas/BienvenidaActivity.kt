package edu.umg.lumifinanzas

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class BienvenidaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mostrar siempre la pantalla de bienvenida
        setContentView(R.layout.activity_bienvenida)

        // Obtener los elementos de la pantalla
        val encabezado = findViewById<View>(
            R.id.encabezadoBienvenida
        )

        val logo = findViewById<View>(
            R.id.logoCentralBienvenida
        )

        val titulo = findViewById<View>(
            R.id.tituloBienvenida
        )

        val subtitulo = findViewById<View>(
            R.id.subtituloBienvenida
        )

        val tarjetas = findViewById<View>(
            R.id.tarjetasBienvenida
        )

        val boton = findViewById<Button>(
            R.id.btnComenzarBienvenida
        )

        // ANIMACIÓN DEL ENCABEZADO
        encabezado.alpha = 0f

        encabezado.animate()
            .alpha(1f)
            .setDuration(650)
            .start()

        // ANIMACIÓN DEL LOGO CENTRAL
        logo.alpha = 0f
        logo.scaleX = 0.5f
        logo.scaleY = 0.5f

        logo.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(200)
            .setDuration(900)
            .start()

        // ANIMACIÓN DEL TÍTULO
        titulo.alpha = 0f
        titulo.translationY = 40f

        titulo.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(450)
            .setDuration(700)
            .start()

        // ANIMACIÓN DEL SUBTÍTULO
        subtitulo.alpha = 0f

        subtitulo.animate()
            .alpha(1f)
            .setStartDelay(700)
            .setDuration(700)
            .start()

        // ANIMACIÓN DE LAS TARJETAS FINANCIERAS
        tarjetas.alpha = 0f
        tarjetas.translationY = 50f

        tarjetas.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(900)
            .setDuration(750)
            .start()

        // ANIMACIÓN DEL BOTÓN COMENZAR
        boton.alpha = 0f
        boton.translationY = 35f

        boton.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(1100)
            .setDuration(750)
            .start()

        // BOTÓN PARA ENTRAR A LUMIFINANZAS
        boton.setOnClickListener {
            abrirInicio()
        }
    }

    // ABRIR LA PANTALLA PRINCIPAL
    private fun abrirInicio() {

        val intent = Intent(
            this,
            MainActivity::class.java
        )

        startActivity(intent)

        // Cerrar la bienvenida para no regresar
        // a ella al presionar el botón Atrás
        finish()
    }
}
