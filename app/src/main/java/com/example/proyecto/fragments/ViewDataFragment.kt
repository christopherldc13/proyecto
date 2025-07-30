package com.example.proyecto.fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.AdminSQLiteOpenHelper
import com.example.proyecto.R
import com.example.proyecto.Student
import com.example.proyecto.StudentAdapter

// Implementa la interfaz del adaptador
class ViewDataFragment : Fragment(), StudentAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoData: TextView
    private lateinit var etSearch: EditText
    private var studentList = mutableListOf<Student>()
    private lateinit var studentAdapter: StudentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_view_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerView)
        tvNoData = view.findViewById(R.id.tvNoData)
        etSearch = view.findViewById(R.id.etSearch)

        // Pasa 'this' como listener al adaptador
        studentAdapter = StudentAdapter(studentList, this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = studentAdapter

        loadStudentsFromDatabase()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                studentAdapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        loadStudentsFromDatabase()
        etSearch.setText("")
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadStudentsFromDatabase() {
        val tempStudentList = mutableListOf<Student>()
        val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
        val db = admin.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM estudiantes ORDER BY Nombre DESC", null)

        if (cursor.moveToFirst()) {
            do {
                tempStudentList.add(
                    Student(
                        id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                        nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                        apellido = cursor.getString(cursor.getColumnIndexOrThrow("apellido")),
                        fechaNacimiento = cursor.getString(cursor.getColumnIndexOrThrow("fecha_nacimiento")),
                        sexo = cursor.getString(cursor.getColumnIndexOrThrow("sexo")),
                        telefono = cursor.getString(cursor.getColumnIndexOrThrow("telefono"))
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        studentAdapter.updateFullList(tempStudentList)

        if (tempStudentList.isEmpty()) {
            tvNoData.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvNoData.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    // Esta función se llamará cuando se presione el botón "Eliminar"
    override fun onDeleteClick(student: Student) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar a ${student.nombre} ${student.apellido}?")
            .setPositiveButton("Sí, Eliminar") { _, _ ->
                deleteStudentFromDatabase(student.id)
            }
            .setNegativeButton("No", null)
            .show()
    }

    // Función para borrar el estudiante de la base de datos
    private fun deleteStudentFromDatabase(studentId: String) {
        val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
        val db = admin.writableDatabase
        val result = db.delete("estudiantes", "id=?", arrayOf(studentId))
        db.close()

        if (result > 0) {
            Toast.makeText(requireContext(), "Estudiante eliminado.", Toast.LENGTH_SHORT).show()
            loadStudentsFromDatabase() // Recargar la lista para reflejar el cambio
        } else {
            Toast.makeText(requireContext(), "Error al eliminar.", Toast.LENGTH_SHORT).show()
        }
    }
}