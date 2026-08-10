package com.example.householdapp.workouts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.householdapp.core.model.WorkoutEntry
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.core.ui.components.LevelUpDialog
import com.example.householdapp.core.ui.components.Pill
import com.example.householdapp.core.ui.components.SectionHeader
import com.example.householdapp.core.ui.formatTimestamp

private data class WorkoutPreset(val label: String, val icon: ImageVector)

private val workoutPresets = listOf(
    WorkoutPreset("Running", Icons.Filled.DirectionsRun),
    WorkoutPreset("Walking", Icons.Filled.SelfImprovement),
    WorkoutPreset("Cycling", Icons.Filled.DirectionsBike),
    WorkoutPreset("Swimming", Icons.Filled.Pool),
    WorkoutPreset("Gym", Icons.Filled.FitnessCenter)
)

private val durationPresets = listOf(15, 30, 45, 60, 90)

private val intensityOptions = listOf(
    "light" to "Light",
    "medium" to "Moderate",
    "intense" to "Intense"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutsScreen(
    workoutViewModel: WorkoutViewModel = viewModel()
) {
    val sessionState by SessionManager.sessionState.collectAsState()
    val uiState by workoutViewModel.uiState.collectAsState()
    val user = sessionState.user
    val partner = sessionState.partner

    var workoutType by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("30") }
    var intensity by remember { mutableStateOf("medium") }
    var notes by remember { mutableStateOf("") }
    var exerciseTogether by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { workoutViewModel.loadWorkouts() }

    val duration = durationText.toIntOrNull() ?: 0
    val previewXp = (duration / 10) * 10
    val previewCoins = if (duration > 0) 3 else 0
    val boostedXp = Math.round(previewXp * 1.05).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        GradientHeader(
            title = "Workouts",
            subtitle = if (user != null) "Logged in as ${user.displayName}" else "Sign in to log workouts"
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (partner != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Exercise Together",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "+5% Streak Bonus for both \u2014 this workout is logged for you and ${partner.displayName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        Switch(
                            checked = exerciseTogether,
                            onCheckedChange = {
                                exerciseTogether = it
                                workoutViewModel.clearMessage()
                            }
                        )
                    }
                }
            }
            if (user != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        text = "${user.xpTotal} XP",
                        icon = Icons.Filled.Bolt,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Pill(
                        text = "${user.coinsTotal} coins",
                        icon = Icons.Filled.MonetizationOn,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Pill(
                        text = "${user.currentStreak} day streak",
                        icon = Icons.Filled.LocalFireDepartment,
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            uiState.successMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    workoutPresets.forEach { preset ->
                        FilterChip(
                            selected = workoutType == preset.label,
                            onClick = {
                                workoutType = preset.label
                                workoutViewModel.clearMessage()
                            },
                            label = { Text(preset.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = preset.icon,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        )
                    }
                }
                OutlinedTextField(
                    value = workoutType,
                    onValueChange = {
                        workoutType = it
                        workoutViewModel.clearMessage()
                    },
                    label = { Text("Or type a custom workout") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Duration (minutes)", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    durationPresets.forEach { preset ->
                        FilterChip(
                            selected = durationText == preset.toString(),
                            onClick = {
                                durationText = preset.toString()
                                workoutViewModel.clearMessage()
                            },
                            label = { Text("$preset min") }
                        )
                    }
                }
                OutlinedTextField(
                    value = durationText,
                    onValueChange = {
                        durationText = it.filter { ch -> ch.isDigit() }.take(3)
                        workoutViewModel.clearMessage()
                    },
                    label = { Text("Custom duration") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Intensity", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    intensityOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = intensity == value,
                            onClick = {
                                intensity = value
                                workoutViewModel.clearMessage()
                            },
                            label = { Text(label) }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                    workoutViewModel.clearMessage()
                },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (duration > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (exerciseTogether) "You'll each earn" else "You'll earn",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill(
                                text = if (exerciseTogether) "+$boostedXp XP" else "+$previewXp XP",
                                icon = Icons.Filled.Bolt,
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Pill(
                                text = "+$previewCoins coins",
                                icon = Icons.Filled.MonetizationOn,
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (exerciseTogether) {
                            Text(
                                text = "+5% Exercise Together bonus applied",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    workoutViewModel.submitWorkout(
                        workoutType = workoutType,
                        durationText = durationText,
                        intensity = intensity,
                        notes = notes,
                        exerciseTogether = exerciseTogether
                    ) {
                        workoutType = ""
                        durationText = "30"
                        intensity = "medium"
                        notes = ""
                    }
                },
                enabled = user != null && !uiState.isSubmitting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (uiState.isSubmitting) "Saving..." else "Log Workout",
                    fontWeight = FontWeight.SemiBold
                )
            }

            SectionHeader(
                title = "Recent workouts",
                count = uiState.workouts.size.takeIf { it > 0 }
            )

            when {
                uiState.isLoadingHistory -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.workouts.isEmpty() -> {
                    Text(
                        text = "No workouts logged yet. Your history will appear here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                else -> {
                    uiState.workouts.forEach { workout ->
                        WorkoutHistoryRow(workout)
                    }
                }
            }
        }
    }

    LevelUpDialog(
        event = uiState.levelUpEvent,
        onDismiss = { workoutViewModel.consumeLevelUp() }
    )
}

@Composable
private fun WorkoutHistoryRow(workout: WorkoutEntry) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.workoutType,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${workout.durationMinutes} min \u00b7 ${workout.intensity.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatTimestamp(workout.loggedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (workout.xpReward > 0 || workout.coinReward > 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Pill(
                        text = "+${workout.xpReward} XP",
                        icon = Icons.Filled.Bolt,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Pill(
                        text = "+${workout.coinReward} coins",
                        icon = Icons.Filled.MonetizationOn,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}
