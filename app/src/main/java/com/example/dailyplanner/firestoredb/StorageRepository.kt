package com.example.dailyplanner.firestoredb

import com.example.dailyplanner.Plan
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.Flow

const val PLANS_COLLECTION_REF = "plans"

class StorageRepository() {
    private val firestore = FirebaseFirestore.getInstance()

    private val plansRef = firestore.collection(PLANS_COLLECTION_REF)


    fun getUserPlans(
        userId: String,
    ): Flow<Resources<List<Plan>>> = callbackFlow {
        var snapshotStateListener: ListenerRegistration? = null

        try {
            snapshotStateListener = plansRef
                .whereEqualTo("userId", userId)
                .addSnapshotListener { snapshot, e ->
                    val response = if (snapshot != null) {
                        val plans = snapshot.toObjects(Plan::class.java)
                        Resources.Success(data = plans)
                    } else {
                        Resources.Error(throwable = e?.cause)
                    }
                    trySend(response)

                }


        } catch (e: Exception) {
            trySend(Resources.Error(e.cause))
            e.printStackTrace()
        }

        awaitClose {
            snapshotStateListener?.remove()
        }


    }

    fun addPlan(
        userId : String,
        date : String,
        time : String,
        planText : String,
        useful_habit :Boolean,
        planDone : Boolean
    ) {
        val documentId = plansRef.document().id
        val planAdd = Plan(
            userId = userId,
            date = date,
            time = time,
            planText = planText,
            useful_habit = useful_habit,
            planDone = planDone,
            documentId = documentId
        )
        plansRef
            .document(documentId)
            .set(planAdd)


    }
    fun deletePlan(planId: String){
        plansRef.document(planId)
            .delete()
    }

    fun updatePlan(
        planId: String,
        planDone: Boolean
    ) {
        val updateData = hashMapOf<String, Any>(
            "planDone" to planDone,
        )

        plansRef.document("/$planId").update(updateData)
    }
    fun signOut() = Firebase.auth.signOut()


    sealed class Resources<T>(
        val data: T? = null,
        val throwable: Throwable? = null,
    ) {
        class Loading<T> : Resources<T>()
        class Success<T>(data: T?) : Resources<T>(data = data)
        class Error<T>(throwable: Throwable?) : Resources<T>(throwable = throwable)

    }}
