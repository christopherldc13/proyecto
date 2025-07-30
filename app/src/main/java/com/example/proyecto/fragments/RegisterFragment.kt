package com.example.proyecto.fragments

import android.app.DatePickerDialog
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.proyecto.AdminSQLiteOpenHelper
import com.example.proyecto.R
import java.util.*

class RegisterFragment : Fragment() {

    // Vistas de la UI
    private lateinit var etId: EditText
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var rgSexo: RadioGroup
    private lateinit var rbMasculino: RadioButton
    private lateinit var rbFemenino: RadioButton
    private lateinit var etTelefono: EditText

    // Botones para controlar su estado
    private lateinit var btnRegistrar: Button
    private lateinit var btnActualizar: Button
    private lateinit var btnBuscar: Button
    private lateinit var btnLimpiar: Button // <-- LÍNEA AÑADIDA

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa las vistas y botones
        etId = view.findViewById(R.id.etId)
        etNombre = view.findViewById(R.id.etNombre)
        etApellido = view.findViewById(R.id.etApellido)
        etFechaNacimiento = view.findViewById(R.id.etFechaNacimiento)
        rgSexo = view.findViewById(R.id.rgSexo)
        rbMasculino = view.findViewById(R.id.rbMasculino)
        rbFemenino = view.findViewById(R.id.rbFemenino)
        etTelefono = view.findViewById(R.id.etTelefono)
        btnRegistrar = view.findViewById(R.id.btnRegistrar)
        btnBuscar = view.findViewById(R.id.btnBuscar)
        btnActualizar = view.findViewById(R.id.btnActualizar)
        btnLimpiar = view.findViewById(R.id.btnLimpiar) // <-- LÍNEA AÑADIDA

        // Configura los listeners
        btnRegistrar.setOnClickListener { registrar() }
        btnBuscar.setOnClickListener { buscar() }
        btnActualizar.setOnClickListener { actualizar() }
        btnLimpiar.setOnClickListener { limpiarCampos() } // <-- LÍNEA AÑADIDA
        etFechaNacimiento.setOnClickListener { mostrarDatePicker() }

