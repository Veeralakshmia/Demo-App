package com.example.myapplication

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        db = ContactDatabase.getInstance(this)
        initializeFirebase(this)
    }

    companion object {
         lateinit var db: ContactDatabase;

        private const val TAG = "MyApplication"
        private const val DATABASE_URL = "https://myapplication-4bca4-default-rtdb.firebaseio.com/"

        // Flag to check if Firebase is initialized
        private var isFirebaseInitialized = false

        // Public method to get Firebase initialization status
        fun isFirebaseInitialized(): Boolean {
            return isFirebaseInitialized
        }

        // Public method to initialize Firebase
        fun initializeFirebase(context: android.content.Context): Boolean {
            if (isFirebaseInitialized) {
                return true // Already initialized
            }

            try {
                // Initialize Firebase if not already initialized
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                    Log.d(TAG, "Firebase initialized successfully")
                } else {
                    Log.d(TAG, "Firebase was already initialized")
                }

                // Set persistence enabled for offline capabilities
                try {
                    FirebaseDatabase.getInstance(DATABASE_URL).setPersistenceEnabled(true)
                    Log.d(TAG, "Firebase Database persistence enabled")
                } catch (e: Exception) {
                    Log.e(TAG, "Error enabling persistence, but continuing", e)
                }

                isFirebaseInitialized = true
                return true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Firebase", e)
                return false
            }
        }
    }
}