package com.example.nshutiplanner.data.repository

import android.net.Uri
import com.example.nshutiplanner.data.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val currentUid get() = auth.currentUser?.uid ?: ""

    // ── Auth ──────────────────────────────────────────────────────────────────

    suspend fun register(email: String, password: String, name: String): Result<User> = runCatching {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        val coupleId = uid  // solo until partner links
        val user = User(uid = uid, displayName = name, email = email, coupleId = coupleId)
        db.collection("users").document(uid).set(user).await()
        user
    }

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    fun logout() = auth.signOut()

    suspend fun getUser(uid: String): User? =
        db.collection("users").document(uid).get().await().toObject(User::class.java)

    suspend fun linkPartner(partnerEmail: String): Result<Unit> = runCatching {
        val snap = db.collection("users").whereEqualTo("email", partnerEmail).get().await()
        val partner = snap.documents.firstOrNull()?.toObject(User::class.java)
            ?: error("Partner not found")
        val coupleId = listOf(currentUid, partner.uid).sorted().joinToString("_")
        db.collection("users").document(currentUid).update("partnerId", partner.uid, "coupleId", coupleId).await()
        db.collection("users").document(partner.uid).update("partnerId", currentUid, "coupleId", coupleId).await()
    }

    // ── Plans ─────────────────────────────────────────────────────────────────

    fun getPlans(coupleId: String): Flow<List<Plan>> = callbackFlow {
        val listener = db.collection("plans")
            .whereEqualTo("coupleId", coupleId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
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
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
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
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(VisionItem::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    suspend fun saveVisionItem(item: VisionItem, coupleId: String) {
        val ref = if (item.id.isEmpty()) db.collection("vision").document()
        else db.collection("vision").document(item.id)
        ref.set(item.copy(id = ref.id, coupleId = coupleId)).await()
    }

    suspend fun uploadVisionImage(uri: Uri, coupleId: String): String {
        val ref = storage.reference.child("vision/$coupleId/${System.currentTimeMillis()}.jpg")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    suspend fun deleteVisionItem(itemId: String) {
        db.collection("vision").document(itemId).delete().await()
    }

    // ── NshutiCare ────────────────────────────────────────────────────────────

    fun getMessages(coupleId: String): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("messages")
            .whereEqualTo("coupleId", coupleId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, _ ->
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
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(14)
            .addSnapshotListener { snap, _ ->
                trySend(snap?.toObjects(MoodEntry::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }

    // ── Badges ────────────────────────────────────────────────────────────────

    fun getBadges(coupleId: String): Flow<List<Badge>> = callbackFlow {
        val listener = db.collection("badges")
            .whereEqualTo("coupleId", coupleId)
            .addSnapshotListener { snap, _ ->
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
