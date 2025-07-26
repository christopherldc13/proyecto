package com.example.proyecto.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.proyecto.R

class HomeFragment : Fragment() {

    private lateinit var themeButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        themeButton = view.findViewById(R.id.btnToggleTheme)

        // Establecer el ícono correcto al iniciar
        updateButtonIcon()

        themeButton.setOnClickListener {
            toggleTheme()
        }
    }

    private fun isNightMode(): Boolean {
        val sharedPreferences = requireActivity().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("isNightMode", false)
    }

    private fun updateButtonIcon() {
        if (isNightMode()) {
            themeButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.sun))
        } else {
            themeButton.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.moon))
        }
    }

    private fun toggleTheme() {
        val sharedPreferences = requireActivity().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        if (isNightMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            editor.putBoolean("isNightMode", false)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            editor.putBoolean("isNightMode", true)
        }
        editor.apply()
    }
}