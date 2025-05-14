package com.example.myapplication

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Button
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class StocksActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_stocks)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

       // var viewButton: Button  = findViewById(R.id.button)
        var spinner:Spinner = findViewById(R.id.spinner)
        var webview:WebView = findViewById(R.id.webview)
        webview.loadUrl("https://www.google.com/finance/quote/${spinner.selectedItem}:NSE")
        webview.settings.javaScriptEnabled = true

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                webview.loadUrl("https://www.google.com/finance/quote/${spinner.selectedItem}:NSE")
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Required to implement but can be left empty
            }
        }

//        viewButton.setOnClickListener {
//            webview.loadUrl("https://www.google.com/finance/quote/${spinner.selectedItem}:NSE")
//        }

    }
}