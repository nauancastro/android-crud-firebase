package com.example.firebaseapp.data.model

data class Task(
    val id: String = "", // ID gerado pelo Firestore
    val title: String = "",
    val description: String = "",
    val isCompleted: Boolean = false
)