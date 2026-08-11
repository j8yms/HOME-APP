package com.example.householdapp.tasks

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Instant
import java.time.ZoneOffset
import com.example.householdapp.core.model.Task
import com.example.householdapp.core.model.UserProfile
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.EmptyState
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.core.ui.components.LevelUpDialog
import com.example.householdapp.core.ui.components.TaskRow

@Composable
fun TasksScreen(
    tasksViewModel: TasksViewModel = viewModel()
) {
    val uiState by tasksViewModel.uiState.collectAsState()
    val sessionState by SessionManager.sessionState.collectAsState()
    val currentUser = sessionState.user
    val partner = sessionState.partner

    TasksContent(
        uiState = uiState,
        currentUser = currentUser,
        partner = partner,
        onRefresh = { tasksViewModel.loadTasks(forceRefresh = true) },
        onCompleteTask = { tasksViewModel.completeTask(it) },
        onDeleteTask = { tasksViewModel.deleteTask(it) },
        onClaimTask = { tasksViewModel.claimTask(it) },
        onCreateTask = { title, description, category, priority, dueDate, repeatRule, streakEligible, assigneeId, assigneeLabel ->
            tasksViewModel.createTask(title, description, category, priority, dueDate, repeatRule, streakEligible, assigneeId, assigneeLabel) {}
        },
        onUpdateTask = { task, title, description, category, priority, dueDate, repeatRule, streakEligible, assigneeId, assigneeLabel ->
            tasksViewModel.updateTask(task, title, description, category, priority, dueDate, repeatRule, streakEligible, assigneeId, assigneeLabel) {}
        },
        onClearMessage = { tasksViewModel.clearMessage() },
        onDismissLevelUp = { tasksViewModel.consumeLevelUp() }
    )
}

private enum class TaskFilter(val label: String) {
    All("All"), Routines("Routines"), Todo("To do"), Shopping("Shopping"), DateIdeas("Date ideas"), Done("Done")
}

private val repeatRules = listOf(
    "" to "None",
    "daily" to "Daily",
    "weekly" to "Weekly",
    "monthly" to "Monthly"
)