        // Configura TextWatchers
        etNombre.addTextChangedListener(CapitalizeTextWatcher(etNombre))
        etApellido.addTextChangedListener(CapitalizeTextWatcher(etApellido))
        etTelefono.addTextChangedListener(PhoneTextWatcher(etTelefono))
    }

    override fun onResume() {
        super.onResume()
        limpiarCampos()
    }

    private fun actualizarEstadoBotones(busquedaExitosa: Boolean) {
        btnRegistrar.isEnabled = !busquedaExitosa
        btnActualizar.isEnabled = busquedaExitosa
        etId.isEnabled = !busquedaExitosa
    }

    private fun limpiarCampos() {
        etId.setText("")
        etNombre.setText("")
        etApellido.setText("")
        etFechaNacimiento.setText("")
        rgSexo.clearCheck()
        etTelefono.setText("")
        etId.requestFocus()
        actualizarEstadoBotones(busquedaExitosa = false)
    }

    private fun registrar() {
        val id = etId.text.toString().trim()
        val nombre = etNombre.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val telefono = etTelefono.text.toString()
        val fechaNacimiento = etFechaNacimiento.text.toString()
        val sexoId = rgSexo.checkedRadioButtonId

        if (id.isNotEmpty() && nombre.isNotEmpty() && apellido.isNotEmpty() && fechaNacimiento.isNotEmpty() && sexoId != -1 && telefono.length == 14) {
            val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
            val baseDeDatos = admin.writableDatabase

            // Llama a la validación completa para el registro
            val errorMessage = checkExistingRecordForRegister(baseDeDatos, id, nombre, apellido, telefono)
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                baseDeDatos.close()
                return
            }

            val sexo = view?.findViewById<RadioButton>(sexoId)?.text.toString()
            val registro = ContentValues().apply {
                put("id", id)
                put("nombre", nombre)
                put("apellido", apellido)
                put("fecha_nacimiento", fechaNacimiento)
                put("sexo", sexo)
                put("telefono", telefono)
            }
            baseDeDatos.insert("estudiantes", null, registro)
            baseDeDatos.close()
            limpiarCampos()
            Toast.makeText(requireContext(), "Estudiante registrado exitosamente.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Debes llenar todos los campos correctamente.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizar() {
        val id = etId.text.toString().trim()
        val nombre = etNombre.text.toString().trim()
        val apellido = etApellido.text.toString().trim()
        val telefono = etTelefono.text.toString()
        val fechaNacimiento = etFechaNacimiento.text.toString()
        val sexoId = rgSexo.checkedRadioButtonId

        if (id.isNotEmpty() && nombre.isNotEmpty() && apellido.isNotEmpty() && fechaNacimiento.isNotEmpty() && sexoId != -1 && telefono.length == 14) {
            val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
            val baseDeDatos = admin.writableDatabase

            // Llama a la validación completa para la actualización
            val errorMessage = checkExistingRecordForUpdate(baseDeDatos, id, nombre, apellido, telefono)
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                baseDeDatos.close()
                return
            }

            val sexo = view?.findViewById<RadioButton>(sexoId)?.text.toString()
            val registro = ContentValues().apply {
                put("nombre", nombre)
                put("apellido", apellido)
                put("fecha_nacimiento", fechaNacimiento)
                put("sexo", sexo)
                put("telefono", telefono)
            }
            val cantidad = baseDeDatos.update("estudiantes", registro, "id=?", arrayOf(id))
            baseDeDatos.close()

            if (cantidad > 0) {
                Toast.makeText(requireContext(), "Datos actualizados exitosamente.", Toast.LENGTH_SHORT).show()
                limpiarCampos()
            } else {
                Toast.makeText(requireContext(), "Error: No se encontró el estudiante para actualizar.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Todos los campos deben estar llenos para actualizar.", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Valida para REGISTRAR.
     * Verifica si el ID, el nombre completo O el teléfono ya existen.
     */
    private fun checkExistingRecordForRegister(db: SQLiteDatabase, id: String, nombre: String, apellido: String, telefono: String): String? {
        db.query("estudiantes", arrayOf("id"), "id = ?", arrayOf(id), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) return "El ID ya se encuentra registrado."
        }
        db.query("estudiantes", arrayOf("id"), "LOWER(nombre) = ? AND LOWER(apellido) = ?", arrayOf(nombre.lowercase(), apellido.lowercase()), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) return "El nombre y apellido ya se encuentran registrados."
        }
        db.query("estudiantes", arrayOf("id"), "telefono = ?", arrayOf(telefono), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) return "El número de teléfono ya ha sido registrado."
        }
        return null
    }

    /**
     * Valida para ACTUALIZAR.
     * Verifica si el nombre completo O el teléfono ya existen en OTRO registro.
     */
    private fun checkExistingRecordForUpdate(db: SQLiteDatabase, idToExclude: String, nombre: String, apellido: String, telefono: String): String? {
        db.query("estudiantes", arrayOf("id"), "LOWER(nombre) = ? AND LOWER(apellido) = ? AND id != ?", arrayOf(nombre.lowercase(), apellido.lowercase(), idToExclude), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) return "El nombre y apellido ya están registrados por otro estudiante."
        }
        db.query("estudiantes", arrayOf("id"), "telefono = ? AND id != ?", arrayOf(telefono, idToExclude), null, null, null).use { cursor ->
            if (cursor.moveToFirst()) return "El número de teléfono ya está registrado por otro estudiante."
        }
        return null
    }

    private fun buscar() {
        val id = etId.text.toString().trim()
        if (id.isNotEmpty()) {
            val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
            val baseDeDatos = admin.readableDatabase
            val cursor = baseDeDatos.rawQuery("SELECT nombre, apellido, fecha_nacimiento, sexo, telefono FROM estudiantes WHERE id = ?", arrayOf(id))
            if (cursor.moveToFirst()) {
                etNombre.setText(cursor.getString(0))
                etApellido.setText(cursor.getString(1))
                etFechaNacimiento.setText(cursor.getString(2))
                if (cursor.getString(3) == "Masculino") rbMasculino.isChecked = true else rbFemenino.isChecked = true
                etTelefono.setText(cursor.getString(4))
                Toast.makeText(requireContext(), "Estudiante encontrado. Puede actualizar los datos.", Toast.LENGTH_SHORT).show()
                actualizarEstadoBotones(busquedaExitosa = true)
            } else {
                Toast.makeText(requireContext(), "No se encontró el estudiante.", Toast.LENGTH_SHORT).show()
                limpiarCampos()
            }
            cursor.close()
            baseDeDatos.close()
        } else {
            Toast.makeText(requireContext(), "Debes ingresar un ID para buscar.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarDatePicker() {
        val calendario = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val fechaSeleccionada = "$dayOfMonth/${month + 1}/$year"
                etFechaNacimiento.setText(fechaSeleccionada)
            },
            calendario.get(Calendar.YEAR),
            calendario.get(Calendar.MONTH),
            calendario.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private class CapitalizeTextWatcher(private val editText: EditText) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val original = s.toString()
            if (original.isEmpty()) return

            editText.removeTextChangedListener(this)
            val capitalized = original.split(" ").joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            if (original != capitalized) {
                editText.setText(capitalized)
                editText.setSelection(capitalized.length)
            }
            editText.addTextChangedListener(this)
        }
    }

    private class PhoneTextWatcher(private val editText: EditText) : TextWatcher {
        private var isFormatting = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (isFormatting) return
            isFormatting = true

            val digits = s.toString().filter { it.isDigit() }
            val formatted = StringBuilder()

            try {
                if (digits.length >= 3) {
                    formatted.append("(${digits.substring(0, 3)}) ")
                    if (digits.length >= 6) {
                        formatted.append(digits.substring(3, 6))
                        if (digits.length > 6) {
                            formatted.append("-")
                            formatted.append(digits.substring(6, minOf(10, digits.length)))
                        }
                    } else if (digits.length > 3) {
                        formatted.append(digits.substring(3))
                    }
                } else {
                    formatted.append(digits)
                }
                editText.setText(formatted.toString())
                editText.setSelection(editText.text.length)
            } catch (e: Exception) {
                s?.clear()
            }
            isFormatting = false
        }
    }
}