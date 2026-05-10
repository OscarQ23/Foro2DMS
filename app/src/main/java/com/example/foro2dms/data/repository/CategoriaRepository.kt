package com.example.foro2dms.data.repository

import com.example.foro2dms.data.model.Categoria
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CategoriaRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun categoriasCollection() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("categorias")
    } ?: throw IllegalStateException("No hay usuario autenticado")

    fun observeCategorias(): Flow<List<Categoria>> = callbackFlow {
        val listener = categoriasCollection()
            .orderBy("nombre", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val categorias = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Categoria::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(categorias)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addCategoria(nombre: String): Result<Unit> {
        return try {
            categoriasCollection().add(Categoria(nombre = nombre.trim()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCategoria(categoriaId: String): Result<Unit> {
        return try {
            categoriasCollection().document(categoriaId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
