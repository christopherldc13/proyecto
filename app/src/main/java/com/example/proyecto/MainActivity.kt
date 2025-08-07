package com.example.proyecto

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        applySavedTheme()
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        val navView: BottomNavigationView = findViewById(R.id.bottom_nav_view)

        // Reemplazamos setupWithNavController con un listener personalizado
        navView.setOnItemSelectedListener { item ->
            val builder = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setEnterAnim(androidx.navigation.ui.R.anim.nav_default_enter_anim)
                .setExitAnim(androidx.navigation.ui.R.anim.nav_default_exit_anim)
                .setPopEnterAnim(androidx.navigation.ui.R.anim.nav_default_pop_enter_anim)
                .setPopExitAnim(androidx.navigation.ui.R.anim.nav_default_pop_exit_anim)

            // Si el destino es el fragmento de registro, lo reiniciamos
            if (item.itemId == R.id.registerFragment) {
                // Esta línea borra el historial de la pestaña "Registrar"
                builder.setPopUpTo(R.id.registerFragment, true)
            }

            val options = builder.build()

            try {
                navController.navigate(item.itemId, null, options)
                true
            } catch (e: Exception) {
                // Evita un crash si el item no es un destino de navegación
                false
            }
        }

        // Sincronizar el item seleccionado cuando se navega con el botón "atrás"
        navController.addOnDestinationChangedListener { _, destination, _ ->
            navView.menu.findItem(destination.id)?.isChecked = true
        }
    }

    private fun applySavedTheme() {
        val sharedPreferences = getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val isNightMode = sharedPreferences.getBoolean("isNightMode", false)
        if (isNightMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}