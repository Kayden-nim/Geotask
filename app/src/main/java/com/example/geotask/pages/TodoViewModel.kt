package com.example.geotask.pages

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class TodoViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _todoList = MutableStateFlow<List<TodoItem>>(emptyList())
    val todoList: StateFlow<List<TodoItem>> = _todoList.asStateFlow()

    var showDialog = mutableStateOf(false)
        private set

    var newTitle = mutableStateOf("")
        private set

    val categories = listOf("Work", "Medicine", "Grocery", "Food", "Shopping", "Touring", "Other", "All")

    var selectedCategory = mutableStateOf("All")
        private set

    var selectedDate = mutableStateOf(Date()) // Deadline date
        private set

    var selectedSortOption = mutableStateOf(SortOption.BY_PRIORITY)
        private set

    private var editingItemId: String? = null

    init {
        fetchTodos()
    }

    private fun getUserTodosRef() = auth.currentUser?.let { user ->
        firestore.collection("users").document(user.uid).collection("todos")
    }

    fun showAddTodoDialog() {
        showDialog.value = true
    }

    fun hideAddTodoDialog() {
        showDialog.value = false
        newTitle.value = ""
        selectedCategory.value = categories.first()
        selectedDate.value = Date()
        editingItemId = null
    }

    fun updateNewTitle(title: String) {
        newTitle.value = title
    }

    fun updateSelectedCategory(category: String) {
        selectedCategory.value = category
        fetchTodos() // Refresh list when category changes
    }

    fun updateSelectedDate(date: Date) {
        selectedDate.value = date
    }

    fun updateSortOption(sortOption: SortOption) {
        selectedSortOption.value = sortOption
        fetchTodos() // Refresh list when sorting changes
    }

    fun addTodoItem(title: String, category: String, date: Date) {
        val userTodosRef = getUserTodosRef() ?: return
        if (title.isEmpty()) return

        val newItem = TodoItem(
            id = UUID.randomUUID().toString(),
            title = title,
            category = category,
            createdAt = Date().time,
            deadline = date.time
        )

        // Optimistically update UI
        _todoList.value = _todoList.value + newItem

        viewModelScope.launch {
            try {
                userTodosRef.document(newItem.id).set(newItem).await()
                Log.d("TodoViewModel", "Todo item added successfully!")
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Failed to add todo: ${e.message}")
                // Rollback UI change in case of failure
                _todoList.value = _todoList.value - newItem
            }
        }

        hideAddTodoDialog()
    }


    fun editTodoItem(id: String, title: String, category: String, date: Date) {
        val userTodosRef = getUserTodosRef() ?: return

        viewModelScope.launch {
            try {
                userTodosRef.document(id).update(
                    mapOf(
                        "title" to title,
                        "category" to category,
                        "deadline" to date.time
                    )
                ).await()
                Log.d("TodoViewModel", "Todo item updated successfully!")
                fetchTodos() // Refresh UI after update
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Failed to update todo: ${e.message}")
            }
        }

        hideAddTodoDialog()
    }

    fun deleteTodoItem(id: String) {
        val userTodosRef = getUserTodosRef() ?: return

        viewModelScope.launch {
            try {
                userTodosRef.document(id).delete().await()
                Log.d("TodoViewModel", "Todo item deleted successfully!")
                fetchTodos() // Refresh UI after deletion
            } catch (e: Exception) {
                Log.e("TodoViewModel", "Failed to delete todo: ${e.message}")
            }
        }
    }

    fun startEdit(item: TodoItem) {
        newTitle.value = item.title
        selectedCategory.value = if (categories.contains(item.category)) item.category else categories.first()
        selectedDate.value = Date(item.deadline)
        editingItemId = item.id
        showDialog.value = true
    }

    private fun fetchTodos() {
        val userTodosRef = getUserTodosRef() ?: return

        viewModelScope.launch {
            try {
                val snapshot = userTodosRef.orderBy("deadline").get().await()
                val newList = snapshot.documents.mapNotNull { doc -> doc.toObject(TodoItem::class.java) }

                // Apply category filter
                val filteredList = if (selectedCategory.value == "All") {
                    newList
                } else {
                    newList.filter { it.category == selectedCategory.value }
                }

                // Apply sorting based on priority and deadline
                _todoList.value = when (selectedSortOption.value) {
                    SortOption.BY_PRIORITY -> filteredList.sortedWith(
                        compareBy<TodoItem> { getCategoryPriority(it.category) }
                            .thenBy { it.deadline }
                    )
                    SortOption.BY_DEADLINE -> filteredList.sortedBy { it.deadline }
                    SortOption.BY_CREATED_AT -> filteredList.sortedBy { it.createdAt }
                    SortOption.BY_CATEGORY -> filteredList.sortedBy { it.category }
                }

            } catch (e: Exception) {
                Log.e("TodoViewModel", "Failed to fetch todos: ${e.message}")
            }
        }
    }

    private fun getCategoryPriority(category: String): Int {
        return when (category) {
            "Work", "Medicine", "Grocery", "Food" -> 1  // Highest priority
            "Shopping", "Touring" -> 2  // Lower priority
            else -> 3  // Default priority
        }
    }
}

enum class SortOption {
    BY_PRIORITY,
    BY_DEADLINE,
    BY_CREATED_AT,
    BY_CATEGORY
}

data class TodoItem(
    val id: String = "",
    val title: String = "",
    val category: String = "",
    val createdAt: Long = 0L,
    val deadline: Long = 0L
) {
    constructor() : this("", "", "", 0L, 0L) // Firestore needs an empty constructor
}
