package com.example.nshutiplanner.data.repository

import android.content.Context
import android.net.Uri
import com.example.nshutiplanner.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val cloudinary = CloudinaryRepository(context)

    val currentUid get() = auth.currentUser?.uid ?: ""

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun register(email: String, password: String, name: String): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        val coupleId = uid  // solo until partner links
        val user = User(uid = uid, displayName = name, email = email.trim().lowercase(), coupleId = coupleId)
        db.collection("users").document(uid).set(user).await()
        user
    }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        // Ensure Firestore document exists — creates it if the user signed up
        // via a path that didn't write to Firestore (e.g. direct Auth creation)
        val docRef = db.collection("users").document(uid)
        val existing = docRef.get().await()
        if (!existing.exists()) {
            val user = User(
                uid = uid,
                displayName = result.user?.displayName ?: email.substringBefore("@"),
                email = email.trim().lowercase(),
                coupleId = uid
            )
            docRef.set(user).await()
        } else if (existing.getString("email").isNullOrBlank()) {
            // Document exists but email field is missing — patch it
            docRef.update("email", email.trim().lowercase()).await()
        }
    }

    fun logout() = auth.signOut()

    suspend fun getUser(uid: String): User? =
        db.collection("users").document(uid).get().await().toObject(User::class.java)

    /**
     * Ensures a Firestore document exists for the currently signed-in user.
     * Called on every app launch — safe to call repeatedly (uses set with merge).
     */
    suspend fun ensureProfile(): User {
        val firebaseUser = auth.currentUser ?: error("Not signed in")
        val uid = firebaseUser.uid
        val email = (firebaseUser.email ?: "").trim().lowercase()
        val docRef = db.collection("users").document(uid)
        val snap = docRef.get().await()

        return if (!snap.exists()) {
            // Document missing — create it now
            val user = User(
                uid = uid,
                displayName = firebaseUser.displayName?.ifBlank { email.substringBefore("@") }
                    ?: email.substringBefore("@"),
                email = email,
                coupleId = uid
            )
            docRef.set(user).await()
            user
        } else {
            // Document exists — patch email if blank
            val stored = snap.toObject(User::class.java) ?: User(uid = uid, email = email, coupleId = uid)
            if (stored.email.isBlank() && email.isNotBlank()) {
                docRef.update("email", email).await()
                stored.copy(email = email)
            } else {
                stored
            }
        }
    }

    suspend fun linkPartner(partnerEmail: String): Result<Unit> = runCatching {
        val normalizedEmail = partnerEmail.trim().lowercase()

        // Try exact match first
        var snap = db.collection("users").whereEqualTo("email", normalizedEmail).get().await()

        // Fallback: original casing (in case stored with uppercase)
        if (snap.isEmpty) {
            snap = db.collection("users").whereEqualTo("email", partnerEmail.trim()).get().await()
        }

        // Fallback: scan all users and compare email case-insensitively
        // (handles accounts where email was stored with different casing)
        val partner: User = if (!snap.isEmpty) {
            snap.documents.firstOrNull()?.toObject(User::class.java)
                ?: error("Partner not found")
        } else {
            val allUsers = db.collection("users").get().await()
            allUsers.documents
                .mapNotNull { it.toObject(User::class.java) }
                .firstOrNull { it.email.trim().lowercase() == normalizedEmail }
                ?: error("Partner not found. Make sure they have signed up with this email.")
        }

        if (partner.uid == currentUid) error("You cannot link yourself as a partner.")

        val coupleId = listOf(currentUid, partner.uid).sorted().joinToString("_")
        db.collection("users").document(currentUid)
            .update("partnerId", partner.uid, "coupleId", coupleId).await()
        db.collection("users").document(partner.uid)
            .update("partnerId", currentUid, "coupleId", coupleId).await()
    }

    suspend fun updateProfile(uid: String, updates: Map<String, Any>) {
        db.collection("users").document(uid).update(updates).await()
    }

    suspend fun uploadProfilePhoto(uri: Uri, uid: String): String =
        cloudinary.upload(uri, folder = "nshuti/profiles")

    // ── Plans ─────────────────────────────────────────────────────────────────

    fun getPlans(coupleId: String): Flow<List<Plan>> = callbackFlow {
        val listener = db.collection("plans")
            .whereEqualTo("coupleId", coupleId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.toObjects(Plan::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun savePlan(plan: Plan, coupleId: String) {
        val ref = if (plan.id.isEmpty()) db.collection("plans").document()
        else db.collection("plans").document(plan.id)
        ref.set(plan.copy(id = ref.id, coupleId = coupleId)).await()
    }

    suspend fun togglePlan(planId: String, completed: Boolean) {
        db.collection("plans").document(planId).update("isCompleted", completed).await()
    }

    suspend fun deletePlan(planId: String) {
        db.collection("plans").document(planId).delete().await()
    }

    // ── Tasks ─────────────────────────────────────────────────────────────────

    fun getTasks(coupleId: String): Flow<List<Task>> = callbackFlow {
        val listener = db.collection("tasks")
            .whereEqualTo("coupleId", coupleId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.toObjects(Task::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveTask(task: Task, coupleId: String) {
        val ref = if (task.id.isEmpty()) db.collection("tasks").document()
        else db.collection("tasks").document(task.id)
        ref.set(task.copy(id = ref.id, coupleId = coupleId)).await()
    }

    suspend fun updateTaskProgress(taskId: String, progress: Float, completed: Boolean) {
        db.collection("tasks").document(taskId)
            .update("progress", progress, "isCompleted", completed).await()
    }

    suspend fun deleteTask(taskId: String) {
        db.collection("tasks").document(taskId).delete().await()
    }

    // ── Vision Board ──────────────────────────────────────────────────────────

    fun getVisionItems(coupleId: String): Flow<List<VisionItem>> = callbackFlow {
        val listener = db.collection("vision")
            .whereEqualTo("coupleId", coupleId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.toObjects(VisionItem::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveVisionItem(item: VisionItem, coupleId: String) {
        val ref = if (item.id.isEmpty()) db.collection("vision").document()
        else db.collection("vision").document(item.id)
        ref.set(item.copy(id = ref.id, coupleId = coupleId)).await()
    }

    suspend fun uploadVisionImage(uri: Uri, coupleId: String): String =
        cloudinary.upload(uri, folder = "nshuti/vision/$coupleId")

    suspend fun deleteVisionItem(itemId: String) {
        db.collection("vision").document(itemId).delete().await()
    }

    // ── NshutiCare ────────────────────────────────────────────────────────────

    fun getMessages(coupleId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("messages")
            .whereEqualTo("coupleId", coupleId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.toObjects(Message::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(message: Message, coupleId: String) {
        val ref = db.collection("messages").document()
        ref.set(message.copy(id = ref.id, coupleId = coupleId)).await()
    }

    suspend fun saveMood(entry: MoodEntry, coupleId: String) {
        val ref = db.collection("moods").document()
        ref.set(entry.copy(id = ref.id, coupleId = coupleId)).await()
    }

    fun getMoods(coupleId: String): Flow<List<MoodEntry>> = callbackFlow {
        val listener = db.collection("moods")
            .whereEqualTo("coupleId", coupleId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.toObjects(MoodEntry::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ── Badges ────────────────────────────────────────────────────────────────

    fun getBadges(coupleId: String): Flow<List<Badge>> = callbackFlow {
        val listener = db.collection("badges")
            .whereEqualTo("coupleId", coupleId)
            .addSnapshotListener { snap, err ->
                if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                trySend(snap?.toObjects(Badge::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun awardBadge(badge: Badge, coupleId: String) {
        val existing = db.collection("badges")
            .whereEqualTo("coupleId", coupleId)
            .whereEqualTo("id", badge.id).get().await()
        if (existing.isEmpty) {
            val ref = db.collection("badges").document()
            ref.set(badge.copy(coupleId = coupleId, earnedBy = currentUid)).await()
        }
    }
}
