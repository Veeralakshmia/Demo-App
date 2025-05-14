package com.example.myapplication

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*

/**
 * ViewModel for managing bookmarks using Firebase Realtime Database
 */
class BookmarkViewModel(context: Context) {
    private val TAG = "BookmarkViewModel"
    private val DATABASE_URL = "https://myapplication-4bca4-default-rtdb.firebaseio.com/"

    // Status flag for UI
    private val _isError = mutableStateOf(false)
    val isError: State<Boolean> = _isError

    private val _errorMessage = mutableStateOf<String?>(null)
    val errorMessage: State<String?> = _errorMessage

    // Firebase Realtime Database reference
    private val dbRef: DatabaseReference? = try {
        // Ensure Firebase is initialized
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
            Log.d(TAG, "Initialized Firebase in ViewModel")
        }

        // Get database reference
        val database = FirebaseDatabase.getInstance(DATABASE_URL)
        database.getReference("bookmarks")

    } catch (e: Exception) {
        Log.e(TAG, "Error initializing database", e)
        _isError.value = true
        _errorMessage.value = e.message
        Toast.makeText(context, "Firebase error: ${e.message}", Toast.LENGTH_SHORT).show()
        null
    }

    private val _bookmarks = mutableStateOf<List<Bookmark>>(emptyList())
    val bookmarks: State<List<Bookmark>> = _bookmarks

    init {
        // Log the database connection status
        Log.d(TAG, "BookmarkViewModel initialized")
        Log.d(TAG, "Database reference: ${dbRef?.toString() ?: "null"}")

        // Attempt to load bookmarks
        loadBookmarks()
    }

    /**
     * Load all bookmarks from Firebase Realtime Database
     */
    fun loadBookmarks() {
        Log.d(TAG, "loadBookmarks() called")

        val dbRefCopy = dbRef
        if (dbRefCopy == null) {
            Log.e(TAG, "Database reference is null")
            _isError.value = true
            _errorMessage.value = "Database reference is null"
            return
        }

        dbRefCopy.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "onDataChange() received ${snapshot.childrenCount} children")

                val bookmarksList = mutableListOf<Bookmark>()

                for (bookmarkSnapshot in snapshot.children) {
                    try {
                        val id = bookmarkSnapshot.key ?: continue

                        // Attempt to get values with detailed logging
                        val text = try {
                            bookmarkSnapshot.child("text").getValue(String::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting text for bookmark $id", e)
                            null
                        } ?: continue

                        val timestamp = try {
                            bookmarkSnapshot.child("timestamp").getValue(Long::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting timestamp for bookmark $id", e)
                            null
                        } ?: System.currentTimeMillis()

                        val isUrl = try {
                            bookmarkSnapshot.child("isUrl").getValue(Boolean::class.java)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting isUrl for bookmark $id", e)
                            null
                        } ?: false

                        // Create bookmark object and add to list
                        val bookmark = Bookmark(
                            id = id,
                            text = text,
                            timestamp = Date(timestamp),
                            isUrl = isUrl
                        )

                        Log.d(TAG, "Added bookmark: $id, text: $text")
                        bookmarksList.add(bookmark)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error processing bookmark snapshot", e)
                    }
                }

                // Sort by most recent first
                bookmarksList.sortByDescending { it.timestamp }
                _bookmarks.value = bookmarksList

                Log.d(TAG, "Loaded ${bookmarksList.size} bookmarks")

                // If we got here, clear any previous error state
                _isError.value = false
                _errorMessage.value = null
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Error loading bookmarks: ${error.message}", error.toException())
                _isError.value = true
                _errorMessage.value = error.message
            }
        })
    }

    /**
     * Add a new bookmark
     *
     * @param text The text or URL to bookmark
     */
    fun addBookmark(text: String) {
        Log.d(TAG, "addBookmark() called with text: $text")

        val dbRefCopy = dbRef
        if (dbRefCopy == null) {
            Log.e(TAG, "Database reference is null")
            _isError.value = true
            _errorMessage.value = "Database reference is null"
            return
        }

        if (text.isBlank()) {
            Log.w(TAG, "Attempted to add empty bookmark")
            return
        }

        // Determine if the text is likely a URL
        val isUrl = text.startsWith("http://") || text.startsWith("https://") ||
                text.startsWith("www.") || text.contains(".com") ||
                text.contains(".org") || text.contains(".net")

        // Create bookmark data
        val bookmarkData = HashMap<String, Any>()
        bookmarkData["text"] = text
        bookmarkData["timestamp"] = System.currentTimeMillis()
        bookmarkData["isUrl"] = isUrl

        Log.d(TAG, "Created bookmark data: $bookmarkData")

        // Push to get a new unique key
        val newBookmarkRef = dbRefCopy.push()
        newBookmarkRef.setValue(bookmarkData)
            .addOnSuccessListener {
                Log.d(TAG, "Bookmark added successfully with ID: ${newBookmarkRef.key}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding bookmark", e)
                _isError.value = true
                _errorMessage.value = "Error adding bookmark: ${e.message}"
            }
    }

    /**
     * Delete a bookmark
     *
     * @param id The ID of the bookmark to delete
     */
    fun deleteBookmark(id: String) {
        Log.d(TAG, "deleteBookmark() called with id: $id")

        val dbRefCopy = dbRef
        if (dbRefCopy == null) {
            Log.e(TAG, "Database reference is null")
            _isError.value = true
            _errorMessage.value = "Database reference is null"
            return
        }

        dbRefCopy.child(id).removeValue()
            .addOnSuccessListener {
                Log.d(TAG, "Bookmark deleted successfully: $id")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting bookmark", e)
                _isError.value = true
                _errorMessage.value = "Error deleting bookmark: ${e.message}"
            }
    }

    companion object {
        // Create a dummy ViewModel when Firebase initialization fails
        fun createDummy(context: Context): BookmarkViewModel {
            return BookmarkViewModel(context).apply {
                _isError.value = true
                _errorMessage.value = "Firebase is not initialized. Bookmarks feature unavailable."
            }
        }
    }
}

/**
 * Data class representing a bookmark
 */
data class Bookmark(
    val id: String,
    val text: String,
    val timestamp: Date,
    val isUrl: Boolean
)