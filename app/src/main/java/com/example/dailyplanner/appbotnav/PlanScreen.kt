package com.example.dailyplanner.appbotnav

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.dailyplanner.Plan
import com.example.dailyplanner.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun rememberFirebaseAuthLauncher(
    onAuthComplete: (AuthResult) -> Unit, onAuthError: (ApiException) -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
    val scope = rememberCoroutineScope()
    return rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            scope.launch {
                val authResult = Firebase.auth.signInWithCredential(credential).await()
                onAuthComplete(authResult)
            }
        } catch (e: ApiException) {
            onAuthError(e)
        }
    }
}

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Plans(
    viewModel: PlansViewModel = viewModel(), onAddNotify: NotificationManager
) {
    LaunchedEffect(key1 = Unit) {
        viewModel.loadPlans()
    }
    var pickedDate by remember {
        mutableStateOf(LocalDate.now())
    }
    val formattedDate by remember {
        derivedStateOf { DateTimeFormatter.ofPattern("dd-MM-yyyy").format(pickedDate) }
    }
    val formattedDateforButton by remember {
        derivedStateOf { DateTimeFormatter.ofPattern("dd MMM yyyy").format(pickedDate) }
    }
    val dateDialogState = rememberMaterialDialogState()
    val openDialog = remember { mutableStateOf(false) }
    val openProfile = remember { mutableStateOf(false) }

    var pickedTime by remember {
        mutableStateOf(LocalTime.now(Clock.systemDefaultZone()))
    }
    val timeDialogState = rememberMaterialDialogState()
    val formattedTime by remember {
        derivedStateOf {
            DateTimeFormatter.ofPattern("HH:mm").format(pickedTime)
        }
    }
    var user by remember { mutableStateOf(Firebase.auth.currentUser) }
    val launcher = rememberFirebaseAuthLauncher(onAuthComplete = { result ->
        user = result.user
    }, onAuthError = {
        user = null
    })
    val token = stringResource(R.string.default_web_client_id)
    val context = LocalContext.current
    val notificationId = 999
    val channelId = "channelID"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Create the NotificationChannel.
        val name = stringResource(R.string.channel_name)
        val descriptionText = stringResource(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(channelId, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system. You can't change the importance
        // or other notification behaviors after this.
        onAddNotify.createNotificationChannel(mChannel)
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            modifier = Modifier
                .height(16.dp)
                .padding(bottom = 2.dp),
            progress = viewModel.daysCheckedPlans(formattedDate),
            color = Color(150, 100, 255)
        )
        Row(
            modifier = Modifier
                .width(220.dp)
                .height(35.dp)
        ) {


            Button(
                onClick = { dateDialogState.show() },
                modifier = Modifier
                    .size(width = 150.dp, height = 35.dp)
                    .padding(start = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.White, contentColor = Color.Black
                )
            ) {

                Text(
                    text = formattedDateforButton,
                    modifier = Modifier.padding(0.dp),
                    style = MaterialTheme.typography.body2
                )
            }
            Button(
                onClick = { openDialog.value = true },
                shape = RoundedCornerShape(10),
                modifier = Modifier
                    .size(width = 60.dp, height = 35.dp)
                    .padding(start = 10.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(150, 100, 255))
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.add_button
                    ), contentDescription = "Image", modifier = Modifier.size(20.dp)
                )
            }

        }



        RecyclerView(
            currentDay = formattedDate,
            viewModel = viewModel(),
        )
    }
    MaterialDialog(dialogState = timeDialogState, buttons = {
        positiveButton(text = stringResource(R.string.ok))
        negativeButton(text = stringResource(R.string.cancel))
    }) {
        timepicker(

            title = stringResource(R.string.PickTime), is24HourClock = true
        ) {
            pickedTime = it
        }
    }

    MaterialDialog(dialogState = dateDialogState, buttons = {
        positiveButton(text = stringResource(R.string.ok))
        negativeButton(text = stringResource(R.string.cancel))
    }) {
        datepicker(
            initialDate = LocalDate.now(),
            title = (stringResource(R.string.pickDay)),
        ) {
            pickedDate = it
        }

    }




    if (openDialog.value) {
        AlertDialog(onDismissRequest = {
            openDialog.value = false
        }, title = {
            Text(text = stringResource(R.string.addPlan))
        }, text = {
            Column {

                Button(
                    onClick = { timeDialogState.show() },
                    modifier = Modifier
                        .size(height = 40.dp, width = 70.dp)
                        .padding(bottom = 5.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(150, 100, 255), contentColor = Color.Black
                    )
                ) {
                    Text(text = formattedTime, style = MaterialTheme.typography.body2)
                }
                TextField(value = viewModel.planUiState.planText,
                    onValueChange = { viewModel.onTextChange(it) })

                Row(modifier = Modifier.wrapContentSize()) {
                    Checkbox(
                        checked = viewModel.planUiState.useful_habit,
                        onCheckedChange = { viewModel.onHabitChange(it) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(255, 255, 255),
                            checkmarkColor = Color(43, 0, 61)
                        )
                    )
                    Text(
                        text = stringResource(R.string.usefulHabit),
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }, buttons = {
            Row(
                modifier = Modifier.padding(all = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(150, 100, 255), contentColor = Color.Black
                ), onClick = {
                    viewModel.onTimeChange(formattedTime)
                    viewModel.onDateChange(formattedDate)
                    openDialog.value = false
                    viewModel.addPlan()
                    viewModel.loadPlans()
                    val intent = Intent(context, Plan::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    val pendingIntent: PendingIntent = PendingIntent.getActivity(
                        context, 0, intent, PendingIntent.FLAG_MUTABLE
                    )

                    val startOfDay = LocalDateTime.of(pickedDate, LocalTime.MIN)
                    val millis =
                        startOfDay.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val delay =
                        pickedTime.toNanoOfDay() / 1000000 + millis - System.currentTimeMillis() - 1000 * 60 * 60
                    val builder = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_launcher_foreground)
                        .setContentTitle(":)")
                        .setContentText("Вы запланировали в ${viewModel.planUiState.time} ${viewModel.planUiState.planText}")
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent).setGroupSummary(true)
                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        with(NotificationManagerCompat.from(context)) {
                            notify(notificationId, builder.build()) // посылаем уведомление
                        }
                    }, delay)


                    viewModel.planUiState = PlanUiState()

                }) {
                    Text("Добавить")
                }
            }
        })

    }
    Column(
        modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (user == null) {
                Button(onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(token).requestEmail().build()
                    val googleSignInClient = GoogleSignIn.getClient(context, gso)
                    launcher.launch(googleSignInClient.signInIntent)
                }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(150, 100, 255))) {
                    Image(
                        painter = painterResource(
                            id = R.drawable.google
                        ), contentDescription = "Image", modifier = Modifier.size(20.dp)
                    )
                }
                viewModel.loadPlans()

            } else {
                Button(onClick = {
                    viewModel.signOut()
                    user = null
                    viewModel.loadPlans()
                }, colors = ButtonDefaults.buttonColors(backgroundColor = Color(150, 100, 255))) {
                    Image(
                        painter = painterResource(
                            id = R.drawable.google
                        ), contentDescription = "Image", modifier = Modifier.size(20.dp)
                    )
                }
            }
            Button(
                onClick = { openProfile.value = true },
                shape = RoundedCornerShape(10),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(150, 100, 255))
            ) {
                Image(
                    painter = painterResource(
                        id = R.drawable.profile_icon
                    ), contentDescription = "Image", modifier = Modifier.size(20.dp)
                )
            }

        }

    }




    if (openProfile.value) {
        AlertDialog(onDismissRequest = {
            openDialog.value = false
        }, modifier = Modifier.wrapContentSize(), title = {
            Text(
                if (user != null) user!!.displayName.toString() else stringResource(R.string.signIn),
                style = MaterialTheme.typography.h5,
                textDecoration = TextDecoration.Underline
            )
        }, text = {
            Text(
                text = "Вы выполнили ${viewModel.allDaysHabit()} (В этот день : ${
                    viewModel.habitDayCheckedPlans(
                        formattedDate
                    )
                }) полезных привычек"
            )
        }, buttons = {
            Row(
                modifier = Modifier.padding(all = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(150, 100, 255), contentColor = Color.Black
                ), onClick = {
                    openProfile.value = false
                }) {
                    Text("Закрыть")
                }
            }
        })

    }
}

