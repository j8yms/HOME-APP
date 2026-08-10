package com.example.householdapp.reference

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.householdapp.core.model.ReferenceEntry
import com.example.householdapp.core.session.SessionManager
import com.example.householdapp.core.ui.components.GradientHeader
import com.example.householdapp.core.ui.formatTimestamp

private data class DirectoryMeta(val title: String, val subtitle: String, val icon: ImageVector)

private fun directoryMeta(category: String): DirectoryMeta {
    return when (category.lowercase()) {
        "vault" -> DirectoryMeta(
            "Shared Digital Vault",
            "Passwords, accounts, documents & codes",
            Icons.Filled.Lock
        )
        "health" -> DirectoryMeta(
            "Family Health Hub",
            "Medications, allergies, doctors & records",
            Icons.Filled.Favorite
        )
        else -> DirectoryMeta(
            "Home Maintenance",
            "Plumbers, electricians, warranties & repairs",
            Icons.Filled.Build
        )
    }
}

@Composable
fun ReferenceScreen(
    category: String,
    referenceViewModel: ReferenceViewModel = viewModel()
) {
    val sessionState by SessionManager.sessionState.collectAsState()
    val uiState by referenceViewModel.uiState.collectAsState()
    val user = sessionState.user
    val meta = directoryMeta(category)
    var showCreateDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<ReferenceEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<ReferenceEntry?>(null) }

    LaunchedEffect(category) {
        referenceViewModel.loadEntries(category)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientHeader(
            title = meta.title,
            subtitle = "${meta.subtitle} \u00b7 Shared with your household",
            actions = {
                if (user != null) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        IconButton(
                            onClick = {
                                referenceViewModel.clearMessage()
                                showCreateDialog = true
                            },
                            enabled = !uiState.isSubmitting
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add entry",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
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
        }

        when {
            uiState.isLoading && uiState.entries.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.entries.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = meta.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        "No entries yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        "Tap + to add the first entry to your ${meta.title.lowercase().replaceFirstChar { it.uppercase() }}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, start = 32.dp, end = 32.dp)
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.entries, key = { it.entryId }) { entry ->
                        val isDeleting = uiState.deletingEntryId == entry.entryId
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (entry.updatedAt.isNotBlank()) {
                                            Text(
                                                text = "Updated ${formatTimestamp(entry.updatedAt)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingEntry = entry
                                                referenceViewModel.clearMessage()
                                            },
                                            enabled = !isDeleting && !uiState.isSubmitting
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "Edit entry",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { entryToDelete = entry },
                                            enabled = !isDeleting && !uiState.isSubmitting
                                        ) {
                                            if (isDeleting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "Delete entry",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (entry.content.isNotBlank()) {
                                    Text(
                                        text = entry.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var entryTitle by remember { mutableStateOf("") }
        var entryContent by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { if (!uiState.isSubmitting) showCreateDialog = false },
            title = { Text("Add to ${meta.title}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = entryTitle,
                        onValueChange = { entryTitle = it; referenceViewModel.clearMessage() },
                        label = { Text("Title") },
                        singleLine = true,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = entryContent,
                        onValueChange = { entryContent = it },
                        label = { Text("Details") },
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    uiState.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        referenceViewModel.createEntry(category, entryTitle, entryContent) {
                            showCreateDialog = false
                        }
                    },
                    enabled = !uiState.isSubmitting && entryTitle.isNotBlank()
                ) {
                    Text(if (uiState.isSubmitting) "Adding..." else "Add")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCreateDialog = false },
                    enabled = !uiState.isSubmitting
                ) { Text("Cancel") }
            }
        )
    }

    editingEntry?.let { entry ->
        var entryTitle by remember { mutableStateOf(entry.title) }
        var entryContent by remember { mutableStateOf(entry.content) }
        AlertDialog(
            onDismissRequest = { if (!uiState.isSubmitting) editingEntry = null },
            title = { Text("Edit entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = entryTitle,
                        onValueChange = { entryTitle = it; referenceViewModel.clearMessage() },
                        label = { Text("Title") },
                        singleLine = true,
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = entryContent,
                        onValueChange = { entryContent = it },
                        label = { Text("Details") },
                        enabled = !uiState.isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    )
                    uiState.errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        referenceViewModel.updateEntry(entry.entryId, entryTitle, entryContent) {
                            editingEntry = null
                        }
                    },
                    enabled = !uiState.isSubmitting && entryTitle.isNotBlank()
                ) {
                    Text(if (uiState.isSubmitting) "Saving..." else "Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { editingEntry = null },
                    enabled = !uiState.isSubmitting
                ) { Text("Cancel") }
            }
        )
    }

    entryToDelete?.let { entry ->
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete entry") },
            text = { Text("Delete \"${entry.title}\" from your shared directory? This cannot be undone.") },
            confirmButton = {
                Button(onClick = {
                    referenceViewModel.deleteEntry(entry.entryId)
                    entryToDelete = null
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { entryToDelete = null }) { Text("Cancel") }
            }
        )
    }
}
