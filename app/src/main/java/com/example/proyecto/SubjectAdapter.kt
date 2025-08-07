package com.example.proyecto

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class SubjectAdapter(
    private var subjectListFull: MutableList<Subject>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>(), Filterable {

    private var subjectListFiltered = mutableListOf<Subject>().apply {
        addAll(subjectListFull)
    }

    interface OnItemClickListener {
        fun onUpdateClick(subject: Subject)
        fun onDeleteClick(subject: Subject)
    }

    inner class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSubjectName: TextView = itemView.findViewById(R.id.tvSubjectName)
        val tvSubjectCode: TextView = itemView.findViewById(R.id.tvSubjectCode)
        val tvSubjectProfessor: TextView = itemView.findViewById(R.id.tvSubjectProfessor)
        val tvSubjectCourse: TextView = itemView.findViewById(R.id.tvSubjectCourse)
        val tvSubjectSchedule: TextView = itemView.findViewById(R.id.tvSubjectSchedule)
        val iconModality: ImageView = itemView.findViewById(R.id.icon_modality) // Referencia al icono
        val tvSubjectModality: TextView = itemView.findViewById(R.id.tvSubjectModality) // Referencia al texto
        val tvSubjectDuration: TextView = itemView.findViewById(R.id.tvSubjectDuration)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subject, parent, false)
        return SubjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
        val subject = subjectListFiltered[position]

        holder.tvSubjectName.text = subject.nombre
        holder.tvSubjectCode.text = "Código: ${subject.codigo}"
        holder.tvSubjectProfessor.text = subject.profesor
        holder.tvSubjectCourse.text = "Curso: ${subject.curso}"
        holder.tvSubjectSchedule.text = "${subject.dia}, ${subject.hora_inicio} - ${subject.hora_fin}"

        // --- LÓGICA PARA LA MODALIDAD (PRESENCIAL/VIRTUAL) ---
        if (subject.curso.contains("v", ignoreCase = true)) {
            holder.tvSubjectModality.text = "(Virtual)"
            holder.iconModality.setImageResource(R.drawable.laptop)
        } else {
            holder.tvSubjectModality.text = "(Presencial)"
            holder.iconModality.setImageResource(R.drawable.buil)
        }

        // --- LÓGICA PARA CALCULAR LA DURACIÓN ---
        try {
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTime = format.parse(subject.hora_inicio)
            val endTime = format.parse(subject.hora_fin)

            if (startTime != null && endTime != null) {
                val diff = endTime.time - startTime.time
                if (diff >= 0) {
                    val totalMinutes = diff / (60 * 1000)
                    val hours = totalMinutes / 60
                    val minutes = totalMinutes % 60

                    val durationText = when {
                        hours > 0 && minutes > 0 -> "(${hours}h ${minutes}m)"
                        hours > 0 -> "(${hours}h)"
                        minutes > 0 -> "(${minutes}m)"
                        else -> ""
                    }
                    holder.tvSubjectDuration.text = durationText
                    holder.tvSubjectDuration.visibility = View.VISIBLE
                } else {
                    holder.tvSubjectDuration.visibility = View.GONE
                }
            }
        } catch (e: Exception) {
            holder.tvSubjectDuration.visibility = View.GONE
            e.printStackTrace()
        }

        holder.btnDelete.setOnClickListener {
            listener.onDeleteClick(subject)
        }
    }

    override fun getItemCount() = subjectListFiltered.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateFullList(newList: List<Subject>) {
        subjectListFull.clear()
        subjectListFull.addAll(newList)
        filter("")
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint.toString().lowercase(Locale.getDefault()).trim()
                subjectListFiltered = if (charString.isEmpty()) {
                    subjectListFull
                } else {
                    subjectListFull.filter {
                        it.nombre.lowercase(Locale.getDefault()).contains(charString) ||
                                it.codigo.lowercase(Locale.getDefault()).contains(charString) ||
                                it.profesor.lowercase(Locale.getDefault()).contains(charString)
                    }.toMutableList()
                }
                val filterResults = FilterResults()
                filterResults.values = subjectListFiltered
                return filterResults
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                subjectListFiltered = results?.values as? MutableList<Subject> ?: mutableListOf()
                notifyDataSetChanged()
            }
        }
    }

    fun filter(query: String) {
        filter.filter(query)
    }
}