@Composable
fun ListItem(
    plan: Plan,
    viewModel: PlansViewModel,
) {

    val checkboxChanged = remember {
        mutableStateOf(false)
    }
    var showDeleteConfirm by remember {
        mutableStateOf(false)
    }
    Surface(
        color = Color(200, 208, 255),
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(9.dp)
                .fillMaxWidth(),
        ) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = plan.time, style = MaterialTheme.typography.h5)

                }

                Checkbox(
                    checked = viewModel.getPlan(plan.documentId).planDone, onCheckedChange = {
                        viewModel.getPlan(plan.documentId).planDone = it; checkboxChanged.value =
                        true
                    }, colors = CheckboxDefaults.colors(
                        checkedColor = Color(255, 255, 255), checkmarkColor = Color(43, 0, 61)
                    )
                )
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                }
            }
            Column() {

                Text(text = plan.planText)
                if (plan.useful_habit) Image(
                    painter = painterResource(id = R.drawable.usefulhabit),
                    contentDescription = "Image",
                )

            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                modifier = Modifier.wrapContentSize(),
                onDismissRequest = { showDeleteConfirm = false },
                text = {
                    Text("Вы дейставительно хотите удалить план?")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            viewModel.deletePlan(plan.documentId)
                        }, colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(150, 100, 255)
                        )
                    ) {
                        Text("Да")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                        }, colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(150, 100, 255)
                        )
                    ) {
                        Text("Нет")
                    }
                }
            )
        }
    }
    if (checkboxChanged.value) {
        viewModel.updatePlan(
            viewModel.getPlan(plan.documentId).documentId,
        )
        checkboxChanged.value = false
    }

}


@Composable
fun RecyclerView(
    currentDay: String,
    viewModel: PlansViewModel = viewModel(),
) {

    LazyColumn(
        modifier = Modifier.padding(bottom = 55.dp)
    ) {
        items(items = viewModel.getCurrentDayPlans(currentDay)) { plan ->
            ListItem(
                Plan(
                    plan.userId,
                    currentDay,
                    plan.time,
                    plan.planText,
                    plan.useful_habit,
                    plan.planDone,
                    plan.documentId
                ),

                viewModel = viewModel,

                )
        }
    }

}

