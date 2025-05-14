package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging


// Screen routes
sealed class Screen(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    object Profile : Screen("profile", Icons.Default.Person, "Profile")
    object Bookmarks : Screen("bookmarks", Icons.Default.Bookmark, "Bookmarks")
    object Stocks : Screen("stocks", Icons.Default.AddChart, "Stocks")
}

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    private val startForResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Handle the result data
            val data = result.data
            val returnedValue = data?.getStringExtra("returnValue") ?: "No data returned"
            // Update your UI with the returned value
            updateResultText(returnedValue)
        }
    }

    fun launchSkillsActivity(inputText: String) {
        val intent = Intent(this, SkillActivity::class.java).apply {
            putExtra("textValue", inputText)
        }
        // Launch the activity expecting a result
        startForResult.launch(intent)
    }

    private var resultText by mutableStateOf("No result yet")

    private fun updateResultText(value: String) {
        resultText = value
        Toast.makeText(this, resultText, Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Ensure Firebase is initialized before setting content
        val isInitialized = MyApplication.initializeFirebase(this)
        if (!isInitialized) {
            Toast.makeText(this, "Failed to initialize Firebase. Some features may not work.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Firebase initialization failed in MainActivity")
        }
        checkNotificationPermission()
        getFCMToken()


        setContent {
            MyApplicationTheme {
                MainScreen(
                    onLaunchSkillsClick = { inputText ->
                        launchSkillsActivity(inputText)
                    },
                    {
                        var intent = Intent(this, MainActivity::class.java)
                        val pendingIntent = PendingIntent.getActivity(
                            this, 0, intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )

                        // Build notification
                        val builder =
                            buildNotification("New Message", "Button triggered notification")
                                .setContentIntent(pendingIntent)

                        // Show notification
                        with(NotificationManagerCompat.from(this)) {
                            if (checkNotificationPermission()) {
                                createNotificationChannel()
                                notify(1, builder.build())
                            }
                        }
                    }
                )
            }
        }
    }
    var PERMISSION_REQUEST_CODE = 100
    private fun checkNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }

    private fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get the FCM token
            val token = task.result

            // Log and display the token
            val msg = "Token: $token"
            Log.d(TAG, msg)
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

            // Here you would normally send this token to your server
        }
    }

    private fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                val msg = if (task.isSuccessful) {
                    "Subscribed to topic: $topic"
                } else {
                    "Failed to subscribe to topic: $topic"
                }
                Log.d(TAG, msg)
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
    }

    fun createNotificationChannel() {
        // Only required for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notification"
            val descriptionText = "Notification Desc"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel("CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, "CHANNEL_ID")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
    }

}

@Composable
fun MainScreen(onLaunchSkillsClick: (String) -> Unit, showNotification: (String) -> Unit) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val items = listOf(Screen.Profile, Screen.Bookmarks, Screen.Stocks)

    // Make sure Firebase is initialized before creating the ViewModel
    val isFirebaseInitialized = remember {
        MyApplication.isFirebaseInitialized() || MyApplication.initializeFirebase(context)
    }

    // Create BookmarkViewModel only if Firebase is initialized
    val bookmarkViewModel = remember {
        if (isFirebaseInitialized) {
            BookmarkViewModel(context)
        } else {
            // Create a dummy ViewModel that shows an error message
            BookmarkViewModel.createDummy(context)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Profile.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Profile.route) {
                ProfileScreenContent(
                    name = "John Doe",
                    address = "123 Main Street, Madurai, Tamil Nadu, India",
                    onLaunchSkillsClick = onLaunchSkillsClick,
                    showNotification = showNotification
                )
            }
            composable(Screen.Bookmarks.route) {
                if (isFirebaseInitialized) {
                    BookmarksScreen(viewModel = bookmarkViewModel)
                } else {
                    // Show error message if Firebase is not initialized
                    FirebaseErrorScreen(
                        onRetry = {
                            MyApplication.initializeFirebase(context)
                        }
                    )
                }
            }
            composable(Screen.Stocks.route) {
                context.startActivity(Intent(context, StocksActivity::class.java))
            }
        }
    }
}

@Composable
fun FirebaseErrorScreen(onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Firebase Connection Error",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Could not connect to the database. Bookmarks feature is unavailable.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun ProfileScreenContent(
    name: String = "Your Name",
    address: String = "Your Address",
    onLaunchSkillsClick: (String) -> Unit,
    showNotification: (String) -> Unit
) {
    val localContext = LocalContext.current
    var textValue by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Profile Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Name
                    Text(
                        text = "Name",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Address
                    Text(
                        text = "Address",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = address,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Input field (for text/link)
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = { Text("Enter text") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Type something...") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Skills button
                    Button(
                        onClick = { onLaunchSkillsClick(textValue) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Skills")
                    }

                    // Share button
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, textValue)
                                    setPackage("com.whatsapp")
                                }
                                localContext.startActivity(Intent.createChooser(intent, "Share to"))
                            } catch (e: Exception) {
                                Toast.makeText(
                                    localContext,
                                    "Unable to share. Please make sure WhatsApp is installed.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share")
                    }

                    // Weather button
                    Button(
                        onClick = {
                            localContext.startActivity(Intent(localContext, WeatherActivity::class.java))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Weather")
                    }

                    // Contact button
                    Button(
                        onClick = {
                            localContext.startActivity(Intent(localContext, ContactActivity::class.java))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Contact")
                    }


                    Button(
                        onClick = {
                            showNotification("")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Show Notification")
                    }
                }
            }
        }
    }


}