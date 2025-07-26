package com.example.proyecto

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class StudentAdapter(
    private var studentListFiltered: MutableList<Student>,
    // Añadimos el listener para los clics en los botones
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    private var studentListFull: List<Student> = ArrayList(studentListFiltered)

    // Interfaz para comunicar los clics al Fragment
    interface OnItemClickListener {
        fun onDeleteClick(student: Student)
        // Aquí podrías añadir onUpdateClick en el futuro
    }

    // ViewHolder actualizado para el nuevo diseño
    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val studentId: TextView = itemView.findViewById(R.id.tvStudentId)
        // Referencias a los nuevos TextViews
        val studentBirthday: TextView = itemView.findViewById(R.id.tvStudentBirthday)
        val studentGender: TextView = itemView.findViewById(R.id.tvStudentGender)
        val studentPhone: TextView = itemView.findViewById(R.id.tvStudentPhone)
        // Referencia al botón
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val currentStudent = studentListFiltered[position]
        holder.studentName.text = "${currentStudent.nombre.trim()} ${currentStudent.apellido.trim()}"
        holder.studentId.text = "ID: ${currentStudent.id}"

        // Asignar cada dato a su nuevo TextView
        holder.studentBirthday.text = currentStudent.fechaNacimiento
        holder.studentGender.text = currentStudent.sexo
        holder.studentPhone.text = currentStudent.telefono

        // Asignar el clic del botón al listener
        holder.btnDelete.setOnClickListener {
            listener.onDeleteClick(currentStudent)
        }
    }

    override fun getItemCount() = studentListFiltered.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateFullList(newList: List<Student>) {
        studentListFull = ArrayList(newList)
        studentListFiltered.clear()
        studentListFiltered.addAll(studentListFull)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun filter(text: String) {
        val searchText = text.lowercase(Locale.getDefault())
        studentListFiltered.clear()
        if (searchText.isEmpty()) {
            studentListFiltered.addAll(studentListFull)
        } else {
            for (student in studentListFull) {
                if (student.nombre.lowercase(Locale.getDefault()).contains(searchText) ||
                    student.apellido.lowercase(Locale.getDefault()).contains(searchText) ||
                    student.id.contains(searchText)
                ) {
                    studentListFiltered.add(student)
                }
            }
        }
        notifyDataSetChanged()
    }
}