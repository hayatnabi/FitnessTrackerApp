package compose.fitness.tracker

import android.content.Context
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import compose.fitness.tracker.ui.theme.FitnessTrackerComposeTheme
import ir.ehsannarmani.compose_charts.ColumnChart
import ir.ehsannarmani.compose_charts.models.BarProperties
import ir.ehsannarmani.compose_charts.models.Bars
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private lateinit var sharedPreferences: SharedPreferences

    private var totalSteps by mutableFloatStateOf(0f)
    private var previousSteps by mutableFloatStateOf(0f)
    private var history by mutableStateOf(mapOf<String, Int>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("stepPrefs", Context.MODE_PRIVATE)
        previousSteps = sharedPreferences.getFloat("previousSteps", 0f)
        history = loadHistory()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        setContent {
            FitnessTrackerComposeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StepCounterScreen(
                        steps = (totalSteps - previousSteps).coerceAtLeast(0f),
                        onReset = { resetSteps() },
                        history = history
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }


    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            totalSteps = it.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun resetSteps() {
        previousSteps = totalSteps
        sharedPreferences.edit().putFloat("previousSteps", previousSteps).apply()
        saveTodaySteps((totalSteps - previousSteps).toInt())
        history = loadHistory()
    }

    private fun getTodayKey(): String {
        val sdf = SimpleDateFormat("EEE", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun saveTodaySteps(steps: Int) {
        val updatedHistory = loadHistory().toMutableMap()
        updatedHistory[getTodayKey()] = steps
        sharedPreferences.edit().putString("stepHistory", updatedHistory.entries.joinToString("|") { "${it.key},${it.value}" }).apply()
    }

    private fun loadHistory(): Map<String, Int> {
        val data = sharedPreferences.getString("stepHistory", "") ?: ""
        return data.split("|").mapNotNull {
                val parts = it.split(",")
                if (parts.size == 2) {
                    val value = parts[1].toIntOrNull()
                    if (value != null) parts[0] to value else null
                } else null
            }.toMap()
    }
}

@Composable
fun StepCounterScreen(steps: Float, onReset: () -> Unit, history: Map<String, Int>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Steps Taken", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 16.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = steps.toInt().toString(), style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onReset, colors = ButtonDefaults.buttonColors(Color.Red)) {
            Text("Reset Steps")
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("7-Days History", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 16.dp))
        Spacer(modifier = Modifier.height(22.dp))
        StepHistoryChart(history)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStepCounterScreen() {
    FitnessTrackerComposeTheme {
        StepCounterScreen(
            steps = (0f).coerceAtLeast(0f),
            onReset = {  },
            history = mapOf(
                "Sun" to 5000,
                "Mon" to 7000,
                "Tue" to 8000,
                "Wed" to 6000,
                "Thu" to 9000,
                "Fri" to 10000,
                "Sat" to 12000
            )
        )
    }
}

//@Composable
//fun StepHistoryChart(history: Map<String, Int>) {
//    val types = listOf("Today's Steps", "Other Day's Steps")
////    val today = SimpleDateFormat("EEE", Locale.getDefault()).format(Date())
//
//    val normalizedHistory = types.map { day ->
//        val value = history[day] ?: (5000..15000).random() // fallback to fake realistic values
//        day to value
//    }.toMap()
//
//    val bars = listOf(
//        Bars(
//            label = "Days",
//            values = normalizedHistory.map { (type, steps) ->
//                val isToday = type == "Today's Steps"
//                Bars.Data(
//                    label = type,
//                    value = steps.toDouble(),
//                    color = if (isToday)
//                        Brush.verticalGradient(
//                            colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784)) // Green gradient
//                        )
//                    else
//                        Brush.verticalGradient(
//                            colors = listOf(Color(0xFF1976D2), Color(0xFF64B5F6)) // Blue gradient
//                        )
//                )
//            }
//        )
//    )
//
////    ColumnChart(
////        modifier = Modifier
////            .fillMaxWidth()
////            .height(220.dp)
////            .padding(horizontal = 22.dp),
////        data = bars,
////        barProperties = BarProperties(
////            cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
////            spacing = 3.dp,
////            thickness = 20.dp
////        ),
////        animationSpec = spring(
////            dampingRatio = Spring.DampingRatioMediumBouncy,
////            stiffness = Spring.StiffnessLow
////        )
////    )
//
//    ColumnChart(
//        modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
//        data = remember {
//            listOf(
//                Bars(
//                    label = "Jan", values = listOf(
//                        Bars.Data(
//                            label = "Today's Steps",
//                            value = normalizedHistory["Today's Steps"]?.toDouble() ?: 0.0,
//                            color = Brush.verticalGradient(
//                                colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
//                            )
//                        ),
//                        Bars.Data(
//                            label = "Other Days",
//                            value = normalizedHistory["Other Day's Steps"]?.toDouble() ?: 0.0,
//                            color = Brush.verticalGradient(
//                                colors = listOf(Color(0xFF1976D2), Color(0xFF64B5F6))
//                            )
//                        )
//                    )))
//        },
//        barProperties = BarProperties(
//            cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
//            spacing = 3.dp,
//            thickness = 20.dp
//        ),
//        animationSpec = spring(
//            dampingRatio = Spring.DampingRatioMediumBouncy,
//            stiffness = Spring.StiffnessLow
//        )
//    )
//
//
////    Column(
////        modifier = Modifier
////            .fillMaxWidth()
////            .padding(horizontal = 22.dp)
////    ) {
////        // Legend
////        Row(
////            modifier = Modifier.fillMaxWidth(),
////            horizontalArrangement = Arrangement.SpaceEvenly
////        ) {
////            LegendItem("Today's Steps", Color(0xFF4CAF50), Color(0xFF81C784))
////            LegendItem("Other Days", Color(0xFF1976D2), Color(0xFF64B5F6))
////        }
////
////        Spacer(modifier = Modifier.height(16.dp))
////
////    }
//}
//
//@Composable
//fun LegendItem(label: String, startColor: Color, endColor: Color) {
//    Row(verticalAlignment = Alignment.CenterVertically) {
//        Box(
//            modifier = Modifier
//                .size(16.dp, 16.dp)
//                .background(
//                    brush = Brush.verticalGradient(colors = listOf(startColor, endColor)),
//                    shape = MaterialTheme.shapes.small
//                )
//        )
//        Spacer(modifier = Modifier.width(8.dp))
//        Text(label, style = MaterialTheme.typography.bodyMedium)
//    }
//}

@Composable
fun StepHistoryChart(history: Map<String, Int>) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val today = SimpleDateFormat("EEE", Locale.getDefault()).format(Date())
    val bars = listOf(
        Bars(
            label = "\tM\t\t\t\tT\t\t\tW\t\t\tT\t\t\tF\t\t\tS\t\t\tS", // No label at bottom
            values = days.map { day ->
                val steps = (history[day] ?: 0).coerceIn(3000, 15000)
                Bars.Data(
                    label = day,
                    value = steps.rangeTo(15000).random().toDouble(), // Randomize for demo purposes
                    color = if (day == today)
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF1976D2), Color(0xFF64B5F6)) // Blue for Today
                        )
                    else
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784)) // Green for Others
                        )
                )
            }
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
    ) {
        // CHART
        ColumnChart(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            data = bars,
            barProperties = BarProperties(
                cornerRadius = Bars.Data.Radius.Rectangle(topLeft = 6.dp, topRight = 6.dp),
                spacing = 6.dp,
                thickness = 22.dp
            ),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            ),
//            showYAxis = true,
//            yAxisRange = 5000f..15000f // Visible step range
        )

        Spacer(modifier = Modifier.height(12.dp))

        // X-axis Labels (Mon - Sun)
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            days.forEach { day ->
//                Text(
//                    text = day,
//                    style = MaterialTheme.typography.labelSmall,
//                    color = if (day == today) Color(0xFF1976D2) else Color.Gray,
//                    modifier = Modifier.weight(1f),
//                )
//            }
//        }

        Spacer(modifier = Modifier.height(8.dp))

        // LEGEND (Just 2 indicators below)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendDot(color = Brush.verticalGradient(listOf(Color(0xFF1976D2), Color(0xFF64B5F6))), label = "Today's Steps")
            LegendDot(color = Brush.verticalGradient(listOf(Color(0xFF4CAF50), Color(0xFF81C784))), label = "Other Days")
        }
    }
}

