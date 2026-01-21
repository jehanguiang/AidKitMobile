package com.jehan.aidkitmobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.ui.draw.clip
import com.jehan.aidkitmobile.interfaces.AskRequest
import androidx.compose.foundation.background
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jehan.aidkitmobile.models.Medication
import com.jehan.aidkitmobile.network.RetrofitClient
import com.jehan.aidkitmobile.ui.theme.AidKitMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AidKitMobileTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Medications", "Ask AI")
    var chatMessages by remember { mutableStateOf<List<ChatMessage>>(emptyList()) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(title = { Text("Aid Kit") })
                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> MedicationScreen()
                1 -> ChatScreen(
                    messages = chatMessages,
                    onMessagesChange = { chatMessages = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen() {
    var medications by remember { mutableStateOf<List<Medication>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredMedications = medications.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    LaunchedEffect(Unit) {
        try {
            val response = RetrofitClient.medicationApi.getAll()
            if (response.isSuccessful) {
                medications = response.body() ?: emptyList()
            } else {
                errorMessage = "Error: ${response.code()}"
            }
        } catch (e: Exception) {
            errorMessage = "Failed to load: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    if (showAddDialog) {
        AddMedicationDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newMedication ->
                scope.launch {
                    try {
                        val response = RetrofitClient.medicationApi.create(newMedication)
                        if (response.isSuccessful) {
                            response.body()?.let { medications = medications + it }
                        } else {
                            android.util.Log.e("MedicationApi", "Create failed: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MedicationApi", "Create error: ${e.message}")
                    }
                    showAddDialog = false
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add medication")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by name...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    filteredMedications.isEmpty() -> {
                        Text(
                            text = if (searchQuery.isEmpty()) "No medications found" else "No results for \"$searchQuery\"",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    else -> {
                        MedicationList(
                            medications = filteredMedications,
                            onDelete = { medication ->
                                scope.launch {
                                    try {
                                        val response = RetrofitClient.medicationApi.delete(medication.id)
                                        if (response.isSuccessful) {
                                            medications = medications.filter { it.id != medication.id }
                                        } else {
                                            android.util.Log.e("MedicationApi", "Delete failed: ${response.code()}")
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("MedicationApi", "Delete error: ${e.message}")
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationList(
    medications: List<Medication>,
    onDelete: (Medication) -> Unit
) {
    var medicationToDelete by remember { mutableStateOf<Medication?>(null) }

    if (medicationToDelete != null) {
        AlertDialog(
            onDismissRequest = { medicationToDelete = null },
            title = { Text("Delete Medication") },
            text = { Text("Are you sure you want to delete \"${medicationToDelete!!.name}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(medicationToDelete!!)
                        medicationToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { medicationToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxHeight().fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(medications, key = { it.id }) { medication ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    if (value == SwipeToDismissBoxValue.EndToStart) {
                        medicationToDelete = medication
                        false
                    } else {
                        false
                    }
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                },
                enableDismissFromStartToEnd = false,
                enableDismissFromEndToStart = true
            ) {
                MedicationCard(medication = medication)
            }
        }
    }
}

@Composable
fun AddMedicationDialog(
    onDismiss: () -> Unit,
    onAdd: (Medication) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var purpose by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var sideEffects by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Medication") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Purpose") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    label = { Text("Expiry Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sideEffects,
                    onValueChange = { sideEffects = it },
                    label = { Text("Side Effects (comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && purpose.isNotBlank()) {
                        onAdd(
                            Medication(
                                name = name,
                                purpose = purpose,
                                expiryDate = expiryDate.takeIf { it.isNotBlank() },
                                sideEffects = sideEffects.takeIf { it.isNotBlank() }
                                    ?.split(",")?.map { it.trim() }
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && purpose.isNotBlank()
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

@Composable
fun MedicationCard(medication: Medication) {
    Card(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = medication.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = medication.purpose,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            medication.expiryDate?.let {
                Text(
                    text = "Expires: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            medication.sideEffects?.takeIf { it.isNotEmpty() }?.let { effects ->
                Text(
                    text = "Side effects: ${effects.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

data class ChatMessage(
    val content: String,
    val isUser: Boolean
)

@Composable
fun ChatScreen(
    messages: List<ChatMessage>,
    onMessagesChange: (List<ChatMessage>) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Ask questions about your medications",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            items(messages) { message ->
                ChatBubble(message = message)
            }
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about medications...") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isLoading) {
                        val question = inputText
                        inputText = ""
                        onMessagesChange(messages + ChatMessage(question, isUser = true))
                        isLoading = true

                        scope.launch {
                            try {
                                val response = RetrofitClient.aiApi.askAboutMedication(AskRequest(question))
                                if (response.isSuccessful) {
                                    val answer = response.body() ?: "No response"
                                    onMessagesChange(messages + ChatMessage(question, isUser = true) + ChatMessage(answer, isUser = false))
                                } else {
                                    onMessagesChange(messages + ChatMessage(question, isUser = true) + ChatMessage("Error: ${response.code()}", isUser = false))
                                }
                            } catch (e: Exception) {
                                onMessagesChange(messages + ChatMessage(question, isUser = true) + ChatMessage("Error: ${e.message}", isUser = false))
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = inputText.isNotBlank() && !isLoading
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank() && !isLoading)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            color = if (message.isUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser)
                    MaterialTheme.colorScheme.onPrimary
                else
                    MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}