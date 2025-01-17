package com.example.dailyplanner.appbotnav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dailyplanner.Plan
import com.example.dailyplanner.firestoredb.StorageRepository

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

class PlansViewModel(private val repository: StorageRepository = StorageRepository()) :
    ViewModel() {
    private fun userId() = Firebase.auth.currentUser?.uid.orEmpty()
    private fun hasUser(): Boolean = Firebase.auth.currentUser != null

    var planListUiState by mutableStateOf(PlanListUiState())
    var planUiState by mutableStateOf(PlanUiState())
    fun loadPlans() {
        if (hasUser()) {
            if (userId().isNotBlank()) {
                getUserPlan(userId())
            }
        } else {
            planListUiState = planListUiState.copy(
                planList = StorageRepository.Resources.Error(
                    throwable = Throwable(message = "User is not Login")
                )
            )
        }
    }

    private fun getUserPlan(userId: String) = viewModelScope.launch {
        repository.getUserPlans(userId).collect {
            planListUiState = planListUiState.copy(planList = it)
        }
    }

    fun signOut() = repository.signOut()

    fun getPlan(id: String): Plan {
       val planList =  planListUiState.planList.data!!
        return planList.first{it.documentId == id}
    }

    fun onDateChange(date: String) {
        planUiState = planUiState.copy(date = date)
    }

    fun onTimeChange(time: String) {
        planUiState = planUiState.copy(time = time)
    }

    fun onTextChange(planText: String) {
        planUiState = planUiState.copy(planText = planText)
    }

    fun onHabitChange(habit: Boolean) {
        planUiState = planUiState.copy(useful_habit = habit)
    }



    fun addPlan() {
        repository.addPlan(
            userId = userId(),
            date = planUiState.date,
            time = planUiState.time,
            planText = planUiState.planText,
            useful_habit = planUiState.useful_habit,
            planDone = planUiState.planDone,

            )
    }
    fun updatePlan(
        planId: String,

    ){
        repository.updatePlan(
           planId = planId,
            planDone = getPlan(planId).planDone
        )
    }
    fun deletePlan(id : String){
        repository.deletePlan(id)
    }


    fun getCurrentDayPlans(date: String): List<Plan> =
        if (planListUiState.planList.data != null) planListUiState.planList.data!!.filter { it.date == date }.sortedBy { it.time.split(":").toString() } else listOf()

    fun daysCheckedPlans(day: String): Float =
        if (getCurrentDayPlans(day).isNotEmpty()) (getCurrentDayPlans(day).filter { it.planDone }.size.toFloat() / getCurrentDayPlans(
            day
        ).size.toFloat()) else 0f

    fun habitDayCheckedPlans(day: String): Int =
        (getCurrentDayPlans(day).filter { it.planDone && it.useful_habit }.size)

    fun allDaysHabit() : Int = if (planListUiState.planList.data != null) (planListUiState.planList.data!!.filter { it.planDone && it.useful_habit }.size) else 0
    }


data class PlanUiState(
    var date: String = "",
    var time: String = "",
    var planText: String = "",
    var useful_habit: Boolean = false,
    var planDone: Boolean = false,
    var documentId: String = ""
)

data class PlanListUiState(
    val planList: StorageRepository.Resources<List<Plan>> = StorageRepository.Resources.Loading(),
)