private fun millisToDateString(millis: Long?): String {
    if (millis == null) return ""
    return Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksContent(
    uiState: TasksUiState,
    currentUser: UserProfile?,
    partner: UserProfile?,
    onRefresh: () -> Unit,
    onCompleteTask: (Task) -> Unit,
    onDeleteTask: (Task) -> Unit,
    onClaimTask: (Task) -> Unit,
    onCreateTask: (String, String, String, String, String, String, Boolean, String, String) -> Unit,
    onUpdateTask: (Task, String, String, String, String, String, String, Boolean, String, String) -> Unit,
    onClearMessage: () -> Unit,
    onDismissLevelUp: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var filter by remember { mutableStateOf(TaskFilter.All) }
    var showDatePicker by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }
    var dueDate by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var repeatRule by remember { mutableStateOf("") }
    var streakEligible by remember { mutableStateOf(true) }
    var assigneeLabel by remember { mutableStateOf("shared") }

    fun resetForm() {
        title = ""
        description = ""
        category = "General"
        dueDate = ""
        priority = "medium"
        repeatRule = ""
        streakEligible = true
        assigneeLabel = "shared"
    }

    fun openCreateDialog() {
        onClearMessage()
        resetForm()
        category = when (filter) {
            TaskFilter.Shopping -> "Shopping"
            TaskFilter.DateIdeas -> "Date ideas"
            else -> "General"
        }
        showCreateDialog = true
    }

    fun prefillForEdit(task: Task) {
        title = task.title
        description = task.description.orEmpty()
        category = task.category.ifBlank { "General" }
        dueDate = task.dueDate.orEmpty()
        priority = task.priority
        repeatRule = task.repeatRule.orEmpty()
        streakEligible = task.streakEligible
        assigneeLabel = task.assigneeLabel.takeIf { it == "his" || it == "hers" || it == "shared" } ?: "shared"
    }

    fun resolveAssigneeId(label: String): String {
        if (label == "shared") return ""
        val users = listOfNotNull(currentUser, partner)
        return users.firstOrNull { it.householdSide == label }?.userId
            ?: users.firstOrNull()?.userId
            ?: ""
    }

    val visibleTasks = when (filter) {
        TaskFilter.All -> uiState.tasks
        TaskFilter.Routines -> uiState.tasks.filter { !it.repeatRule.isNullOrBlank() && it.repeatRule != "none" }
        TaskFilter.Todo -> uiState.tasks.filter { it.status != "completed" }
        TaskFilter.Shopping -> uiState.tasks.filter { it.category.equals("shopping", ignoreCase = true) }
        TaskFilter.DateIdeas -> uiState.tasks.filter { it.category.equals("date ideas", ignoreCase = true) || it.category.equals("date", ignoreCase = true) }
        TaskFilter.Done -> uiState.tasks.filter { it.status == "completed" }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader(
            title = "Tasks",
            subtitle = "${uiState.tasks.size} total",
            actions = {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    IconButton(
                        onClick = { openCreateDialog() },
                        enabled = currentUser != null && !uiState.isCreatingTask
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "New task",
                            tint = Color.White
                        )
                    }
                }
            }
        )

        Column(modifier = Modifier.padding(16.dp)) {
            uiState.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            uiState.successMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilter.entries.forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label) }
                    )
                }
            }
        }

        when {
            uiState.isLoading && uiState.tasks.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            visibleTasks.isEmpty() -> {
                EmptyState(
                    icon = Icons.Filled.Rule,
                    title = "No tasks here",
                    subtitle = when (filter) {
                        TaskFilter.All -> "Tap the + button to create your first task."
                        TaskFilter.Routines -> "Recurring tasks will show up here. Set a repeat in the task form."
                        TaskFilter.Todo -> "All tasks are done. Nice work!"
                        TaskFilter.Shopping -> "Shopping tasks will show up here. Tap + to add one \u2014 category is pre-set for you."
                        TaskFilter.DateIdeas -> "Date idea tasks will show up here. Tap + to add one \u2014 category is pre-set for you."
                        TaskFilter.Done -> "Completed tasks will show up here."
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibleTasks, key = { it.taskId }) { task ->
                        val isCompleting = uiState.completingTaskId == task.taskId
                        val isDeleting = uiState.deletingTaskId == task.taskId
                        val isCompleted = task.status == "completed"
                        TaskRow(
                            task = task,
                            onClaim = { onClaimTask(task) },
                            trailing = {
                                if (!isCompleted) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onCompleteTask(task) },
                                            enabled = !isCompleting && !isDeleting
                                        ) {
                                            if (isCompleting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.onPrimary
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                if (isCompleting) "Completing..." else "Complete",
                                                modifier = Modifier.padding(start = 6.dp)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                editingTask = task
                                                prefillForEdit(task)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text("Edit", modifier = Modifier.padding(start = 6.dp))
                                        }
                                        IconButton(
                                            onClick = { taskToDelete = task },
                                            enabled = !isDeleting
                                        ) {
                                            if (isDeleting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Delete task",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        TaskFormDialog(
            title = "Create task",
            buttonText = if (uiState.isCreatingTask) "Creating..." else "Create",
            isLoading = uiState.isCreatingTask,
            titleFieldValue = title,
            descriptionFieldValue = description,
            categoryFieldValue = category,
            dueDateFieldValue = dueDate,
            priorityFieldValue = priority,
            repeatRuleFieldValue = repeatRule,
            streakEligibleFieldValue = streakEligible,
            assigneeLabelFieldValue = assigneeLabel,
            currentUser = currentUser,
            onTitleChange = { title = it; onClearMessage() },
            onDescriptionChange = { description = it },
            onCategoryChange = { category = it },
            onDueDateChange = { dueDate = it },
            onShowDatePicker = { showDatePicker = true },
            onPriorityChange = { priority = it },
            onRepeatRuleChange = { repeatRule = it },
            onStreakEligibleChange = { streakEligible = it },
            onAssigneeLabelChange = { assigneeLabel = it },
            onConfirm = {
                onCreateTask(title, description, category, priority, dueDate, repeatRule, streakEligible, resolveAssigneeId(assigneeLabel), assigneeLabel)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    editingTask?.let { task ->
        TaskFormDialog(
            title = "Edit task",
            buttonText = if (uiState.isUpdatingTask) "Saving..." else "Save",
            isLoading = uiState.isUpdatingTask,
            titleFieldValue = title,
            descriptionFieldValue = description,
            categoryFieldValue = category,
            dueDateFieldValue = dueDate,
            priorityFieldValue = priority,
            repeatRuleFieldValue = repeatRule,
            streakEligibleFieldValue = streakEligible,
            assigneeLabelFieldValue = assigneeLabel,
            currentUser = currentUser,
            onTitleChange = { title = it; onClearMessage() },
            onDescriptionChange = { description = it },
            onCategoryChange = { category = it },
            onDueDateChange = { dueDate = it },
            onShowDatePicker = { showDatePicker = true },
            onPriorityChange = { priority = it },
            onRepeatRuleChange = { repeatRule = it },
            onStreakEligibleChange = { streakEligible = it },
            onAssigneeLabelChange = { assigneeLabel = it },
            onConfirm = {
                onUpdateTask(task, title, description, category, priority, dueDate, repeatRule, streakEligible, resolveAssigneeId(assigneeLabel), assigneeLabel)
                editingTask = null
            },
            onDismiss = { editingTask = null }
        )
    }

    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete task") },
            text = { Text("Delete \"${task.title}\"? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    onDeleteTask(task)
                    taskToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { taskToDelete = null }) { Text("Cancel") }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    millisToDateString(datePickerState.selectedDateMillis).let {
                        if (it.isNotBlank()) dueDate = it
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LevelUpDialog(
        event = uiState.levelUpEvent,
        onDismiss = onDismissLevelUp
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskFormDialog(
    title: String,
    buttonText: String,
    isLoading: Boolean,
    titleFieldValue: String,
    descriptionFieldValue: String,
    categoryFieldValue: String,
    dueDateFieldValue: String,
    priorityFieldValue: String,
    repeatRuleFieldValue: String,
    streakEligibleFieldValue: Boolean,
    assigneeLabelFieldValue: String,
    currentUser: UserProfile?,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onDueDateChange: (String) -> Unit,
    onShowDatePicker: () -> Unit,
    onPriorityChange: (String) -> Unit,
    onRepeatRuleChange: (String) -> Unit,
    onStreakEligibleChange: (Boolean) -> Unit,
    onAssigneeLabelChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = titleFieldValue, onValueChange = onTitleChange,
                    label = { Text("Title") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = descriptionFieldValue, onValueChange = onDescriptionChange,
                    label = { Text("Description") }, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = categoryFieldValue, onValueChange = onCategoryChange,
                    label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDateFieldValue, onValueChange = onDueDateChange,
                    label = { Text("Due date (YYYY-MM-DD)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = onShowDatePicker) {
                            Icon(
                                imageVector = Icons.Filled.EditCalendar,
                                contentDescription = "Pick date"
                            )
                        }
                    }
                )
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach { option ->
                        val label = option.replaceFirstChar { it.uppercase() }
                        FilterChip(
                            selected = priorityFieldValue == option,
                            onClick = { onPriorityChange(option) },
                            label = { Text(label) }
                        )
                    }
                }
                Text("Repeat", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeatRules.forEach { (value, label) ->
                        FilterChip(
                            selected = repeatRuleFieldValue == value,
                            onClick = { onRepeatRuleChange(value) },
                            label = { Text(label) }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Counts toward streak", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Completing this task keeps your daily streak going",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = streakEligibleFieldValue,
                        onCheckedChange = onStreakEligibleChange
                    )
                }
                if (currentUser != null) {
                    Text("Who does it?", style = MaterialTheme.typography.labelLarge)
                    Text(
                        "Shared tasks can be claimed by either partner",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("his", "hers", "shared").forEach { label ->
                            val display = when (label) {
                                "his" -> "His"
                                "hers" -> "Hers"
                                else -> "Shared"
                            }
                            FilterChip(
                                selected = assigneeLabelFieldValue == label,
                                onClick = { onAssigneeLabelChange(label) },
                                label = { Text(display) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isLoading) { Text(buttonText) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
        }
    )
}
