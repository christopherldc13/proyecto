package com.example.proyecto.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.example.proyecto.R

class AboutFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)

        // Sitio Web
        val btnWebsite = view.findViewById<ImageButton>(R.id.btnWebsite)
        btnWebsite.setOnClickListener {
            val url = "https://www.ucateci.edu.do/"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // Instagram
        val btnInstagram = view.findViewById<ImageButton>(R.id.btnInstagram)
        btnInstagram.setOnClickListener {
            val url = "https://www.instagram.com/ucatecioficial"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // Facebook
        val btnFacebook = view.findViewById<ImageButton>(R.id.btnFacebook)
        btnFacebook.setOnClickListener {
            val url = "https://www.facebook.com/UCATECI/about"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        return view
    }
}
