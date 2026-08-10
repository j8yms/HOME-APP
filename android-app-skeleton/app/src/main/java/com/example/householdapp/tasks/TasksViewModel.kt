package com.example.householdapp.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdapp.core.model.Task
import com.example.householdapp.core.network.CreateTaskRequest
import com.example.householdapp.core.network.UpdateTaskRequest
import com.example.householdapp.core.repository.TaskRepository
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.LevelUpEvent
import com.example.householdapp.core.ui.components.detectLevelUp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TasksUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isCreatingTask: Boolean = false,
    val isUpdatingTask: Boolean = false,
    val deletingTaskId: String? = null,
    val completingTaskId: String? = null,
    val claimingTaskId: String? = null,
    val tasks: List<Task> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val levelUpEvent: LevelUpEvent? = null
)

class TasksViewModel(
    private val taskRepository: TaskRepository = TaskRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(TasksUiState())
    val uiState: StateFlow<TasksUiState> = _uiState.asStateFlow()

    init {
        loadTasks()
    }

    fun loadTasks(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = !forceRefresh && it.tasks.isEmpty(),
                    isRefreshing = forceRefresh,
                    errorMessage = null
                )
            }

            runCatching { taskRepository.listTasks() }
                .onSuccess { response ->
                    val tasks = if (response.success) {
                        response.data?.tasks.orEmpty()
                    } else {
                        emptyList()
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            tasks = tasks.sortedWith(
                                compareBy<Task> { it.status == "completed" }
                                    .thenBy { it.dueDate }
                                    .thenBy { it.title }
                            ),
                            errorMessage = if (response.success) null else response.error?.message
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = throwable.message ?: "Unable to load tasks."
                        )
                    }
                }
        }
    }

    fun completeTask(task: Task) {
        val userId = SessionManager.sessionState.value.user?.userId
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign in first to complete tasks.") }
            return
        }

        viewModelScope.launch {
            val previousLevel = SessionManager.sessionState.value.user?.level

            _uiState.update {
                it.copy(
                    completingTaskId = task.taskId,
                    errorMessage = null,
                    successMessage = null
                )
            }

            runCatching { taskRepository.completeTask(task.taskId, userId) }
                .onSuccess { response ->
                    val data = response.data
                    if (!response.success || data == null) {
                        _uiState.update {
                            it.copy(
                                completingTaskId = null,
                                errorMessage = response.error?.message ?: "Unable to complete task."
                            )
                        }
                        return@launch
                    }

                    SessionManager.updateUser(data.user)
                    _uiState.update { state ->
                        val updatedTasks = state.tasks.map { existing ->
                            if (existing.taskId == data.task.taskId) data.task else existing
                        }
                        state.copy(
                            completingTaskId = null,
                            tasks = (updatedTasks + listOfNotNull(data.recurringTask)).sortedWith(
                                compareBy<Task> { it.status == "completed" }
                                    .thenBy { it.dueDate }
                                    .thenBy { it.title }
                            ),
                            successMessage = if (data.recurringTask != null) {
                                "Completed ${data.task.title}. Next occurrence created."
                            } else {
                                "Completed ${data.task.title}."
                            },
                            errorMessage = null,
                            levelUpEvent = detectLevelUp(previousLevel, data.user)?.let { LevelUpEvent(it) }
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            completingTaskId = null,
                            errorMessage = throwable.message ?: "Unable to complete task."
                        )
                    }
                }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    deletingTaskId = task.taskId,
                    errorMessage = null,
                    successMessage = null
                )
            }

            runCatching { taskRepository.deleteTask(task.taskId, task.version) }
                .onSuccess { response ->
                    val deletedTask = response.data?.task
                    if (!response.success || deletedTask == null) {
                        _uiState.update {
                            it.copy(
                                deletingTaskId = null,
                                errorMessage = response.error?.message ?: "Unable to delete task."
                            )
                        }
                        return@launch
                    }

                    _uiState.update { state ->
                        state.copy(
                            deletingTaskId = null,
                            tasks = state.tasks.filter { it.taskId != deletedTask.taskId },
                            successMessage = "Deleted ${deletedTask.title}.",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            deletingTaskId = null,
                            errorMessage = throwable.message ?: "Unable to delete task."
                        )
                    }
                }
        }
    }

    fun claimTask(task: Task) {
        val userId = SessionManager.sessionState.value.user?.userId
        if (userId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign in first to claim tasks.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    claimingTaskId = task.taskId,
                    errorMessage = null,
                    successMessage = null
                )
            }

            runCatching { taskRepository.claimTask(task.taskId, userId) }
                .onSuccess { response ->
                    val claimedTask = response.data?.task
                    if (!response.success || claimedTask == null) {
                        _uiState.update {
                            it.copy(
                                claimingTaskId = null,
                                errorMessage = response.error?.message ?: "Unable to claim task."
                            )
                        }
                        return@launch
                    }

                    _uiState.update { state ->
                        state.copy(
                            claimingTaskId = null,
                            tasks = state.tasks.map { existing ->
                                if (existing.taskId == claimedTask.taskId) claimedTask else existing
                            },
                            successMessage = "Claimed ${claimedTask.title}.",
                            errorMessage = null
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            claimingTaskId = null,
                            errorMessage = throwable.message ?: "Unable to claim task."
                        )
                    }
                }
        }
    }

    fun updateTask(
        task: Task,
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDate: String,
        repeatRule: String,
        streakEligible: Boolean,
        assignedToUserId: String,
        assigneeLabel: String,
        onSuccess: () -> Unit = {}
    ) {
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a task title.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingTask = true, errorMessage = null, successMessage = null) }

            val request = UpdateTaskRequest(
                taskId = task.taskId,
                title = title.trim(),
                description = description.trim(),
                category = category.trim().ifBlank { "General" },
                assignedToUserId = assignedToUserId,
                assigneeLabel = assigneeLabel,
                priority = priority,
                dueDate = dueDate.trim(),
                repeatRule = repeatRule,
                streakEligible = streakEligible,
                version = task.version
            )

            runCatching { taskRepository.updateTask(request) }
                .onSuccess { response ->
                    val updatedTask = response.data?.task
                    if (!response.success || updatedTask == null) {
                        _uiState.update {
                            it.copy(
                                isUpdatingTask = false,
                                errorMessage = response.error?.message ?: "Unable to update task."
                            )
                        }
                        return@launch
                    }

                    _uiState.update { state ->
                        state.copy(
                            isUpdatingTask = false,
                            tasks = state.tasks.map { existing ->
                                if (existing.taskId == updatedTask.taskId) updatedTask else existing
                            }.sortedWith(
                                compareBy<Task> { it.status == "completed" }
                                    .thenBy { it.dueDate }
                                    .thenBy { it.title }
                            ),
                            successMessage = "Updated ${updatedTask.title}.",
                            errorMessage = null
                        )
                    }
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isUpdatingTask = false,
                            errorMessage = throwable.message ?: "Unable to update task."
                        )
                    }
                }
        }
    }

    fun createTask(
        title: String,
        description: String,
        category: String,
        priority: String,
        dueDate: String,
        repeatRule: String,
        streakEligible: Boolean,
        assignedToUserId: String,
        assigneeLabel: String,
        onSuccess: () -> Unit = {}
    ) {
        val currentUser = SessionManager.sessionState.value.user
        val creatorId = currentUser?.userId
        if (creatorId.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign in first to create tasks.") }
            return
        }

        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Enter a task title.") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCreatingTask = true,
                    errorMessage = null,
                    successMessage = null
                )
            }

            val request = CreateTaskRequest(
                title = title.trim(),
                description = description.trim(),
                category = category.trim().ifBlank { "General" },
                assignedToUserId = assignedToUserId,
                assigneeLabel = assigneeLabel,
                createdByUserId = creatorId,
                priority = priority,
                dueDate = dueDate.trim(),
                repeatRule = repeatRule,
                streakEligible = streakEligible
            )

            runCatching { taskRepository.createTask(request) }
                .onSuccess { response ->
                    val task = response.data?.task
                    if (!response.success || task == null) {
                        _uiState.update {
                            it.copy(
                                isCreatingTask = false,
                                errorMessage = response.error?.message ?: "Unable to create task."
                            )
                        }
                        return@launch
                    }

                    _uiState.update { state ->
                        state.copy(
                            isCreatingTask = false,
                            tasks = (state.tasks + task).sortedWith(
                                compareBy<Task> { it.status == "completed" }
                                    .thenBy { it.dueDate }
                                    .thenBy { it.title }
                            ),
                            successMessage = "Created ${task.title}.",
                            errorMessage = null
                        )
                    }
                    onSuccess()
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isCreatingTask = false,
                            errorMessage = throwable.message ?: "Unable to create task."
                        )
                    }
                }
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun consumeLevelUp() {
        _uiState.update { it.copy(levelUpEvent = null) }
    }
}
