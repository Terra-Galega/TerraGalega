package com.example.terragalegamovile.data

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

// Client ID WEB de Google Cloud (el mismo de la web y de GOOGLE_CLIENT_ID del back).
// Ojo: aquí va el de tipo "Aplicación web", no el de Android. Además hay que
// crear en Google Cloud un ID de cliente tipo "Android" con el paquete
// com.example.terragalegamovile y la huella SHA-1 de la llave de firma; ese no
// se pone en el código, Google lo usa para reconocer la app.
// Vacío = el botón "Continuar con Google" no aparece.
const val GOOGLE_WEB_CLIENT_ID = "805596346290-qk1d7bngiepvdvpbibqvjobkmpidd3ch.apps.googleusercontent.com"

// Abre la hoja de cuentas de Google y devuelve el ID token, o null si el
// usuario la cerró. El back verifica ese token en POST /auth/google.
// context debe ser la Activity (LocalContext en Compose).
suspend fun requestGoogleIdToken(context: Context): String? {
    val option = GetSignInWithGoogleOption.Builder(GOOGLE_WEB_CLIENT_ID).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(option)
        .build()

    return try {
        val credential = CredentialManager.create(context)
            .getCredential(context, request)
            .credential

        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            throw IllegalStateException("Google no devolvió una cuenta válida.")
        }
    } catch (_: GetCredentialCancellationException) {
        null
    } catch (_: NoCredentialException) {
        throw IllegalStateException("No hay ninguna cuenta de Google en este dispositivo.")
    } catch (e: GetCredentialException) {
        throw IllegalStateException("No se pudo usar Google: ${e.message ?: e.type}")
    }
}
