package com.example.geotask.pages

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodoList(
    navController: NavController,
    todoViewModel: TodoViewModel = viewModel()
) {
    val todoList by todoViewModel.todoList.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<TodoItem?>(null) }

    val dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM)
    val currentDate = dateFormatter.format(Date())
    val currentTime = remember { mutableStateOf(getCurrentTime()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = currentDate, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "To-Do List",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(text = currentTime.value, style = MaterialTheme.typography.bodyMedium)
        }

        Box(modifier = Modifier.fillMaxSize()) {
            if (todoList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No To-Do Items",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(todoList) { item ->
                        TodoItemCard(
                            item = item,
                            onDeleteItem = { todoViewModel.deleteTodoItem(item.id) },
                            onEditItem = {
                                editingItem = item
                                showDialog = true
                            }
                        )
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    editingItem = null
                    showDialog = true
                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Text("+ ADD")
            }

            if (showDialog) {
                AddTodoDialog(
                    onDismiss = { showDialog = false },
                    onConfirm = { title, category, date ->
                        if (editingItem == null) {
                            todoViewModel.addTodoItem(title, category, date)
                        } else {
                            todoViewModel.editTodoItem(
                                editingItem!!.id,
                                title,
                                category,
                                date
                            )
                        }
                        showDialog = false
                    },
                    existingItem = editingItem
                )
            }
        }
    }
}

fun getCurrentTime(): String {
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return timeFormat.format(Date())
}

@Composable
fun TodoItemCard(
    item: TodoItem,
    onDeleteItem: () -> Unit,
    onEditItem: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = item.title, style = MaterialTheme.typography.bodyLarge)
            Text(text = "Category: ${item.category}", color = Color.Gray)
            Text(text = "Deadline: ${DateFormat.getDateInstance().format(item.deadline)}", color = Color.Gray)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEditItem) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFFFFC107)
                    )
                }

                IconButton(onClick = onDeleteItem) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTodoDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Date) -> Unit,
    existingItem: TodoItem? = null
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf(existingItem?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(existingItem?.category ?: "Work") }
    var selectedDate by remember { mutableStateOf(Date(existingItem?.deadline ?: Date().time)) }

    val topPriorityCategories = listOf("Work", "Medicine", "Grocery", "Food")
    val lowerPriorityCategories = listOf("Shopping", "Touring", "Entertainment", "Others")
    val allCategories = topPriorityCategories + lowerPriorityCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingItem == null) "Add Task" else "Edit Task") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("To-Do Title") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown Arrow")
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allCategories.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        category,
                                        fontWeight = if (category in topPriorityCategories) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val datePickerDialog = createDatePickerDialog(context) { date ->
                    selectedDate = date
                }
                Button(onClick = { datePickerDialog.show() }) {
                    Text("Select Deadline")
                }
                Text("Deadline: ${DateFormat.getDateInstance().format(selectedDate)}")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, selectedCategory, selectedDate)
                        onDismiss()
                    }
                }
            ) {
                Text(if (existingItem == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun createDatePickerDialog(context: Context, onDateSelected: (Date) -> Unit): DatePickerDialog {
    val calendar = Calendar.getInstance()
    return DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            onDateSelected(calendar.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}
