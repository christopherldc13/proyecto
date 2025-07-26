package com.example.proyecto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StudentAdapter(private val studentList: List<Student>) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val studentId: TextView = itemView.findViewById(R.id.tvStudentId)
        val studentDetails: TextView = itemView.findViewById(R.id.tvStudentDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_student, parent, false)
        return StudentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val currentStudent = studentList[position]
        holder.studentName.text = "${currentStudent.nombre} ${currentStudent.apellido}"
        holder.studentId.text = "ID: ${currentStudent.id}"
        holder.studentDetails.text = "Nac: ${currentStudent.fechaNacimiento} | Sexo: ${currentStudent.sexo} | Tel: ${currentStudent.telefono}"
    }

    override fun getItemCount() = studentList.size
}