package com.example.myapplication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

/**
 * Screen to display all bookmarks
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksScreen(viewModel: BookmarkViewModel) {
    val bookmarks by viewModel.bookmarks
    val isError by viewModel.isError
    val errorMessage by viewModel.errorMessage
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bookmarks") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    IconButton(onClick = {
                        if (!isError) {
                            showAddDialog = true
                        } else {
                            Toast.makeText(context, errorMessage ?: "Firebase error", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Bookmark"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isError) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Bookmark")
                }
            }
        }
    ) { paddingValues ->
        // Show error state if there's an error
        if (isError) {
            FirebaseErrorState(
                errorMessage = errorMessage ?: "Unknown error",
                onRetry = { viewModel.loadBookmarks() },
                modifier = Modifier.padding(paddingValues)
            )
        }
        // Show empty state if no bookmarks
        else if (bookmarks.isEmpty()) {
            EmptyBookmarksState(
                onAddClick = { showAddDialog = true },
                modifier = Modifier.padding(paddingValues)
            )
        }
        // Show bookmarks list
        else {
            BookmarksList(
                bookmarks = bookmarks,
                onDeleteBookmark = { viewModel.deleteBookmark(it) },
                modifier = Modifier.padding(paddingValues)
            )
        }

        // Add Bookmark Dialog
        if (showAddDialog) {
            AddBookmarkDialog(
                onDismiss = { showAddDialog = false },
                onAddBookmark = { text ->
                    viewModel.addBookmark(text)
                    showAddDialog = false
                }
            )
        }
    }
}

/**
 * Firebase Error State
 */
@Composable
fun FirebaseErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = "Firebase Error",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Retry")
            }
        }
    }
}

/**
 * Add Bookmark Dialog
 */
@Composable
fun AddBookmarkDialog(
    onDismiss: () -> Unit,
    onAddBookmark: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Bookmark") },
        text = {
            Column {
                Text(
                    "Enter text or URL to bookmark",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Text or URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (text.isBlank()) {
                        Toast.makeText(context, "Please enter text to bookmark", Toast.LENGTH_SHORT).show()
                    } else {
                        onAddBookmark(text)
                        Toast.makeText(context, "Bookmark added!", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Empty state when no bookmarks are available
 */
@Composable
fun EmptyBookmarksState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "No bookmarks yet",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                "Click the + button to add a bookmark",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Add Bookmark")
            }
        }
    }
}

/**
 * List of bookmarks
 */
@Composable
fun BookmarksList(
    bookmarks: List<Bookmark>,
    onDeleteBookmark: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxSize()
    ) {
        items(
            count = bookmarks.size,
            key = { index -> bookmarks[index].id }
        ) { index ->
            val bookmark = bookmarks[index]
            BookmarkItem(
                bookmark = bookmark,
                onDeleteClick = { onDeleteBookmark(bookmark.id) }
            )
        }
    }
}

/**
 * Individual bookmark item
 */
@Composable
fun BookmarkItem(
    bookmark: Bookmark,
    onDeleteClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon based on whether it's a URL or text
                Icon(
                    if (bookmark.isUrl) Icons.Default.Link else Icons.Default.TextSnippet,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Bookmark text and date
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = bookmark.text,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "Added on ${dateFormat.format(bookmark.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Action buttons
                Row {
                    // Open URL button (if it's a URL)
                    if (bookmark.isUrl) {
                        IconButton(
                            onClick = {
                                try {
                                    var url = bookmark.text
                                    if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                        url = "https://$url"
                                    }
                                    uriHandler.openUri(url)
                                } catch (e: Exception) {
                                    // Handle error opening URL
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = "Open URL",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Delete button
                    IconButton(onClick = onDeleteClick) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Bookmark",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}