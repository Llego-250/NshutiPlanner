package com.example.nshutiplanner.data.repository

import com.example.nshutiplanner.data.model.Actor
import com.example.nshutiplanner.data.model.LocationRecord
import com.example.nshutiplanner.data.model.Role
import com.example.nshutiplanner.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LocationRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val httpClient = OkHttpClient()

    private val projectId = "nshutitracker"
    private val fcmEndpoint =
        "https://fcm.googleapis.com/v1/projects/$projectId/messages:send"

    /**
     * Resolves the current user (SENDER) and their partner (RECEIVER) as Actor objects.
     */
    suspend fun resolveActors(currentUser: User): Pair<Actor, Actor> {
        val partnerDoc = db.collection("users")
            .document(currentUser.partnerId)
            .get()
            .await()
        val partner = partnerDoc.toObject(User::class.java)
            ?: throw Exception("Partner document not found for uid=${currentUser.partnerId}")

        val senderActor = Actor(
            uid = currentUser.uid,
            displayName = currentUser.displayName,
            photoUrl = currentUser.photoUrl,
            fcmToken = currentUser.fcmToken,
            role = Role.SENDER
        )
        val receiverActor = Actor(
            uid = partner.uid,
            displayName = partner.displayName,
            photoUrl = partner.photoUrl,
            fcmToken = partner.fcmToken,
            role = Role.RECEIVER
        )
        return Pair(senderActor, receiverActor)
    }

    /**
     * Writes the sender's LocationRecord to Firestore at locations/{actor.uid}.
     * Only valid for SENDER actors.
     */
    suspend fun publishSenderLocation(actor: Actor, location: LocationRecord) {
        if (actor.role != Role.SENDER) {
            throw IllegalArgumentException("Only SENDER can publish location")
        }
        val data = mapOf(
            "uid" to location.uid,
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "accuracyMetres" to location.accuracyMetres,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("locations")
            .document(actor.uid)
            .set(data)
            .await()
    }

    /**
     * Reads the receiver's LocationRecord from Firestore at locations/{actor.uid}.
     * Only valid for RECEIVER actors.
     */
    suspend fun fetchReceiverLocation(actor: Actor): LocationRecord {
        if (actor.role != Role.RECEIVER) {
            throw IllegalArgumentException("Only RECEIVER location can be fetched")
        }
        val doc = db.collection("locations")
            .document(actor.uid)
            .get()
            .await()
        if (!doc.exists()) {
            throw Exception("Receiver location unavailable")
        }
        return doc.toObject(LocationRecord::class.java)
            ?: throw Exception("Receiver location unavailable")
    }

    /**
     * Sends an FCM data message to the receiver's device to trigger a haptic pulse.
     * Only valid when receiver has role RECEIVER and a non-blank FCM token.
     */
    suspend fun dispatchVibrationSignal(sender: Actor, receiver: Actor) {
        if (receiver.role != Role.RECEIVER) {
            throw IllegalArgumentException("Vibration can only be dispatched to RECEIVER")
        }
        if (receiver.fcmToken.isBlank()) {
            throw IllegalArgumentException("Receiver FCM token is absent")
        }

        val idToken = auth.currentUser
            ?.getIdToken(false)
            ?.await()
            ?.token
            ?: throw Exception("Unable to obtain Firebase ID token")

        val payload = JSONObject().apply {
            put("message", JSONObject().apply {
                put("token", receiver.fcmToken)
                put("data", JSONObject().apply {
                    put("type", "vibration")
                    put("senderUid", sender.uid)
                    put("senderName", sender.displayName)
                })
            })
        }.toString()

        val requestBody = payload.toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(fcmEndpoint)
            .addHeader("Authorization", "Bearer $idToken")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "unknown error"
            throw Exception("FCM dispatch failed (${response.code}): $errorBody")
        }
    }
}
