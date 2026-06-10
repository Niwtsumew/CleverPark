package ua.parking.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl : AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun getCurrentUser(): FirebaseUser? = auth.currentUser

    override suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: FirebaseAuthInvalidUserException) {
            Result.failure(Exception("Користувача не знайдено"))
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            Result.failure(Exception("Невірний пароль"))
        } catch (e: Exception) {
            Result.failure(Exception("Помилка з'єднання. Перевірте інтернет"))
        }
    }

    override suspend fun signUp(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            Result.success(result.user!!)
        } catch (e: Exception) {
            Result.failure(Exception("Помилка входу через Google"))
        }
    }

    override fun signOut() = auth.signOut()

    override suspend fun isAdmin(uid: String): Boolean {
        return try {
            val doc = db.collection("admins").document(uid).get().await()
            doc.exists()
        } catch (e: Exception) {
            false
        }
    }
}
