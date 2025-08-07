package com.example.proyecto.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextWatcher
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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyecto.AdminSQLiteOpenHelper
import com.example.proyecto.R
import com.example.proyecto.Subject
import com.example.proyecto.SubjectAdapter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubjectListFragment : Fragment(), SubjectAdapter.OnItemClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoData: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnSort: ImageButton
    private lateinit var btnExportPdf: ImageButton
    private lateinit var btnBack: ImageButton
    private var subjectList = mutableListOf<Subject>()
    private lateinit var subjectAdapter: SubjectAdapter
    private var currentSortOrder = "nombre ASC"

    // Launcher for requesting storage permission
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
        return inflater.inflate(R.layout.fragment_subject_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerViewSubjects)
        tvNoData = view.findViewById(R.id.tvNoData)
        etSearch = view.findViewById(R.id.etSearch)
        btnSort = view.findViewById(R.id.btnSort)
        btnExportPdf = view.findViewById(R.id.btnExportPdf)
        btnBack = view.findViewById(R.id.btnBack)

        subjectAdapter = SubjectAdapter(subjectList, this)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = subjectAdapter

        loadSubjectsFromDatabase()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                subjectAdapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSort.setOnClickListener { showSortOptionsDialog() }
        btnExportPdf.setOnClickListener { checkStoragePermission() }

        // Action for the back button
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    override fun onResume() {
        super.onResume()
        loadSubjectsFromDatabase()
        etSearch.setText("")
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

    private fun showSortOptionsDialog() {
        val sortOptions = arrayOf("Nombre (A-Z)", "Nombre (Z-A)", "Código (Ascendente)", "Código (Descendente)")
        AlertDialog.Builder(requireContext())
            .setTitle("Ordenar por")
            .setItems(sortOptions) { _, which ->
                currentSortOrder = when (which) {
                    0 -> "nombre ASC"
                    1 -> "nombre DESC"
                    2 -> "codigo ASC"
                    3 -> "codigo DESC"
                    else -> "nombre ASC"
                }
                loadSubjectsFromDatabase()
            }
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadSubjectsFromDatabase() {
        val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
        val db = admin.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM asignaturas ORDER BY $currentSortOrder", null)

        val tempList = mutableListOf<Subject>()
        if (cursor.moveToFirst()) {
            do {
                tempList.add(Subject(
                    codigo = cursor.getString(cursor.getColumnIndexOrThrow("codigo")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    profesor = cursor.getString(cursor.getColumnIndexOrThrow("profesor")),
                    curso = cursor.getString(cursor.getColumnIndexOrThrow("curso")),
                    dia = cursor.getString(cursor.getColumnIndexOrThrow("dia")),
                    hora_inicio = cursor.getString(cursor.getColumnIndexOrThrow("hora_inicio")),
                    hora_fin = cursor.getString(cursor.getColumnIndexOrThrow("hora_fin"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        subjectAdapter.updateFullList(tempList)
        tvNoData.visibility = if (tempList.isEmpty()) View.VISIBLE else View.GONE
        recyclerView.visibility = if (tempList.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onUpdateClick(subject: Subject) {
        Toast.makeText(requireContext(), "Actualizar ${subject.nombre}", Toast.LENGTH_SHORT).show()
    }

    override fun onDeleteClick(subject: Subject) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar la asignatura ${subject.nombre}?")
            .setPositiveButton("Sí, Eliminar") { _, _ -> deleteSubjectFromDatabase(subject.codigo) }
            .setNegativeButton("No", null)
            .show()
    }

    private fun deleteSubjectFromDatabase(subjectCode: String) {
        val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
        val db = admin.writableDatabase
        val result = db.delete("asignaturas", "codigo=?", arrayOf(subjectCode))
        db.close()
        if (result > 0) {
            Toast.makeText(requireContext(), "Asignatura eliminada.", Toast.LENGTH_SHORT).show()
            loadSubjectsFromDatabase()
        } else {
            Toast.makeText(requireContext(), "Error al eliminar.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportToPdf() {
        val fullSubjectList = getFullSubjectList()
        if (fullSubjectList.isEmpty()) {
            Toast.makeText(requireContext(), "No hay asignaturas para exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()

        // Page properties
        val pageWidth = 595
        val pageHeight = 842
        val marginTop = 40
        val marginLeft = 40
        val marginRight = 40
        val marginBottom = 60
        val contentWidth = pageWidth - marginLeft - marginRight

        // Colors and Paints
        val colorPrimary = Color.parseColor("#0D47A1") // Dark Blue
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

        // Table column definitions
        val headers = arrayOf("Código", "Nombre", "Profesor", "Curso", "Día", "Horario")
        val columnWidths = floatArrayOf(
            contentWidth * 0.12f, // Código
            contentWidth * 0.27f, // Nombre
            contentWidth * 0.24f, // Profesor
            contentWidth * 0.12f, // Curso (más espacio)
            contentWidth * 0.13f, // Día (más espacio)
            contentWidth * 0.12f  // Horario (más espacio)
        )
        val cellPadding = 12f

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = currentPage.canvas
        var yPosition = marginTop.toFloat()

        fun drawPageHeader() {
            var currentY = marginTop.toFloat()
            try {
                // Draw logo
                val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                val logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.logo_ucateci)
                val maxLogoHeight = 60f
                val aspectRatio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
                val logoWidth = maxLogoHeight * aspectRatio
                val logoX = (pageWidth - logoWidth) / 2f
                val srcRect = android.graphics.Rect(0, 0, logoBitmap.width, logoBitmap.height)
                val destRect = android.graphics.RectF(logoX, currentY, logoX + logoWidth, currentY + maxLogoHeight)
                canvas.drawBitmap(logoBitmap, srcRect, destRect, logoPaint)
                currentY += maxLogoHeight + 25
            } catch (e: Exception) {
                currentY += 85 // Reserve space even if logo fails
            }

            // Draw Titles
            canvas.drawText("Universidad Católica Tecnológica del Cibao", (pageWidth / 2).toFloat(), currentY, titlePaint)
            currentY += 20
            canvas.drawText("Listado de Asignaturas", (pageWidth / 2).toFloat(), currentY, titlePaint.apply {
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

        for (subject in fullSubjectList) {
            val horario = "${subject.hora_inicio} - ${subject.hora_fin}"
            val subjectData = listOf(subject.codigo, subject.nombre, subject.profesor, subject.curso, subject.dia, horario)
            var maxRowHeight = 0f

            for (i in subjectData.indices) {
                val textPaint = TextPaint(bodyPaint)
                val staticLayout = StaticLayout.Builder.obtain(subjectData[i], 0, subjectData[i].length, textPaint, (columnWidths[i] - 2 * cellPadding).toInt()).build()
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
                isEvenRow = false
            }

            val rowTop = yPosition
            val rowBottom = yPosition + rowHeight

            if (isEvenRow) {
                val backgroundPaint = Paint().apply { color = colorLightGray }
                canvas.drawRect(marginLeft.toFloat(), rowTop, (pageWidth - marginRight).toFloat(), rowBottom, backgroundPaint)
            }
            isEvenRow = !isEvenRow

            var xPosition = marginLeft.toFloat()
            for (i in subjectData.indices) {
                val textPaint = TextPaint(bodyPaint)
                val staticLayout = StaticLayout.Builder.obtain(subjectData[i], 0, subjectData[i].length, textPaint, (columnWidths[i] - 2 * cellPadding).toInt()).build()
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

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Listado_Asignaturas_${timestamp}.pdf"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw IOException("No se pudo crear el archivo en MediaStore.")
                resolver.openOutputStream(uri).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
            } else {
                @Suppress("DEPRECATION")
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

    private fun getFullSubjectList(): List<Subject> {
        val list = mutableListOf<Subject>()
        val admin = AdminSQLiteOpenHelper(requireContext(), "administracion", null, 1)
        val db = admin.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM asignaturas ORDER BY $currentSortOrder", null)
        if (cursor.moveToFirst()) {
            do {
                list.add(Subject(
                    codigo = cursor.getString(cursor.getColumnIndexOrThrow("codigo")),
                    nombre = cursor.getString(cursor.getColumnIndexOrThrow("nombre")),
                    profesor = cursor.getString(cursor.getColumnIndexOrThrow("profesor")),
                    curso = cursor.getString(cursor.getColumnIndexOrThrow("curso")),
                    dia = cursor.getString(cursor.getColumnIndexOrThrow("dia")),
                    hora_inicio = cursor.getString(cursor.getColumnIndexOrThrow("hora_inicio")),
                    hora_fin = cursor.getString(cursor.getColumnIndexOrThrow("hora_fin"))
                ))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
}