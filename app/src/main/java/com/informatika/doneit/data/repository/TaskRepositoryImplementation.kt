package com.informatika.doneit.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.informatika.doneit.data.model.Task
import com.informatika.doneit.data.model.User
import com.informatika.doneit.util.FireStoreCollection
import com.informatika.doneit.util.UiState
import javax.inject.Inject

class TaskRepositoryImplementation @Inject constructor(
    val auth: FirebaseAuth,
    val database: FirebaseFirestore
) : TaskRepository {

    override fun addTask(task: Task, result: (UiState<Pair<Task, String>>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            result(UiState.Failure("User not authenticated"))
            return
        }

        val taskId = task.id.ifEmpty {
            database.collection(FireStoreCollection.TASKS)
                .document().id
        }

        task.id = taskId
        task.userId = currentUser.uid

        database.collection(FireStoreCollection.TASKS)
            .document(taskId)
            .set(task)
            .addOnSuccessListener {
                result(UiState.Success(Pair(task, "Task added successfully")))
            }
            .addOnFailureListener { e ->
                result(UiState.Failure(e.message ?: "Error adding task"))
            }
    }

    override fun updateTask(task: Task, result: (UiState<Pair<Task, String>>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            result(UiState.Failure("User not authenticated"))
            return
        }

        // Ensure the task belongs to the current user
        database.collection(FireStoreCollection.TASKS)
            .document(task.id)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val existingTask = documentSnapshot.toObject(Task::class.java)
                if (existingTask?.userId != currentUser.uid) {
                    result(UiState.Failure("Unauthorized to update this task"))
                    return@addOnSuccessListener
                }

                // Update the task using update() instead of set()
                database.collection(FireStoreCollection.TASKS)
                    .document(task.id)
                    .update(
                        "title", task.title,
                        "description", task.description,
                        "priority", task.priority,
                        "dueDate", task.dueDate,
                        "location", task.location
                    )
                    .addOnSuccessListener {
                        result(UiState.Success(Pair(task, "Task updated successfully")))
                    }
                    .addOnFailureListener { e ->
                        result(UiState.Failure(e.message ?: "Error updating task"))
                    }
            }
            .addOnFailureListener { e ->
                result(UiState.Failure(e.message ?: "Error checking task ownership"))
            }
    }


    override fun deleteTask(task: Task, result: (UiState<Pair<Task,String>>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            result(UiState.Failure("User not authenticated"))
            return
        }

        // Ensure the task belongs to the current user
        database.collection(FireStoreCollection.TASKS)
            .document(task.id)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val existingTask = documentSnapshot.toObject(Task::class.java)
                if (existingTask?.userId != currentUser.uid) {
                    result(UiState.Failure("Unauthorized to delete this task"))
                    return@addOnSuccessListener
                }

                // Delete the task
                database.collection(FireStoreCollection.TASKS)
                    .document(task.id)
                    .delete()
                    .addOnSuccessListener {
                        result(UiState.Success(Pair(task, "Task deleted successfully!")))
                    }
                    .addOnFailureListener {
                        result(UiState.Failure(it.message))
                    }
            }
            .addOnFailureListener { e ->
                result(UiState.Failure(e.message ?: "Error checking task ownership"))
            }
    }

    override fun getTasks(user: User?, result: (UiState<List<Task>>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            result(UiState.Failure("User not authenticated"))
            return
        }

        database.collection(FireStoreCollection.TASKS)
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener {
                val tasks = it.toObjects(Task::class.java)
                result.invoke(UiState.Success(tasks.filterNotNull()))
            }
            .addOnFailureListener {
                result.invoke(UiState.Failure(it.message))
            }
    }

    override fun getTask(id: String, result: (UiState<Pair<Task,String>>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            result(UiState.Failure("User not authenticated"))
            return
        }

        database.collection(FireStoreCollection.TASKS)
            .document(id)
            .get()
            .addOnSuccessListener { documentSnapshot ->
                val task = documentSnapshot.toObject(Task::class.java)
                if (task != null) {
                    // Check if the task belongs to the current user
                    if (task.userId == currentUser.uid) {
                        result.invoke(UiState.Success(Pair(task, "Task retrieved successfully!")))
                    } else {
                        result.invoke(UiState.Failure("Unauthorized to access this task"))
                    }
                } else {
                    result.invoke(UiState.Failure("Task not found!"))
                }
            }
            .addOnFailureListener {
                result.invoke(UiState.Failure(it.message))
            }
    }

    override fun storeTasks(tasks: List<Task>, result: (UiState<String>) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            result(UiState.Failure("User not authenticated"))
            return
        }

        // Filter tasks to only include tasks belonging to the current user
        val userTasks = tasks.filter { it.userId == currentUser.uid }

        // Batch write operations
        val batch = database.batch()

        userTasks.forEach { task ->
            val docRef = database.collection(FireStoreCollection.TASKS).document(task.id)
            batch.set(docRef, task)
        }

        batch.commit()
            .addOnSuccessListener {
                result.invoke(UiState.Success("Tasks stored successfully!"))
            }
            .addOnFailureListener {
                result.invoke(UiState.Failure(it.message))
            }
    }
}