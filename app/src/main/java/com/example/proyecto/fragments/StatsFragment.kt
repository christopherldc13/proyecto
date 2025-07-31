package com.example.proyecto.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.proyecto.AdminSQLiteOpenHelper
import com.example.proyecto.R
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment() {

    private lateinit var tvTotalCount: TextView
    private lateinit var tvMaleCount: TextView
    private lateinit var tvFemaleCount: TextView
    private lateinit var tvAverageAge: TextView
    private lateinit var tvAgeRangePercentage: TextView
    // NUEVO: TextViews para el desglose de rangos
    private lateinit var tvAgeRange1Count: TextView
    private lateinit var tvAgeRange2Count: TextView
    private lateinit var tvAgeRange3Count: TextView
    private lateinit var tvAgeRange4Count: TextView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvTotalCount = view.findViewById(R.id.tvTotalCount)
        tvMaleCount = view.findViewById(R.id.tvMaleCount)
        tvFemaleCount = view.findViewById(R.id.tvFemaleCount)
        tvAverageAge = view.findViewById(R.id.tvAverageAge)
        tvAgeRangePercentage = view.findViewById(R.id.tvAgeRangePercentage)
        // NUEVO: Inicialización de los nuevos TextViews
        tvAgeRange1Count = view.findViewById(R.id.tvAgeRange1Count)
        tvAgeRange2Count = view.findViewById(R.id.tvAgeRange2Count)
        tvAgeRange3Count = view.findViewById(R.id.tvAgeRange3Count)
        tvAgeRange4Count = view.findViewById(R.id.tvAgeRange4Count)
    }

    override fun onResume() {
        super.onResume()
        loadStatistics()
    }

    @SuppressLint("SetTextI18n")
    private fun loadStatistics() {
        val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
        val db = admin.readableDatabase

        var totalCount = 0
        var maleCount = 0
        var femaleCount = 0
        var totalAge = 0
        var mainAgeRangeCount = 0 // Para el % de 17-22
        // NUEVO: Contadores para el desglose
        var range1Count = 0 // 17-21
        var range2Count = 0 // 22-25
        var range3Count = 0 // 26-30
        var range4Count = 0 // 31+

        val cursor = db.rawQuery("SELECT fecha_nacimiento, sexo FROM estudiantes", null)

        if (cursor.moveToFirst()) {
            do {
                totalCount++
                val fechaNacimiento = cursor.getString(0)
                val sexo = cursor.getString(1)

                if (sexo.equals("Masculino", ignoreCase = true)) maleCount++ else femaleCount++

                val age = calculateAge(fechaNacimiento)
                if (age != -1) {
                    totalAge += age
                    // Para el %
                    if (age in 17..22) mainAgeRangeCount++

                    // NUEVO: Clasificación para el desglose
                    when (age) {
                        in 17..21 -> range1Count++
                        in 22..25 -> range2Count++
                        in 26..30 -> range3Count++
                        else -> if (age >= 31) range4Count++
                    }
                }

            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        tvTotalCount.text = totalCount.toString()
        tvMaleCount.text = maleCount.toString()
        tvFemaleCount.text = femaleCount.toString()

        // NUEVO: Asignar los valores del desglose
        tvAgeRange1Count.text = range1Count.toString()
        tvAgeRange2Count.text = range2Count.toString()
        tvAgeRange3Count.text = range3Count.toString()
        tvAgeRange4Count.text = range4Count.toString()

        if (totalCount > 0) {
            val averageAge = totalAge.toDouble() / totalCount
            tvAverageAge.text = String.format(Locale.US, "%.1f", averageAge)

            val percentage = (mainAgeRangeCount.toDouble() * 100) / totalCount.toDouble()
            tvAgeRangePercentage.text = "El ${String.format("%.1f", percentage)}% de los estudiantes tiene entre 17 y 22 años"
        } else {
            tvAverageAge.text = "0"
            tvAgeRangePercentage.text = "No hay datos para calcular rangos de edad."
        }
    }

    private fun calculateAge(birthDateString: String?): Int {
        if (birthDateString.isNullOrEmpty()) return -1
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val birthDate = sdf.parse(birthDateString)

            val dob = Calendar.getInstance()
            dob.time = birthDate
            val today = Calendar.getInstance()

            var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            age
        } catch (e: Exception) {
            -1
        }
    }
}