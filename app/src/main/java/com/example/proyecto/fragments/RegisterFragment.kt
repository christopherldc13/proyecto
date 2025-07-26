package com.example.proyecto.fragments

import android.app.DatePickerDialog
import android.content.ContentValues
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
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

    private lateinit var etId: EditText
    private lateinit var etNombre: EditText
    private lateinit var etApellido: EditText
    private lateinit var etFechaNacimiento: EditText
    private lateinit var rgSexo: RadioGroup
    private lateinit var rbMasculino: RadioButton
    private lateinit var rbFemenino: RadioButton
    private lateinit var etTelefono: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa las vistas
        etId = view.findViewById(R.id.etId)
        etNombre = view.findViewById(R.id.etNombre)
        etApellido = view.findViewById(R.id.etApellido)
        etFechaNacimiento = view.findViewById(R.id.etFechaNacimiento)
        rgSexo = view.findViewById(R.id.rgSexo)
        rbMasculino = view.findViewById(R.id.rbMasculino)
        rbFemenino = view.findViewById(R.id.rbFemenino)
        etTelefono = view.findViewById(R.id.etTelefono)

        // Configura los listeners de los botones
        view.findViewById<Button>(R.id.btnRegistrar).setOnClickListener { registrar() }
        view.findViewById<Button>(R.id.btnBuscar).setOnClickListener { buscar() }
        view.findViewById<Button>(R.id.btnActualizar).setOnClickListener { actualizar() }
        //view.findViewById<Button>(R.id.btnEliminar).setOnClickListener { eliminar() }

        etFechaNacimiento.setOnClickListener { mostrarDatePicker() }

        // Auto-capitalización para Nombre y Apellido
        etNombre.addTextChangedListener(CapitalizeTextWatcher(etNombre))
        etApellido.addTextChangedListener(CapitalizeTextWatcher(etApellido))

        // Formato para número de teléfono (XXX) XXX-XXXX
        etTelefono.addTextChangedListener(PhoneTextWatcher(etTelefono))
    }

    // Clase interna para capitalizar la primera letra de cada palabra
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

    // Clase interna para el formato del teléfono
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

    private fun limpiarCampos() {
        etId.setText("")
        etNombre.setText("")
        etApellido.setText("")
        etFechaNacimiento.setText("")
        rgSexo.clearCheck()
        etTelefono.setText("")
        etId.requestFocus()
    }

    private fun registrar() {
        val id = etId.text.toString()
        val nombre = etNombre.text.toString()
        val apellido = etApellido.text.toString()
        val fechaNacimiento = etFechaNacimiento.text.toString()
        val sexoId = rgSexo.checkedRadioButtonId
        val telefono = etTelefono.text.toString()

        if (id.isNotEmpty() && nombre.isNotEmpty() && apellido.isNotEmpty() && fechaNacimiento.isNotEmpty() && sexoId != -1 && telefono.isNotEmpty()) {
            val sexo = view?.findViewById<RadioButton>(sexoId)?.text.toString()
            val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
            val baseDeDatos = admin.writableDatabase
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
            Toast.makeText(requireContext(), "Debes llenar todos los campos.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buscar() {
        val id = etId.text.toString()
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
                Toast.makeText(requireContext(), "Estudiante encontrado.", Toast.LENGTH_SHORT).show()
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

    private fun actualizar() {
        val id = etId.text.toString()
        val nombre = etNombre.text.toString()
        val apellido = etApellido.text.toString()
        val fechaNacimiento = etFechaNacimiento.text.toString()
        val sexoId = rgSexo.checkedRadioButtonId
        val telefono = etTelefono.text.toString()

        if (id.isNotEmpty() && nombre.isNotEmpty() && apellido.isNotEmpty() && fechaNacimiento.isNotEmpty() && sexoId != -1 && telefono.isNotEmpty()) {
            val sexo = view?.findViewById<RadioButton>(sexoId)?.text.toString()
            val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
            val baseDeDatos = admin.writableDatabase
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
            } else {
                Toast.makeText(requireContext(), "No se encontró el estudiante para actualizar.", Toast.LENGTH_SHORT).show()
            }
            limpiarCampos()
        } else {
            Toast.makeText(requireContext(), "Debes llenar todos los campos.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun eliminar() {
        val id = etId.text.toString()
        if (id.isNotEmpty()) {
            val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
            val baseDeDatos = admin.writableDatabase
            val cantidad = baseDeDatos.delete("estudiantes", "id=?", arrayOf(id))
            baseDeDatos.close()
            limpiarCampos()
            if (cantidad > 0) {
                Toast.makeText(requireContext(), "Estudiante eliminado exitosamente.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "No se encontró el estudiante para eliminar.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Debes ingresar un ID para eliminar.", Toast.LENGTH_SHORT).show()
        }
    }
}