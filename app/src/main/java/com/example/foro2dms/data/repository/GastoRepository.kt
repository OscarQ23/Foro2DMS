package com.example.foro2dms.data.repository

import com.example.foro2dms.data.model.Gasto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GastoRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    private fun gastosCollection() = auth.currentUser?.uid?.let { uid ->
        firestore.collection("users").document(uid).collection("gastos")
    } ?: throw IllegalStateException("No hay usuario autenticado")

    fun observeGastos(): Flow<List<Gasto>> = callbackFlow {
        val listener = gastosCollection()
            .orderBy("fecha", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val gastos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Gasto::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(gastos)
            }

        awaitClose { listener.remove() }
    }

    suspend fun addGasto(gasto: Gasto): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay usuario autenticado"))

            val gastoConUserId = gasto.copy(userId = uid)

            gastosCollection().add(gastoConUserId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateGasto(gasto: Gasto): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("No hay usuario autenticado"))

            if (gasto.id.isBlank()) {
                return Result.failure(IllegalArgumentException("El gasto no tiene ID"))
            }

            val gastoConUserId = gasto.copy(userId = uid)
            gastosCollection().document(gasto.id).set(gastoConUserId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteGasto(gastoId: String): Result<Unit> {
        return try {
            gastosCollection().document(gastoId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
