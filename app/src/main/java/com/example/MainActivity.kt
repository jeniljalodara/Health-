package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.HeightGrowthViewModel
import com.example.ui.HeightGrowthViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Simple screen routing
enum class Screen(val title: String) {
    Dashboard("Dashboard"),
    Nutrition("Nutrition"),
    Sleep("Sleep"),
    Exercises("Exercises"),
    Chat("AI Coach"),
    Profile("Profile")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Core single-action initialization
        val db = HeightGrowthDatabase.getDatabase(applicationContext)
        val repository = HeightGrowthRepository(db.heightGrowthDao())

        setContent {
            MyApplicationTheme {
                val viewModel: HeightGrowthViewModel = viewModel(
                    factory = HeightGrowthViewModelFactory(repository)
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreenContent(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreenContent(viewModel: HeightGrowthViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    var currentScreen by remember { mutableStateOf(Screen.Dashboard) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                tonalElevation = 8.dp
            ) {
                listOf(
                    Screen.Dashboard to Icons.Default.Home,
                    Screen.Nutrition to Icons.Default.Favorite,
                    Screen.Sleep to Icons.Default.Star,
                    Screen.Exercises to Icons.Default.PlayArrow,
                    Screen.Chat to Icons.Default.Send,
                    Screen.Profile to Icons.Default.Person
                ).forEach { (screen, icon) ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = screen.title,
                                modifier = Modifier.testTag("nav_btn_${screen.name.lowercase()}")
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (currentScreen == screen) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Dashboard -> DashboardScreen(viewModel, onNavigateToProfile = { currentScreen = Screen.Profile })
                Screen.Nutrition -> NutritionScreen(viewModel)
                Screen.Sleep -> SleepScreen(viewModel)
                Screen.Exercises -> ExercisesScreen(viewModel)
                Screen.Chat -> AICoachScreen(viewModel)
                Screen.Profile -> ProfileScreen(viewModel)
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(viewModel: HeightGrowthViewModel, onNavigateToProfile: () -> Unit) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val heightRecords by viewModel.heightRecords.collectAsStateWithLifecycle()
    val sleepRecords by viewModel.sleepRecords.collectAsStateWithLifecycle()
    val nutritionRecords by viewModel.nutritionRecords.collectAsStateWithLifecycle()
    val exerciseRecords by viewModel.exerciseRecords.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var showAddHeightDialog by remember { mutableStateOf(false) }

    // Aggregate daily completion indicators for rings
    val todaySleep = sleepRecords.filter { it.date == selectedDate }.sumOf { it.durationHours.toDouble() }.toFloat()
    val todayCalories = nutritionRecords.filter { it.date == selectedDate }.sumOf { it.calories }
    val todayProtein = nutritionRecords.filter { it.date == selectedDate }.sumOf { it.proteinGrams.toDouble() }.toFloat()
    val todayCalcium = nutritionRecords.filter { it.date == selectedDate }.sumOf { it.calciumMg.toDouble() }.toFloat()
    val todayExercises = exerciseRecords.filter { it.date == selectedDate }

    val calorieProgress = (todayCalories / 2200f).coerceIn(0f, 1f)
    val calciumProgress = (todayCalcium / 1000f).coerceIn(0f, 1f)
    val sleepProgress = (todaySleep / 9f).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initials = (userProfile?.name ?: "Grower")
                        .split(" " )
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .joinToString("")
                        .take(2)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Column {
                        Text(
                            text = "Good morning,",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = userProfile?.name ?: "Grower",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                IconButton(
                    onClick = onNavigateToProfile,
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .testTag("dashboard_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Selected Date Toggle Bar
        item {
            DateSelectorBar(selectedDate, onDateSelected = { viewModel.setDate(it) })
        }

        // Hero Height Indicator Board
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("height_stats_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Height Progress Tracker",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Button(
                            onClick = { showAddHeightDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Height")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Height", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Current Height",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                "${userProfile?.currentHeight ?: 165f} cm",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Genetic Potential",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                "${String.format(Locale.getDefault(), "%.1f", userProfile?.getGeneticHeightLimit() ?: 170.5f)} cm",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Target Height",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                "${userProfile?.targetHeight ?: 180f} cm",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Progress indicators
                    val initialH = heightRecords.firstOrNull()?.height ?: 165.0f
                    val currentH = userProfile?.currentHeight ?: 165.0f
                    val targetH = userProfile?.targetHeight ?: 180.0f
                    val diffDone = currentH - initialH
                    val diffTotal = targetH - initialH
                    val percent = if (diffTotal > 0f) (diffDone / diffTotal).coerceIn(0f, 1f) else 1f

                    LinearProgressIndicator(
                        progress = { percent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Started: $initialH cm",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Gained: +${String.format(Locale.getDefault(), "%.1f", diffDone)} cm",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Target: $targetH cm",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Custom Height Vector line chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Growth Progression Trend",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Shows your bone vertical development over logs",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Hand-drawn chart
                    HeightCustomLineChart(heightRecords)
                }
            }
        }

        // Healthy Daily Progress Rings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Daily Nutrient & Sleep Status",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProgressCircleBox(
                            progress = sleepProgress,
                            title = "Sleep",
                            valueString = "${todaySleep}h",
                            subLabel = "Goal 9h",
                            color = MaterialTheme.colorScheme.primary
                        )
                        ProgressCircleBox(
                            progress = calorieProgress,
                            title = "Calories",
                            valueString = "$todayCalories",
                            subLabel = "Goal 2200",
                            color = MaterialTheme.colorScheme.secondary
                        )
                        ProgressCircleBox(
                            progress = calciumProgress,
                            title = "Calcium",
                            valueString = "${todayCalcium.toInt()}mg",
                            subLabel = "Goal 1000",
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Guided Exercises brief
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Today's Growth Exercises",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        OutlinedButton(
                            onClick = { /* Exercise tab */ },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Exercises")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (todayExercises.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Exercises empty",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "No exercises logged today yet. Complete a guided routine!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            todayExercises.forEach { record ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Done",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            record.exerciseName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        "${record.durationMinutes} min",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Add Height Dialog
    if (showAddHeightDialog) {
        var heightInput by remember { mutableStateOf("") }
        var inputError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddHeightDialog = false },
            title = { Text("Log New Height Measurement") },
            text = {
                Column {
                    Text(
                        "Keep track of your physical growth regularly to map details properly.",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = {
                            heightInput = it
                            inputError = false
                        },
                        label = { Text("Height in cm") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = inputError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("diag_height_input"),
                        singleLine = true
                    )
                    if (inputError) {
                        Text(
                            "Please enter a valid height (e.g. 172.5)",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val h = heightInput.toFloatOrNull()
                        if (h != null && h in 50f..250f) {
                            viewModel.addHeightRecord(h)
                            showAddHeightDialog = false
                        } else {
                            inputError = true
                        }
                    },
                    modifier = Modifier.testTag("diag_height_confirm")
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddHeightDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// CUSTOM VECTOR LINE CHART COMPONENT
// ==========================================
@Composable
fun HeightCustomLineChart(records: List<HeightRecord>) {
    val context = LocalContext.current
    if (records.size < 2) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Logs missing",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Need at least 2 height logs to draw progress chart.",
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        return
    }

    val sorted = records.sortedBy { it.timestamp }
    val heights = sorted.map { it.height }
    val minHeight = heights.minOrNull()?.minus(1f) ?: 150f
    val maxHeight = heights.maxOrNull()?.plus(1f) ?: 190f
    val rangeY = if (maxHeight == minHeight) 1f else maxHeight - minHeight

    val labelSdf = SimpleDateFormat("MM-dd", Locale.getDefault())

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(12.dp)
    ) {
        val width = size.width
        val height = size.height
        val stepX = width / (sorted.size - 1)

        val path = Path()
        val fillPath = Path()

        sorted.forEachIndexed { index, record ->
            val ratioY = (record.height - minHeight) / rangeY
            val x = index * stepX
            val y = height - (ratioY * height)

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (index == sorted.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }

            // Draw data text
            drawCircle(
                color = Color(0xFF10B981),
                radius = 5.dp.toPx(),
                center = Offset(x, y)
            )

            // Draw labels
            try {
                val label = labelSdf.format(Date(record.timestamp))
                // Simple dot marker
            } catch (e: Exception) {}
        }

        // Stroke line
        drawPath(
            path = path,
            color = Color(0xFF10B981),
            style = Stroke(width = 3.dp.toPx())
        )

        // Gradient fill underneath
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF10B981).copy(alpha = 0.35f),
                    Color.Transparent
                )
            )
        )
    }
}

@Composable
fun ProgressCircleBox(
    progress: Float,
    title: String,
    valueString: String,
    subLabel: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(80.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track
                drawArc(
                    color = color.copy(alpha = 0.15f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx())
                )
                // Fill
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx())
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = valueString,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = subLabel,
                    fontSize = 8.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Text(
            text = title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun DateSelectorBar(selectedDate: String, onDateSelected: (String) -> Unit) {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val dayNumFormat = SimpleDateFormat("dd", Locale.getDefault())
    val dayNameFormat = SimpleDateFormat("E", Locale.getDefault())

    val todayCalendar = Calendar.getInstance()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Generate previous 6 days and today
        for (i in -6..0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, i)
            val dateStr = sdf.format(cal.time)
            val isSelected = dateStr == selectedDate

            Card(
                onClick = { onDateSelected(dateStr) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .width(55.dp)
                    .height(65.dp)
                    .testTag("date_card_$dateStr")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayNameFormat.format(cal.time).uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dayNumFormat.format(cal.time),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. NUTRITION SCREEN
// ==========================================
@Composable
fun NutritionScreen(viewModel: HeightGrowthViewModel) {
    val nutritionRecords by viewModel.nutritionRecords.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var showLogFoodDialog by remember { mutableStateOf(false) }

    // Constants for goals related directly to vertical growth optimization
    val targetCalories = 2200
    val targetProtein = 75f // grams
    val targetCalcium = 1000f // mg (critical for length-plates bone development!)

    // Filtered data
    val todayRecords = nutritionRecords.filter { it.date == selectedDate }
    val totalCalories = todayRecords.sumOf { it.calories }
    val totalProtein = todayRecords.sumOf { it.proteinGrams.toDouble() }.toFloat()
    val totalCalcium = todayRecords.sumOf { it.calciumMg.toDouble() }.toFloat()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Bone Building Nutrients",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Calcium & Protein stimulate epiphyseal growth plates.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                IconButton(
                    onClick = { showLogFoodDialog = true },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("log_food_button")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Log Food",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // Selection board for Day
        item {
            DateSelectorBar(selectedDate, onDateSelected = { viewModel.setDate(it) })
        }

        // Stats card progress indicators
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Nutrient Intake Target Progress",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Calories progress bar
                    LinearNutritionBar(
                        label = "Energy / Calories",
                        current = totalCalories.toFloat(),
                        target = targetCalories.toFloat(),
                        unit = "kcal",
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Proteins progress bar (Growth hormone driver)
                    LinearNutritionBar(
                        label = "Proteins (Amino acid plate bricks)",
                        current = totalProtein,
                        target = targetProtein,
                        unit = "g",
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Calcium progress bar (Bones calcification length catalyst)
                    LinearNutritionBar(
                        label = "Calcium (Bone mineral volume density catalyst)",
                        current = totalCalcium,
                        target = targetCalcium,
                        unit = "mg",
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Quick log health boosters triggers section
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Quick Health & Height Boosters",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Tap to log rich, standard growth superfoods immediately:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Standard triggers for Milk, Eggs, Greek Yogurt, Spinach
                    val boosters = listOf(
                        Triple("Milk (1 Cup)", Pair(120, 8f), 300f),
                        Triple("Organic Egg (1)", Pair(70, 6f), 25f),
                        Triple("Salmon Fillet (100g)", Pair(200, 22f), 15f),
                        Triple("Spinach Portion (Cup)", Pair(10, 1f), 45f),
                        Triple("Greek Yogurt (Cup)", Pair(130, 12f), 180f)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        boosters.forEach { (name, vit, calc) ->
                            Button(
                                onClick = {
                                    viewModel.addNutritionRecord(
                                        foodName = name,
                                        calories = vit.first,
                                        protein = vit.second,
                                        calcium = calc
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        "P:${vit.second.toInt()}g Ca:${calc.toInt()}mg",
                                        fontSize = 9.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // List of entries
        item {
            Text(
                "Logged items for this day",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (todayRecords.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No food logged for this day. Click log to insert details!",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(todayRecords) { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(record.foodName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("${record.calories} kcal", fontSize = 12.sp)
                            Text("Protein: ${record.proteinGrams}g", fontSize = 12.sp)
                            Text("Calcium: ${record.calciumMg.toInt()}mg", fontSize = 12.sp)
                        }
                    }
                    IconButton(
                        onClick = { viewModel.deleteNutritionRecord(record.id) },
                        modifier = Modifier.testTag("delete_food_${record.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Item",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Modal dialog to Add custom Food Log
    if (showLogFoodDialog) {
        var foodName by remember { mutableStateOf("") }
        var caloriesInput by remember { mutableStateOf("") }
        var proteinInput by remember { mutableStateOf("") }
        var calciumInput by remember { mutableStateOf("") }

        var validationError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showLogFoodDialog = false },
            title = { Text("Log Custom Food Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text("Food / Meal Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("diag_food_name")
                    )
                    OutlinedTextField(
                        value = caloriesInput,
                        onValueChange = { caloriesInput = it },
                        label = { Text("Calories (kcal)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = proteinInput,
                        onValueChange = { proteinInput = it },
                        label = { Text("Protein (grams)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = calciumInput,
                        onValueChange = { calciumInput = it },
                        label = { Text("Calcium (mg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (validationError) {
                        Text(
                            "Please complete all fields with accurate numerical values.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val calories = caloriesInput.toIntOrNull()
                        val protein = proteinInput.toFloatOrNull()
                        val calcium = calciumInput.toFloatOrNull()

                        if (foodName.isNotBlank() && calories != null && protein != null && calcium != null) {
                            viewModel.addNutritionRecord(
                                foodName = foodName,
                                calories = calories,
                                protein = protein,
                                calcium = calcium
                            )
                            showLogFoodDialog = false
                        } else {
                            validationError = true
                        }
                    },
                    modifier = Modifier.testTag("diag_food_confirm")
                ) {
                    Text("Log Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogFoodDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LinearNutritionBar(
    label: String,
    current: Float,
    target: Float,
    unit: String,
    color: Color
) {
    val faction = if (target > 0f) (current / target).coerceIn(0f, 1f) else 1f
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${current.toInt()} / ${target.toInt()} $unit",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        LinearProgressIndicator(
            progress = { faction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = color,
            trackColor = color.copy(alpha = 0.15f)
        )
    }
}

// ==========================================
// 3. SLEEP TRACKER SCREEN
// ==========================================
@Composable
fun SleepScreen(viewModel: HeightGrowthViewModel) {
    val sleepRecords by viewModel.sleepRecords.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var showLogSleepDialog by remember { mutableStateOf(false) }

    val todaySleepEntries = sleepRecords.filter { it.date == selectedDate }
    val totalSleep = todaySleepEntries.sumOf { it.durationHours.toDouble() }.toFloat()

    val hghScore = (totalSleep / 9f).coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "HGH Secretion Sleep",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Human Growth Hormone releases maximally during deep sleep.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                IconButton(
                    onClick = { showLogSleepDialog = true },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .testTag("log_sleep_button")
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Log Sleep",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        item {
            DateSelectorBar(selectedDate, onDateSelected = { viewModel.setDate(it) })
        }

        // Deep Sleep HGH Secretary Report
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProgressCircleBox(
                        progress = hghScore,
                        title = "HGH Surge Ratio",
                        valueString = "${todaySleepEntries.firstOrNull()?.durationHours ?: 0f}h",
                        subLabel = "Goal 9h",
                        color = MaterialTheme.colorScheme.primary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Growth Index Feedback",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val advice = when {
                            totalSleep >= 9 -> "Exceptional slumber! Your pituitary gland releases continuous Growth Hormone spurts. Keep this schedule."
                            totalSleep >= 7 -> "Good sleep, but adding 1-2 more hours will fully decompress and extend spinal cartilage discs for extra micro height."
                            else -> "Insufficient rest detected. Growth plates require active spinal unloading and steady deep sleep to grow safely. Aim for 9h!"
                        }
                        Text(
                            advice,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        item {
            Text(
                "Logged Sleep Events",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (todaySleepEntries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No sleep records logged for this day. Enter hours slept to check pituitary status!",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(todaySleepEntries) { record ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Duration: ${record.durationHours} Hours",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Quality rating:", fontSize = 12.sp)
                            for (st in 1..5) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "*",
                                    modifier = Modifier.size(12.dp),
                                    tint = if (st <= record.quality) AccentAmber else Color.Gray.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { viewModel.deleteSleepRecord(record.id) },
                        modifier = Modifier.testTag("delete_sleep_${record.id}")
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    if (showLogSleepDialog) {
        var sleepHours by remember { mutableStateOf("") }
        var sleepRating by remember { mutableStateOf(5) }
        var isValError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showLogSleepDialog = false },
            title = { Text("Log Sleep Event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Rate and log sleep length. Proper alignment during sleep enables vertical expansion.",
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = sleepHours,
                        onValueChange = {
                            sleepHours = it
                            isValError = false
                        },
                        label = { Text("Length (hours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = isValError,
                        modifier = Modifier.fillMaxWidth().testTag("diag_sleep_hours")
                    )

                    Column {
                        Text("Rating Quality", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            (1..5).forEach { rate ->
                                IconButton(onClick = { sleepRating = rate }) {
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = "Rate $rate",
                                        tint = if (rate <= sleepRating) AccentAmber else Color.Gray.copy(alpha = 0.3f),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (isValError) {
                        Text(
                            "Please enter a valid length of hours (e.g. 8.5)",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hours = sleepHours.toFloatOrNull()
                        if (hours != null && hours in 1f..24f) {
                            viewModel.addSleepRecord(hours, sleepRating)
                            showLogSleepDialog = false
                        } else {
                            isValError = true
                        }
                    },
                    modifier = Modifier.testTag("diag_sleep_confirm")
                ) {
                    Text("Save Log")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogSleepDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// 4. GUIDED EXERCISES SCREEN
// ==========================================
data class HeightExercise(
    val id: String,
    val name: String,
    val type: String,
    val durationSec: Int,
    val description: String,
    val guideTips: String
)

@Composable
fun ExercisesScreen(viewModel: HeightGrowthViewModel) {
    val exerciseRecords by viewModel.exerciseRecords.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()

    var activeExerciseIndex by remember { mutableStateOf<Int?>(null) }

    // Curated Growth routines
    val routines = listOf(
        HeightExercise(
            "cobra",
            "Cobra Spine Stretch",
            "Spine Extension",
            30,
            "Lie on your belly and raise your upper torso up to decompress lumbar plates.",
            "Breathe fluidly; keep shoulders depressed away from neck ears."
        ),
        HeightExercise(
            "hang",
            "Gravity Decompression Hanging",
            "Cartilage Unloading",
            60,
            "Hang from a horizontal bar securely to extend spinal discs and let gravity stretch leg growth plates.",
            "Keep core and joints relaxed to maximize traction forces."
        ),
        HeightExercise(
            "pelvic",
            "Pelvic Decompression Shift",
            "Lower Dorsal Stretch",
            45,
            "Lie on your back, bend knees, and lift pelvis vertically to elongate thoracic and sacral areas.",
            "Contract gluteal muscles at the apex and hold 3 seconds before descending."
        ),
        HeightExercise(
            "forward",
            "Forward Vertebrae Touch",
            "Hamstring & Sacrum Lengthen",
            30,
            "Sit with legs stretched in front and reach forward to compress vertebrae gaps cleanly.",
            "Hinge strictly from hips; do not round shoulders into tension."
        ),
        HeightExercise(
            "jump",
            "Impact Plateload Jumps",
            "Epiphyseal Stimulation",
            60,
            "Sprint high-jumps to generate micro-stresses on growth cartilage. Promotes calcium calcification.",
            "Bend knees on landing to absorb shock safely."
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Growth Guided Routines",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Postural stretching decompresses spinal cartilage and stimulates growth plates.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
              )
        }

        // Routine Player Card
        item {
            if (activeExerciseIndex != null) {
                val exercise = routines[activeExerciseIndex!!]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("exercise_player"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Routine Player",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = { activeExerciseIndex = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Exit Routine")
                            }
                        }

                        Text(
                            exercise.name,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Focus Area: ${exercise.type}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(exercise.description, fontSize = 13.sp)
                        Text(
                            "Coach Tip: ${exercise.guideTips}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Timer logic
                        ExerciseRoutineTimer(
                            totalSeconds = exercise.durationSec,
                            onFinish = {
                                viewModel.addExerciseRecord(exercise.name, exercise.durationSec / 60 + 1)
                                activeExerciseIndex = null
                            }
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { activeExerciseIndex = 0 },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "Play icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Start Joint Stretching Session",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Complete 5 tailored stature decompression stretching steps.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // List of stretching exercises with brief stats
        item {
            Text(
                "Stature Lengthening Library",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(routines.size) { index ->
            val exercise = routines[index]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .clickable { activeExerciseIndex = index }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${index + 1}. ${exercise.name}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                exercise.type,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        exercise.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Start",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun ExerciseRoutineTimer(totalSeconds: Int, onFinish: () -> Unit) {
    var secondsRemaining by remember(totalSeconds) { mutableIntStateOf(totalSeconds) }
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning, secondsRemaining) {
        if (isRunning && secondsRemaining > 0) {
            delay(1000L)
            secondsRemaining -= 1
        } else if (secondsRemaining == 0) {
            onFinish()
        }
    }

    val fraction = secondsRemaining.toFloat() / totalSeconds.toFloat()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(100.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = Color.Gray.copy(alpha = 0.2f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx())
                )
                drawArc(
                    color = Color(0xFF10B981),
                    startAngle = -90f,
                    sweepAngle = fraction * 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx())
                )
            }
            Text(
                "${secondsRemaining / 60}:${String.format(Locale.getDefault(), "%02d", secondsRemaining % 60)}",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { isRunning = !isRunning },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (isRunning) "Pause" else "Resume")
            }
            OutlinedButton(onClick = { secondsRemaining = totalSeconds }) {
                Text("Reset")
            }
            Button(
                onClick = onFinish,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Skip & Log")
            }
        }
    }
}

// ==========================================
// 5. AI COACH SCREEN
// ==========================================
@Composable
fun AICoachScreen(viewModel: HeightGrowthViewModel) {
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()

    var messageText by remember { mutableStateOf("") }
    val listState = rememberScrollState()

    // Suggestions list
    val prePrompts = listOf(
        "Does milk help height?",
        "Will stretching make me taller after 18?",
        "Best sleeping hours for HGH secretion?",
        "Explain height growth plates"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "AI Growth Coach",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Talk to Dr. Heighten, your stature consultant.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            TextButton(
                onClick = { viewModel.clearChat() },
                modifier = Modifier.testTag("clear_chat_button")
            ) {
                Text("Clear")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat logs
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(listState)
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            chatHistory.forEach { content ->
                val isModel = content.role == "model"
                val text = content.parts.firstOrNull()?.text ?: ""

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isModel) Arrangement.Start else Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .background(
                                color = if (isModel) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isModel) 0.dp else 16.dp,
                                    bottomEnd = if (isModel) 16.dp else 0.dp
                                )
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = text,
                            fontSize = 13.sp,
                            color = if (isModel) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if (isChatLoading) {
                Box(modifier = Modifier.padding(8.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            LaunchedEffect(chatHistory.size, isChatLoading) {
                listState.animateScrollTo(listState.maxValue)
            }
        }

        // Suggested questions
        if (chatHistory.size == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                prePrompts.forEach { prompt ->
                    OutlinedButton(
                        onClick = { viewModel.sendChatMessage(prompt) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(prompt, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Input field
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Ask anything about height growth...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                shape = RoundedCornerShape(20.dp),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendChatMessage(messageText)
                        messageText = ""
                    }
                },
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .testTag("chat_send_button"),
                enabled = !isChatLoading
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// ==========================================
// 6. PROFILE & CONFIGURATION SCREEN
// ==========================================
@Composable
fun ProfileScreen(viewModel: HeightGrowthViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()

    var isEditMode by remember { mutableStateOf(false) }

    // State bindings
    var inputName by remember { mutableStateOf("") }
    var inputAge by remember { mutableStateOf("") }
    var inputGender by remember { mutableStateOf("Male") }
    var inputHeight by remember { mutableStateOf("") }
    var inputTargetHeight by remember { mutableStateOf("") }
    var inputFatherHeight by remember { mutableStateOf("") }
    var inputMotherHeight by remember { mutableStateOf("") }
    var inputWeight by remember { mutableStateOf("") }

    var isErrorActive by remember { mutableStateOf(false) }

    // Sync when profile is loaded
    LaunchedEffect(userProfile, isEditMode) {
        if (!isEditMode && userProfile != null) {
            userProfile?.let {
                inputName = it.name
                inputAge = it.age.toString()
                inputGender = it.gender
                inputHeight = it.currentHeight.toString()
                inputTargetHeight = it.targetHeight.toString()
                inputFatherHeight = it.fatherHeight.toString()
                inputMotherHeight = it.motherHeight.toString()
                inputWeight = it.weight.toString()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("profile_view_tab"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "My Growth Bio Profile",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Manage your gender, age, family height indices, and targets.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Button(
                    onClick = {
                        if (isEditMode) {
                            val age = inputAge.toIntOrNull()
                            val curH = inputHeight.toFloatOrNull()
                            val tarH = inputTargetHeight.toFloatOrNull()
                            val fatH = inputFatherHeight.toFloatOrNull()
                            val motH = inputMotherHeight.toFloatOrNull()
                            val wt = inputWeight.toFloatOrNull()

                            if (inputName.isNotBlank() && age != null && curH != null && tarH != null && fatH != null && motH != null && wt != null) {
                                viewModel.updateProfile(
                                    name = inputName,
                                    age = age,
                                    gender = inputGender,
                                    currentHeight = curH,
                                    targetHeight = tarH,
                                    fatherHeight = fatH,
                                    motherHeight = motH,
                                    weight = wt
                                )
                                isEditMode = false
                                isErrorActive = false
                            } else {
                                isErrorActive = true
                            }
                        } else {
                            isEditMode = true
                        }
                    },
                    modifier = Modifier.testTag("btn_edit_profile")
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Create,
                        contentDescription = "Edit check"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isEditMode) "Save Bio" else "Edit Bio")
                }
            }
        }

        if (isErrorActive) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Text(
                        "Please verify all entries. Input values should be numbers for heights/weights.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isEditMode) {
                        OutlinedTextField(
                            value = inputName,
                            onValueChange = { inputName = it },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputAge,
                                onValueChange = { inputAge = it },
                                label = { Text("Age (Years)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = inputWeight,
                                onValueChange = { inputWeight = it },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Column {
                            Text("Gender Choice for Growth Metrics", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                listOf("Male", "Female").forEach { currentSelectedGender ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { inputGender = currentSelectedGender }
                                    ) {
                                        RadioButton(
                                            selected = inputGender == currentSelectedGender,
                                            onClick = { inputGender = currentSelectedGender }
                                        )
                                        Text(currentSelectedGender)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = inputHeight,
                            onValueChange = { inputHeight = it },
                            label = { Text("Current Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = inputTargetHeight,
                            onValueChange = { inputTargetHeight = it },
                            label = { Text("Target Height (cm)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text("Parents Heights (to map biological limits!)", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputFatherHeight,
                                onValueChange = { inputFatherHeight = it },
                                label = { Text("Father (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = inputMotherHeight,
                                onValueChange = { inputMotherHeight = it },
                                label = { Text("Mother (cm)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Display view mode
                        ProfileValueRow(label = "Full Name", valueStr = inputName)
                        ProfileValueRow(label = "Biological Gender", valueStr = inputGender)
                        ProfileValueRow(label = "Current Age", valueStr = "$inputAge Years")
                        ProfileValueRow(label = "Weight Indicator", valueStr = "$inputWeight kg")
                        ProfileValueRow(label = "Height Metric", valueStr = "$inputHeight cm")
                        ProfileValueRow(label = "Target Height Objective", valueStr = "$inputTargetHeight cm")
                        ProfileValueRow(label = "Father Height Index", valueStr = "$inputFatherHeight cm")
                        ProfileValueRow(label = "Mother Height Index", valueStr = "$inputMotherHeight cm")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Genetic Formula Advice",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Pituitary medical guidelines estimate absolute stature targets by mid-parental indices. " +
                                "While physical genetics defines ~70% of potential, healthy posture, sleep, and calcium intake unlocks the absolute peak within that potential!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
fun ProfileValueRow(label: String, valueStr: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = valueStr,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