@Composable
fun LegendDot(color: Brush, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(brush = color, shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}



//LATEST
//@Composable
//fun StepHistoryChart(history: Map<String, Int>) {
//    val types = listOf("Today's Steps", "Other Day's Steps")
//    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
//    val today = SimpleDateFormat("EEE", Locale.getDefault()).format(Date())
//    val bars = listOf(
//        Bars(
//            label = "",
//            values = days.map { day ->
//                val isToday = day == today
//                Bars.Data(
//                    label = day,
//                    value = (history[day] ?: 0).toDouble(),
//                    color = if (isToday)
//                        Brush.verticalGradient(
//                            colors = listOf(Color(0xFF1976D2), Color(0xFF64B5F6))
//                        )
//                    else
//                        Brush.verticalGradient(
//                            colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
//                        )
//                )
//            }
//        )
//    )
//
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(horizontal = 22.dp)
//    ) {
//        ColumnChart(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(220.dp),
//            data = bars,
//            barProperties = BarProperties(
//                cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
//                spacing = 3.dp,
//                thickness = 20.dp
//            ),
//            animationSpec = spring(
//                dampingRatio = Spring.DampingRatioMediumBouncy,
//                stiffness = Spring.StiffnessLow
//            ),
//        )
//        Spacer(modifier = Modifier.height(12.dp))
//        Row(
//            modifier = Modifier.fillMaxWidth(),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            days.forEach { day ->
//                Text(
//                    text = day,
//                    style = MaterialTheme.typography.bodySmall,
//                    modifier = Modifier.weight(1f),
//                    color = if (day == today) Color(0xFF1976D2) else Color.Unspecified
//                )
//            }
//        }
//    }
//}


// new chart with current steps also:
//@Composable
//fun StepHistoryChart(history: Map<String, Int>) {
//    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
//    val today = SimpleDateFormat("EEE", Locale.getDefault()).format(Date())
//    val maxSteps = (history.values.maxOrNull() ?: 1).coerceAtLeast(1)
//    val bars = listOf(
//        Bars(
//            label = "Steps",
//            values = days.map { day ->
//                val isToday = day == today
//                Bars.Data(
//                    label = day,
//                    value = (history[day] ?: 0).toDouble(),
//                    color = if (isToday)
//                        Brush.verticalGradient(
//                            colors = listOf(Color(0xFF1976D2), Color(0xFF64B5F6))
//                        )
//                    else
//                        Brush.verticalGradient(
//                            colors = listOf(Color(0xFF4CAF50), Color(0xFF81C784))
//                        )
//                )
//            }
//        )
//    )
//    ColumnChart(
//        modifier = Modifier
//            .fillMaxWidth()
//            .height(220.dp)
//            .padding(horizontal = 22.dp),
//        data = bars,
//        barProperties = BarProperties(
//            cornerRadius = Bars.Data.Radius.Rectangle(topRight = 6.dp, topLeft = 6.dp),
//            spacing = 3.dp,
//            thickness = 20.dp
//        ),
//        animationSpec = spring(
//            dampingRatio = Spring.DampingRatioMediumBouncy,
//            stiffness = Spring.StiffnessLow
//        ),
//    )
//}

@Preview(showBackground = true)
@Composable
fun PreviewStepHistoryChart() {
    StepHistoryChart(
        history = mapOf(
            "Sun" to 5000,
            "Mon" to 7000,
            "Tue" to 8000,
            "Wed" to 6000,
            "Thu" to 9000,
            "Fri" to 10000,
            "Sat" to 12000
        )
    )
}

