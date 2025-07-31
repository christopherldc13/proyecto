package com.example.proyecto.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.StaticLayout
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.AdminSQLiteOpenHelper
import com.example.proyecto.R
import com.example.proyecto.Student
import com.example.proyecto.StudentAdapter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ViewDataFragment : Fragment(), StudentAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoData: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnSort: ImageButton
    private lateinit var btnExportPdf: ImageButton
    private var studentList = mutableListOf<Student>()
    private lateinit var studentAdapter: StudentAdapter
    private var currentSortOrder = "nombre ASC"

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(requireContext(), "Permiso concedido. Exportando...", Toast.LENGTH_SHORT).show()
                exportToPdf()
            } else {
                Toast.makeText(requireContext(), "Permiso denegado. No se puede guardar el PDF.", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_view_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerView)
        tvNoData = view.findViewById(R.id.tvNoData)
        etSearch = view.findViewById(R.id.etSearch)
        btnSort = view.findViewById(R.id.btnSort)
        btnExportPdf = view.findViewById(R.id.btnExportPdf)

        studentAdapter = StudentAdapter(studentList, this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = studentAdapter

        loadStudentsFromDatabase()

        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                studentAdapter.filter(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnSort.setOnClickListener { showSortOptionsDialog() }

        btnExportPdf.setOnClickListener {
            checkStoragePermission()
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportToPdf()
            return
        }

        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                exportToPdf()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                AlertDialog.Builder(requireContext())
                    .setTitle("Permiso Necesario")
                    .setMessage("Se necesita permiso para guardar el archivo PDF en el almacenamiento de tu dispositivo.")
                    .setPositiveButton("OK") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadStudentsFromDatabase()
        etSearch.setText("")
    }

    private fun showSortOptionsDialog() {
        val sortOptions = arrayOf("Nombre (A-Z)", "Nombre (Z-A)", "ID (Ascendente)", "ID (Descendente)")
        AlertDialog.Builder(requireContext())
            .setTitle("Ordenar por")
            .setItems(sortOptions) { _, which ->
                currentSortOrder = when (which) {
                    0 -> "nombre ASC"
                    1 -> "nombre DESC"
                    2 -> "CAST(id AS INTEGER) ASC"
                    3 -> "CAST(id AS INTEGER) DESC"
                    else -> "nombre ASC"
                }
                loadStudentsFromDatabase()
            }
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadStudentsFromDatabase() {
        val tempStudentList = mutableListOf<Student>()
        val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
        val db = admin.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM estudiantes ORDER BY $currentSortOrder", null)

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
        studentList = tempStudentList

        tvNoData.visibility = if (tempStudentList.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (tempStudentList.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onDeleteClick(student: Student) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar a ${student.nombre} ${student.apellido}?")
            .setPositiveButton("Sí, Eliminar") { _, _ -> deleteStudentFromDatabase(student.id) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteStudentFromDatabase(studentId: String) {
        val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
        val db = admin.writableDatabase
        val result = db.delete("estudiantes", "id=?", arrayOf(studentId))
        db.close()

        if (result > 0) {
            Toast.makeText(requireContext(), "Estudiante eliminado.", Toast.LENGTH_SHORT).show()
            loadStudentsFromDatabase()
        } else {
            Toast.makeText(requireContext(), "Error al eliminar.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportToPdf() {
        if (studentList.isEmpty()) {
            Toast.makeText(requireContext(), "No hay datos para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val marginTop = 40
        val marginLeft = 40
        val marginRight = 40
        val marginBottom = 60
        val contentWidth = pageWidth - marginLeft - marginRight

        val colorPrimary = Color.parseColor("#0D47A1")
        val colorLightGray = Color.parseColor("#F5F5F5")
        val colorHeaderText = Color.WHITE
        val colorBodyText = Color.parseColor("#212121")
        val colorGrid = Color.parseColor("#E0E0E0")

        val titlePaint = Paint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 16f
            color = colorBodyText
            textAlign = Paint.Align.CENTER
        }
        val headerPaint = Paint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textSize = 10f
            color = colorHeaderText
        }
        val bodyPaint = Paint().apply {
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textSize = 10f
            color = colorBodyText
        }
        val footerPaint = Paint().apply {
            typeface = Typeface.SANS_SERIF
            textSize = 8f
            color = Color.GRAY
        }

        val headers = arrayOf("ID", "Nombre", "Apellido", "F. Nacimiento", "Teléfono")
        val columnWidths = floatArrayOf(
            contentWidth * 0.16f,
            contentWidth * 0.23f,
            contentWidth * 0.23f,
            contentWidth * 0.18f,
            contentWidth * 0.20f
        )
        val cellPadding = 12f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        var yPosition = marginTop.toFloat()

        fun drawPageHeader() {
            var currentY = marginTop.toFloat()
            try {
                val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                val logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.logo_ucateci)

                val maxLogoHeight = 60f
                val aspectRatio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
                val logoWidth = maxLogoHeight * aspectRatio

                val logoX = (pageWidth - logoWidth) / 2f

                val srcRect = Rect(0, 0, logoBitmap.width, logoBitmap.height)
                val destRect = RectF(logoX, currentY, logoX + logoWidth, currentY + maxLogoHeight)

                canvas.drawBitmap(logoBitmap, srcRect, destRect, logoPaint)

                currentY += maxLogoHeight + 25

            } catch (_: Exception) {}

            canvas.drawText("Universidad Católica Tecnológica del Cibao", (pageWidth / 2).toFloat(), currentY, titlePaint)
            currentY += 20

            canvas.drawText("Listado de Estudiantes", (pageWidth / 2).toFloat(), currentY, titlePaint.apply {
                textSize = 13f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            })

            yPosition = currentY + 30
        }

        fun drawPageFooter() {
            val lineY = (pageHeight - marginBottom + 10).toFloat()
            canvas.drawLine(marginLeft.toFloat(), lineY, (pageWidth - marginRight).toFloat(), lineY, footerPaint)
            val timestamp = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())
            canvas.drawText(timestamp, marginLeft.toFloat(), (pageHeight - marginBottom + 25).toFloat(), footerPaint)
            val pageText = "Página $pageNumber"
            val textWidth = footerPaint.measureText(pageText)
            canvas.drawText(pageText, (pageWidth - marginRight - textWidth), (pageHeight - marginBottom + 25).toFloat(), footerPaint)
        }

        fun drawTableHeader() {
            var xPosition = marginLeft.toFloat()
            val rowTop = yPosition
            val rowBottom = yPosition + 35
            val backgroundPaint = Paint().apply { color = colorPrimary }
            canvas.drawRect(marginLeft.toFloat(), rowTop, (pageWidth - marginRight).toFloat(), rowBottom, backgroundPaint)

            for (i in headers.indices) {
                val textPaint = TextPaint(headerPaint)
                val staticLayout = StaticLayout.Builder.obtain(headers[i], 0, headers[i].length, textPaint, (columnWidths[i] - 2 * cellPadding).toInt()).build()
                canvas.save()
                canvas.translate(xPosition + cellPadding, rowTop + (rowBottom - rowTop - staticLayout.height) / 2)
                staticLayout.draw(canvas)
                canvas.restore()
                xPosition += columnWidths[i]
            }
            yPosition = rowBottom
        }

        drawPageHeader()
        drawTableHeader()
        var isEvenRow = false

        for (student in studentList) {
            val studentData = listOf(student.id, student.nombre, student.apellido, student.fechaNacimiento, student.telefono)
            var maxRowHeight = 0f

            for (i in studentData.indices) {
                val textPaint = TextPaint(bodyPaint)
                val staticLayout = StaticLayout.Builder.obtain(studentData[i], 0, studentData[i].length, textPaint, (columnWidths[i] - 2 * cellPadding).toInt()).build()
                if (staticLayout.height > maxRowHeight) maxRowHeight = staticLayout.height.toFloat()
            }
            val rowHeight = maxRowHeight + cellPadding * 1.5f

            if (yPosition + rowHeight > pageHeight - marginBottom) {
                drawPageFooter()
                pdfDocument.finishPage(currentPage)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                drawPageHeader()
                drawTableHeader()
            }

            val rowTop = yPosition
            val rowBottom = yPosition + rowHeight

            if (isEvenRow) {
                val backgroundPaint = Paint().apply { color = colorLightGray }
                canvas.drawRect(marginLeft.toFloat(), rowTop, (pageWidth - marginRight).toFloat(), rowBottom, backgroundPaint)
            }
            isEvenRow = !isEvenRow

            var xPosition = marginLeft.toFloat()
            for (i in studentData.indices) {
                val textPaint = TextPaint(bodyPaint)
                val staticLayout = StaticLayout.Builder.obtain(studentData[i], 0, studentData[i].length, textPaint, (columnWidths[i] - 2 * cellPadding).toInt()).build()
                canvas.save()
                canvas.translate(xPosition + cellPadding, rowTop + (rowHeight - staticLayout.height) / 2)
                staticLayout.draw(canvas)
                canvas.restore()
                xPosition += columnWidths[i]
            }
            canvas.drawLine(marginLeft.toFloat(), rowBottom, (pageWidth - marginRight).toFloat(), rowBottom, Paint().apply { color = colorGrid })
            yPosition = rowBottom
        }

        drawPageFooter()
        pdfDocument.finishPage(currentPage)

        // --- LÓGICA DE GUARDADO DEFINITIVA ---
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Listado_Estudiantes_${timestamp}.pdf"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri).use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                } else {
                    throw IOException("No se pudo crear el archivo en MediaStore.")
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
            }
            Toast.makeText(requireContext(), "PDF guardado en la carpeta de Descargas.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error al guardar PDF: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        } finally {
            pdfDocument.close()
        }
    }
}