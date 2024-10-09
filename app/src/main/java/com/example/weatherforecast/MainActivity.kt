package com.example.weatherforecast

import DialogSearch
import MainScreen
import TabLayout
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.weatherforecast.ui.theme.WeatherForecastTheme
import data.WeatherModel
import org.json.JSONObject

const val API_KEY = " 8227ae3d22ea472bbab120915240910"
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherForecastTheme {
                val daysList = remember {
                    mutableStateOf(listOf<WeatherModel>())
                }
                val dialogState = remember {
                    mutableStateOf(false)
                }

                val currentDay = remember {
                    mutableStateOf(
                        WeatherModel(
                            "",
                            "",
                            "0.0",
                            "",
                            "",
                            "0.0",
                            "0.0",
                            ""
                        )
                    )
                }
                if (dialogState.value) {
                    DialogSearch(dialogState, onSubmit = {
                        getData(it, this, daysList, currentDay)
                    })
                }
                getData("London", this, daysList, currentDay)
                Image(
                    painter = painterResource(
                        id = R.drawable.sky
                    ),
                    contentDescription = "im1",
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.5f),
                    contentScale = ContentScale.FillBounds
                )
                Column {
                    MainScreen(currentDay, onClickSync = {
                        getData("London", this@MainActivity, daysList, currentDay)
                    }, onClickSearch = {
                        dialogState.value = true
                    }
                    )
                    TabLayout(daysList, currentDay)
                }

            }
        }
    }
}

private fun getData(
    city: String, context: Context,
    daysList: MutableState<List<WeatherModel>>,
    currentDay: MutableState<WeatherModel>
) {
    val url = "https://api.weatherapi.com/v1/forecast.json?key=$API_KEY&q=$city&days=3&aqi=no&alerts=no"
    val queue = Volley.newRequestQueue(context)

    val sRequest = StringRequest(
        com.android.volley.Request.Method.GET,
        url,
        { response ->
            try {
                val list = getWeatherByDays(response)
                if (list.isNotEmpty()) {
                    currentDay.value = list[0]
                    daysList.value = list
                } else {
                    Log.d("MyLog", "Empty weather data for city: $city")
                }
            } catch (e: Exception) {
                Log.e("MyLog", "Error parsing weather data: ${e.message}")
            }
        },
        { error ->
            Log.e("MyLog", "VolleyError: ${error.message}")
        }
    )

    queue.add(sRequest)
}

private fun getWeatherByDays(response: String): List<WeatherModel> {
    if (response.isEmpty()) return listOf()

    val list = ArrayList<WeatherModel>()
    try {
        val mainObject = JSONObject(response)
        val city = mainObject.getJSONObject("location").getString("name")
        val days = mainObject.getJSONObject("forecast").getJSONArray("forecastday")

        for (i in 0 until days.length()) {
            val item = days.getJSONObject(i)
            val condition = item.getJSONObject("day").getJSONObject("condition")

            list.add(
                WeatherModel(
                    city,
                    item.getString("date"),
                    "",
                    condition.getString("text"),
                    condition.getString("icon"),
                    item.getJSONObject("day").getString("maxtemp_c"),
                    item.getJSONObject("day").getString("mintemp_c"),
                    item.getJSONArray("hour").toString()
                )
            )
        }

        val current = mainObject.getJSONObject("current")
        list[0] = list[0].copy(
            time = current.getString("last_updated"),
            currentTemp = current.getString("temp_c"),
        )
    } catch (e: Exception) {
        Log.e("MyLog", "Error parsing JSON: ${e.message}")
    }
    return list
}
