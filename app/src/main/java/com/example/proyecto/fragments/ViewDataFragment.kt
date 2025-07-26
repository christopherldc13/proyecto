package com.example.proyecto.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.AdminSQLiteOpenHelper
import com.example.proyecto.R
import com.example.proyecto.Student
import com.example.proyecto.StudentAdapter

class ViewDataFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoData: TextView
    private var studentList = mutableListOf<Student>()
    private lateinit var studentAdapter: StudentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_view_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerView)
        tvNoData = view.findViewById(R.id.tvNoData)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        studentAdapter = StudentAdapter(studentList)
        recyclerView.adapter = studentAdapter

        loadStudentsFromDatabase()
    }

    // Se recomienda actualizar la lista cada vez que el fragmento se vuelve visible
    override fun onResume() {
        super.onResume()
        loadStudentsFromDatabase()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadStudentsFromDatabase() {
        studentList.clear()
        val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
        val db = admin.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM estudiantes", null)

        if (cursor.moveToFirst()) {
            do {
                val student = Student(
                    id = cursor.getString(cursor.getColumnIndexOrThrow("id")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    apellido = cursor.getString(cursor.getColumnIndexOrThrow("apellido")),
                    fechaNacimiento = cursor.getString(cursor.getColumnIndexOrThrow("fecha_nacimiento")),
                    sexo = cursor.getString(cursor.getColumnIndexOrThrow("sexo")),
                    telefono = cursor.getString(cursor.getColumnIndexOrThrow("telefono"))
                )
                studentList.add(student)
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        if (studentList.isEmpty()) {
            tvNoData.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvNoData.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            studentAdapter.notifyDataSetChanged()
        }
    }
}