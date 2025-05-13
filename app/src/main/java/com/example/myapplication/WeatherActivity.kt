package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.google.gson.annotations.SerializedName
import kotlin.math.roundToInt

class WeatherActivity : ComponentActivity() {
    private lateinit var sharedPreferences : SharedPreferences;
    private val LAST_CITY = "last_city"
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            sharedPreferences = this.getSharedPreferences("MyPref", Context.MODE_PRIVATE)
            setContent {
                MyApplicationTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        WeatherScreen()
                    }
                }
            }
        }

    fun saveLastCity(city:String) {
        sharedPreferences.edit().putString(LAST_CITY,city).apply()
    }

    fun getLastCity():String {
        return sharedPreferences.getString(LAST_CITY, null)?:"London"
    }
}

    // Single data class for weather information
    data class WeatherData(
        val city: String = "",
        val temperature: Double = 0.0,
        val description: String = "",
        val humidity: Int = 0,
        val windSpeed: Double = 0.0,
        val isLoading: Boolean = false,
        val error: String? = null
    )

@Composable
fun WeatherScreen() {
    val viewModel: WeatherViewModel = viewModel()
    val weatherData by viewModel.weatherData.collectAsState()
    var cityInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    var activity: WeatherActivity = context as WeatherActivity;

    LaunchedEffect(true) {
        cityInput = activity.getLastCity()
        viewModel.fetchWeather(cityInput)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Search section
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = cityInput,
                onValueChange = { cityInput = it },
                label = { Text("Enter city") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = {
                if (cityInput.isNotEmpty()) {
                    viewModel.fetchWeather(cityInput)
                    activity.saveLastCity(cityInput)
                }
            }) {
                Text("Search")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Weather display
        when {
            weatherData.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(50.dp)
                )
            }
            weatherData.error != null -> {
                Text(
                    text = weatherData.error!!
                )
            }
            else -> {
                // Weather card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // City name
                        Text(
                            text = weatherData.city,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Weather icon (simplified)
//                        Icon(
//                            imageVector = when {
//                                weatherData.description.contains("clear", ignoreCase = true) -> Icons.Default.WbSunny
//                                weatherData.description.contains("cloud", ignoreCase = true) -> Icons.Default.Cloud
//                                weatherData.description.contains("rain", ignoreCase = true) -> Icons.Default.Opacity
//                                weatherData.description.contains("snow", ignoreCase = true) -> Icons.Default.AcUnit
//                                weatherData.description.contains("thunder", ignoreCase = true) -> Icons.Default.Bolt
//                                else -> Icons.Default.Close
//                            },
//                            contentDescription = weatherData.description,
//                            modifier = Modifier.size(80.dp)
//                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Temperature
                        Text(
                            text = "${weatherData.temperature.roundToInt()}°C",
                            style = MaterialTheme.typography.titleMedium
                        )

                        // Description
                        Text(
                            text = weatherData.description,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Divider(modifier = Modifier.padding(vertical = 16.dp))

                        // Details row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Humidity
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Humidity"
                                )
                                Text("Humidity")
                                Text(
                                    text = "${weatherData.humidity}%",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Wind speed
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Wind"
                                )
                                Text("Wind")
                                Text(
                                    text = "${weatherData.windSpeed} m/s",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}