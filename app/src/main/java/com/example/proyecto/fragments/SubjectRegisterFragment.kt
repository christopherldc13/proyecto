package com.example.proyecto.fragments

import android.app.TimePickerDialog
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.text.Editable
import android.text.Html
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.proyecto.AdminSQLiteOpenHelper
import com.example.proyecto.R
import java.util.*

class SubjectRegisterFragment : Fragment() {

    private lateinit var admin: AdminSQLiteOpenHelper
    private lateinit var db: SQLiteDatabase

    private lateinit var etCodigo: EditText
    private lateinit var etNombre: EditText
    private lateinit var etProfesor: EditText
    private lateinit var etCurso: EditText
    private lateinit var etDia: AutoCompleteTextView
    private lateinit var etHoraInicio: EditText
    private lateinit var etHoraFin: EditText

    private lateinit var btnRegistrar: Button
    private lateinit var btnBuscar: Button
    private lateinit var btnActualizar: Button
    private lateinit var btnLimpiar: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_subject_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvGoToSubjectList: TextView = view.findViewById(R.id.tvGoToSubjectList)
        val textoHtml = "¿Deseas ver el Listado de Asignaturas? <b><u>PULSE AQUÍ</u></b>"
        tvGoToSubjectList.text = Html.fromHtml(textoHtml, Html.FROM_HTML_MODE_LEGACY)
        tvGoToSubjectList.setOnClickListener {
            findNavController().navigate(R.id.action_subjectRegisterFragment_to_subjectListFragment)
        }

        admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
        db = admin.writableDatabase

        etCodigo = view.findViewById(R.id.etAsignaturaCodigo)
        etNombre = view.findViewById(R.id.etAsignaturaNombre)
        etProfesor = view.findViewById(R.id.etAsignaturaProfesor)
        etCurso = view.findViewById(R.id.etAsignaturaCurso)
        etDia = view.findViewById(R.id.etAsignaturaDia)
        etHoraInicio = view.findViewById(R.id.etHoraInicio)
        etHoraFin = view.findViewById(R.id.etHoraFin)

        btnRegistrar = view.findViewById(R.id.btnRegistrar)
        btnBuscar = view.findViewById(R.id.btnBuscar)
        btnActualizar = view.findViewById(R.id.btnActualizar)
        btnLimpiar = view.findViewById(R.id.btnLimpiar)

        val btnBack: ImageButton = view.findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        val dias = resources.getStringArray(R.array.dias_de_la_semana)
        val adapter = ArrayAdapter(requireContext(), R.layout.list_item_dropdown, dias)
        etDia.setAdapter(adapter)

        etHoraInicio.setOnClickListener { showTimePicker(it as EditText) }
        etHoraFin.setOnClickListener { showTimePicker(it as EditText) }

        btnRegistrar.setOnClickListener { registrar() }
        btnBuscar.setOnClickListener { buscar() }
        btnActualizar.setOnClickListener { actualizar() }
        btnLimpiar.setOnClickListener { limpiarCampos() }

        // --- LÓGICA PARA EL CÓDIGO DE ASIGNATURA ---
        etCodigo.addTextChangedListener(SubjectCodeFormattingWatcher(etCodigo))
        etCodigo.filters = arrayOf(InputFilter.LengthFilter(11))


        // Filtro para el nombre de la asignatura
        val subjectNameFilter = InputFilter { source, _, _, _, _, _ ->
            source.filter { it.isLetterOrDigit() || it.isWhitespace() }
        }
        etNombre.filters = arrayOf(subjectNameFilter)

        // Filtro para el nombre del profesor
        val professorNameFilter = InputFilter { source, _, _, _, _, _ ->
            source.filter { it.isLetter() || it.isWhitespace() }
        }
        etProfesor.filters = arrayOf(professorNameFilter)

        etNombre.addTextChangedListener(CapitalizeTextWatcher(etNombre))
        etProfesor.addTextChangedListener(CapitalizeTextWatcher(etProfesor))

