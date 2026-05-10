package com.example.foro2dms.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.foro2dms.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleAuthClient(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)

    suspend fun getIdToken(): Result<String> {
        return try {
            val webClientId = context.getString(R.string.default_web_client_id)

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(context, request)
            val credential = response.credential

            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                Result.success(googleIdToken.idToken)
            } else {
                Result.failure(IllegalStateException("Credencial no compatible"))
            }
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: NoCredentialException) {
            Result.failure(IllegalStateException("No hay cuentas de Google en este dispositivo"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
