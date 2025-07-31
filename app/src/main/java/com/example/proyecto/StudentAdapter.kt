package com.example.proyecto

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class StudentAdapter(
    private var studentListFiltered: MutableList<Student>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    private var studentListFull: List<Student> = ArrayList(studentListFiltered)

    interface OnItemClickListener {
        fun onDeleteClick(student: Student)
        // Puedes añadir onUpdateClick aquí si lo necesitas en el futuro
    }

    class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val studentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val studentId: TextView = itemView.findViewById(R.id.tvStudentId)
        val studentBirthday: TextView = itemView.findViewById(R.id.tvStudentBirthday)
        val studentGender: TextView = itemView.findViewById(R.id.tvStudentGender)
        val studentPhone: TextView = itemView.findViewById(R.id.tvStudentPhone)
        val studentAge: TextView = itemView.findViewById(R.id.tvStudentAge)
        val studentEmail: TextView = itemView.findViewById(R.id.tvStudentEmail) // <-- AÑADIDO
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
        holder.studentBirthday.text = currentStudent.fechaNacimiento
        holder.studentGender.text = currentStudent.sexo
        holder.studentPhone.text = currentStudent.telefono

        // Mostrar edad detallada
        val edadTexto = calcularEdadDetallada(currentStudent.fechaNacimiento)
        holder.studentAge.text = "($edadTexto)"

        // --- CÓDIGO AÑADIDO PARA EL CORREO ---
        val email = "${currentStudent.id.replace("-", "")}@miucateci.edu.do"
        holder.studentEmail.text = email
        // --- FIN DEL CÓDIGO AÑADIDO ---

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

    // ✅ FUNCIÓN ACTUALIZADA: Edad en años, meses y días sin mostrar ceros
    private fun calcularEdadDetallada(fechaNacimiento: String): String {
        return try {
            val formato = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val nacimiento = Calendar.getInstance().apply {
                time = formato.parse(fechaNacimiento)!!
            }
            val hoy = Calendar.getInstance()

            var años = hoy.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)
            var meses = hoy.get(Calendar.MONTH) - nacimiento.get(Calendar.MONTH)
            var dias = hoy.get(Calendar.DAY_OF_MONTH) - nacimiento.get(Calendar.DAY_OF_MONTH)

            if (dias < 0) {
                meses--
                val mesAnterior = hoy.clone() as Calendar
                mesAnterior.add(Calendar.MONTH, -1)
                dias += mesAnterior.getActualMaximum(Calendar.DAY_OF_MONTH)
            }

            if (meses < 0) {
                años--
                meses += 12
            }

            val partes = mutableListOf<String>()
            if (años > 0) partes.add("$años ${if (años == 1) "año" else "años"}")
            if (meses > 0) partes.add("$meses ${if (meses == 1) "mes" else "meses"}")
            if (dias > 0 || partes.isEmpty()) partes.add("$dias ${if (dias == 1) "día" else "días"}")

            when (partes.size) {
                3 -> "${partes[0]}, ${partes[1]} y ${partes[2]}"
                2 -> "${partes[0]} y ${partes[1]}"
                1 -> partes[0]
                else -> "Edad desconocida"
            }
        } catch (e: Exception) {
            "Edad desconocida"
        }
    }
}