        setFormState(isUpdateMode = false)
    }

    override fun onResume() {
        super.onResume()
        limpiarCampos()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        db.close()
    }

    // --- WATCHER CON MAYÚSCULAS Y GUION ---
    private class SubjectCodeFormattingWatcher(private val editText: EditText) : TextWatcher {
        private var currentText = ""
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (s.toString() != currentText) {
                editText.removeTextChangedListener(this)

                val userInput = s.toString().replace("-", "")

                val formattedText = if (userInput.length >= 3) {
                    // Poner los 3 primeros en mayúscula, un guion, y el resto
                    userInput.substring(0, 3).uppercase() + "-" + userInput.substring(3)
                } else {
                    // Si tiene menos de 3, solo poner en mayúscula
                    userInput.uppercase()
                }

                currentText = formattedText
                editText.setText(formattedText)
                editText.setSelection(formattedText.length)

                editText.addTextChangedListener(this)
            }
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    }


    private class CapitalizeTextWatcher(private val editText: EditText) : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            val original = s.toString()
            if (original.isEmpty()) return

            editText.removeTextChangedListener(this)

            val capitalized = original.split(" ").joinToString(" ") { word ->
                if (word.isNotEmpty()) {
                    word.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }
                } else {
                    ""
                }
            }
            if (original != capitalized) {
                editText.setText(capitalized)
                editText.setSelection(capitalized.length)
            }

            editText.addTextChangedListener(this)
        }
    }

    private fun setFormState(isUpdateMode: Boolean) {
        btnRegistrar.isEnabled = !isUpdateMode
        btnActualizar.isEnabled = isUpdateMode
        etCodigo.isEnabled = !isUpdateMode
    }

    private fun showTimePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(requireContext(), { _, hour, minute ->
            editText.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
    }

    fun registrar() {
        val codigo = etCodigo.text.toString().trim()
        val nombre = etNombre.text.toString().trim()
        val profesor = etProfesor.text.toString().trim()
        val curso = etCurso.text.toString().trim()
        val dia = etDia.text.toString().trim()
        val horaInicio = etHoraInicio.text.toString()
        val horaFin = etHoraFin.text.toString()

        if (codigo.isNotEmpty() && nombre.isNotEmpty() && profesor.isNotEmpty() && curso.isNotEmpty() &&
            dia.isNotEmpty() && horaInicio.isNotEmpty() && horaFin.isNotEmpty()) {

            val errorMessage = checkExistingSubjectForRegister(codigo, nombre)
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                return
            }

            val values = ContentValues().apply {
                put("codigo", codigo)
                put("nombre", nombre)
                put("profesor", profesor)
                put("curso", curso)
                put("dia", dia)
                put("hora_inicio", horaInicio)
                put("hora_fin", horaFin)
            }

            db.insert("asignaturas", null, values)
            Toast.makeText(requireContext(), "Asignatura registrada exitosamente", Toast.LENGTH_SHORT).show()
            limpiarCampos()
        } else {
            Toast.makeText(requireContext(), "Por favor, llene todos los campos", Toast.LENGTH_SHORT).show()
        }
    }

    fun buscar() {
        val codigo = etCodigo.text.toString().trim()
        if (codigo.isNotEmpty()) {
            val cursor = db.rawQuery("SELECT nombre, profesor, curso, dia, hora_inicio, hora_fin FROM asignaturas WHERE codigo = ?", arrayOf(codigo))
            if (cursor.moveToFirst()) {
                etNombre.setText(cursor.getString(0))
                etProfesor.setText(cursor.getString(1))
                etCurso.setText(cursor.getString(2))
                etDia.setText(cursor.getString(3), false)
                etHoraInicio.setText(cursor.getString(4))
                etHoraFin.setText(cursor.getString(5))
                Toast.makeText(requireContext(), "Asignatura encontrada. Puede actualizar.", Toast.LENGTH_SHORT).show()
                setFormState(isUpdateMode = true)
            } else {
                Toast.makeText(requireContext(), "No se encontró la asignatura.", Toast.LENGTH_SHORT).show()
                limpiarCampos()
            }
            cursor.close()
        } else {
            Toast.makeText(requireContext(), "Ingrese un código para buscar.", Toast.LENGTH_SHORT).show()
        }
    }

    fun actualizar() {
        val codigo = etCodigo.text.toString().trim()
        val nombre = etNombre.text.toString().trim()
        val profesor = etProfesor.text.toString().trim()
        val curso = etCurso.text.toString().trim()
        val dia = etDia.text.toString().trim()
        val horaInicio = etHoraInicio.text.toString()
        val horaFin = etHoraFin.text.toString()

        if (codigo.isNotEmpty() && nombre.isNotEmpty() && profesor.isNotEmpty() && curso.isNotEmpty() &&
            dia.isNotEmpty() && horaInicio.isNotEmpty() && horaFin.isNotEmpty()) {

            val errorMessage = checkExistingSubjectForUpdate(codigo, nombre)
            if (errorMessage != null) {
                Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                return
            }

            val values = ContentValues().apply {
                put("nombre", nombre)
                put("profesor", profesor)
                put("curso", curso)
                put("dia", dia)
                put("hora_inicio", horaInicio)
                put("hora_fin", horaFin)
            }

            val cantidad = db.update("asignaturas", values, "codigo=?", arrayOf(codigo))
            if (cantidad > 0) {
                Toast.makeText(requireContext(), "Asignatura actualizada exitosamente.", Toast.LENGTH_SHORT).show()
                limpiarCampos()
            } else {
                Toast.makeText(requireContext(), "No se encontró la asignatura para actualizar.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Por favor, llene todos los campos.", Toast.LENGTH_SHORT).show()
        }
    }

    fun limpiarCampos() {
        etCodigo.setText("")
        etNombre.setText("")
        etProfesor.setText("")
        etCurso.setText("")
        etDia.setText("", false)
        etHoraInicio.setText("")
        etHoraFin.setText("")
        etCodigo.requestFocus()
        setFormState(isUpdateMode = false)
    }

    private fun checkExistingSubjectForRegister(codigo: String, nombre: String): String? {
        db.query("asignaturas", arrayOf("codigo"), "codigo = ?", arrayOf(codigo), null, null, null).use {
            if (it.moveToFirst()) return "El código de la asignatura ya existe."
        }
        db.query("asignaturas", arrayOf("codigo"), "LOWER(nombre) = ?", arrayOf(nombre.lowercase()), null, null, null).use {
            if (it.moveToFirst()) return "El nombre de la asignatura ya existe."
        }
        return null
    }

    private fun checkExistingSubjectForUpdate(codigoToExclude: String, nombre: String): String? {
        db.query("asignaturas", arrayOf("codigo"), "LOWER(nombre) = ? AND codigo != ?", arrayOf(nombre.lowercase(), codigoToExclude), null, null, null).use {
            if (it.moveToFirst()) return "El nombre de la asignatura ya pertenece a otra materia."
        }
        return null
    }
}