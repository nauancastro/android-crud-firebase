package com.example.firebaseapp.ui.viewmodel

import android.util.Log // Importar Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.firebaseapp.data.model.Task
import com.example.firebaseapp.data.repository.TaskRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val crashlytics: FirebaseCrashlytics
) : ViewModel() {

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val TAG = "TaskViewModel" // Tag para logs

    init {
        Log.d(TAG, "ViewModel inicializado. Carregando tarefas...")
        loadTasks()
    }

    fun loadTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d(TAG, "Iniciando loadTasks")
            try {
                _tasks.value = repository.getTasks()
                Log.d(TAG, "loadTasks concluído. Número de tarefas: ${_tasks.value.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Erro em loadTasks", e)
                _error.value = "Erro ao carregar tarefas: ${e.message}"
                crashlytics.recordException(e)
            } finally {
                _isLoading.value = false
                Log.d(TAG, "loadTasks finalizado (finally)")
            }
        }
    }

    fun addTask(title: String, description: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            // isCompleted será false por padrão devido à data class
            val taskToSend = Task(title = title, description = description)
            Log.d(TAG, "ViewModel - Objeto Task a ser enviado para addTask no repositório: $taskToSend")
            try {
                repository.addTask(taskToSend)
                loadTasks() // Essencial para buscar a nova tarefa com seu ID real do Firestore
            } catch (e: Exception) {
                _error.value = "Erro ao adicionar tarefa: ${e.message}"
                crashlytics.recordException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateTask(task: Task) { // Para edição completa
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d(TAG, "Iniciando updateTask (completa): ${task.id}")
            try {
                repository.updateTask(task)
                Log.d(TAG, "updateTask (completa) SUCESSO no repositório: ${task.id}. Atualizando UI e recarregando...")
                // Atualiza a lista localmente
                _tasks.update { currentTasks ->
                    currentTasks.map { if (it.id == task.id) task else it }
                }
                // loadTasks() // Pode ser desnecessário se a atualização local for suficiente
            } catch (e: Exception) {
                Log.e(TAG, "Erro em updateTask (completa): ${task.id}", e)
                _error.value = "Erro ao atualizar tarefa: ${e.message}"
                crashlytics.recordException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            _error.value = null
            val newCompletionState = !task.isCompleted
            Log.d(TAG, "Iniciando toggleTaskCompletion para ID: ${task.id}, novo estado: $newCompletionState")

            // Atualização otimista da UI
            val originalTasks = _tasks.value
            _tasks.update { currentTasks ->
                currentTasks.map {
                    if (it.id == task.id) it.copy(isCompleted = newCompletionState) else it
                }
            }
            Log.d(TAG, "UI atualizada otimisticamente para ID: ${task.id}, estado: $newCompletionState")

            try {
                repository.updateTaskCompletion(task.id, newCompletionState)
                Log.d(TAG, "toggleTaskCompletion SUCESSO no repositório para ID: ${task.id}")
                // Se a chamada ao repositório for bem-sucedida, a UI já está correta.
                // Poderia chamar loadTasks() aqui para uma garantia extra de sincronia,
                // mas geralmente não é necessário e pode causar um pequeno "piscar" na UI.
            } catch (e: Exception) {
                Log.e(TAG, "ERRO em toggleTaskCompletion no repositório para ID: ${task.id}", e)
                _error.value = "Erro ao atualizar status: ${e.message}"
                crashlytics.recordException(e)

                // Reverter a mudança otimista na UI se houver erro
                Log.d(TAG, "Revertendo UI para ID: ${task.id} devido a erro.")
                _tasks.value = originalTasks // Restaura a lista original antes da tentativa
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            Log.d(TAG, "Iniciando deleteTask: $taskId")
            try {
                repository.deleteTask(taskId)
                Log.d(TAG, "deleteTask SUCESSO no repositório: $taskId. Atualizando UI...")
                _tasks.update { currentTasks -> currentTasks.filterNot { it.id == taskId } }
            } catch (e: Exception) {
                Log.e(TAG, "Erro em deleteTask: $taskId", e)
                _error.value = "Erro ao deletar tarefa: ${e.message}"
                crashlytics.recordException(e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
