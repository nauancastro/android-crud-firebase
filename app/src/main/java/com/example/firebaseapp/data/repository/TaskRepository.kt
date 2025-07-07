package com.example.firebaseapp.data.repository

import android.util.Log // Importar Log
import com.example.firebaseapp.data.model.Task
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class TaskRepository(private val firestore: FirebaseFirestore) {

    private val tasksCollection = firestore.collection("tasks")
    private val TAG = "TaskRepository" // Tag para logs

    suspend fun addTask(taskData: Task) { // Renomeado para taskData para clareza
        Log.d(TAG, "Repositório - Dados da Tarefa recebidos para adicionar: $taskData")
        try {
            // Crie um mapa contendo apenas os campos que você quer DENTRO do documento
            val taskMap = hashMapOf(
                "title" to taskData.title,
                "description" to taskData.description,
                "isCompleted" to taskData.isCompleted // Garante que isCompleted (com valor padrão false) seja enviado
                // NÃO inclua "id" aqui, pois o Firestore gerará o ID do documento
            )

            val documentReference = tasksCollection.add(taskMap).await()
            Log.d(TAG, "Tarefa adicionada com ID do Firestore: ${documentReference.id}. Enviado: $taskMap")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao adicionar tarefa", e)
            throw e
        }
    }

    suspend fun getTasks(): List<Task> {
        Log.d(TAG, "Tentando buscar tarefas")
        try {
            val snapshot = tasksCollection.get().await()
            val tasks = snapshot.documents.map { doc ->
                // pega cada campo explicitamente, com fallback
                val title       = doc.getString("title")       ?: ""
                val description = doc.getString("description") ?: ""
                val done        = doc.getBoolean("isCompleted") ?: false
                Task(
                    id = doc.id,
                    title = title,
                    description = description,
                    isCompleted = done
                )
            }
            Log.d(TAG, "Tarefas buscadas: ${tasks.size}. Exemplo de primeira tarefa (se houver): ${tasks.firstOrNull()}")
            return tasks
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar tarefas", e)
            throw e
        }
    }

    suspend fun updateTask(task: Task) {
        if (task.id.isBlank()) {
            Log.e(TAG, "ID da tarefa está em branco. Não é possível atualizar.")
            throw IllegalArgumentException("Task ID cannot be blank for update.")
        }
        Log.d(TAG, "Tentando atualizar tarefa completa ID: ${task.id}")
        try {
            val taskMap = mapOf(
                "title"       to task.title,
                "description" to task.description,
                "isCompleted" to task.isCompleted
            )
            tasksCollection.document(task.id).set(taskMap).await()
            Log.d(TAG, "Tarefa completa atualizada ID: ${task.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar tarefa completa ID: ${task.id}", e)
            throw e
        }
    }

    suspend fun updateTaskCompletion(taskId: String, isCompleted: Boolean) {
        if (taskId.isBlank()) {
            Log.e(TAG, "ID da tarefa está em branco. Não é possível atualizar conclusão.")
            throw IllegalArgumentException("Task ID cannot be blank for update.")
        }
        Log.d(TAG, "Tentando atualizar 'isCompleted' para: $isCompleted, ID: $taskId")
        try {
            // Atualiza APENAS o campo "isCompleted"
            tasksCollection.document(taskId).update("isCompleted", isCompleted).await()
            Log.d(TAG, "'isCompleted' atualizado para: $isCompleted, ID: $taskId SUCESSO")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar 'isCompleted' para: $isCompleted, ID: $taskId", e)
            throw e
        }
    }

    suspend fun deleteTask(taskId: String) {
        if (taskId.isBlank()) {
            Log.e(TAG, "ID da tarefa está em branco. Não é possível deletar.")
            throw IllegalArgumentException("Task ID cannot be blank for delete.")
        }
        Log.d(TAG, "Tentando deletar tarefa ID: $taskId")
        try {
            tasksCollection.document(taskId).delete().await()
            Log.d(TAG, "Tarefa deletada ID: $taskId")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao deletar tarefa ID: $taskId", e)
            throw e
        }
    }
}
