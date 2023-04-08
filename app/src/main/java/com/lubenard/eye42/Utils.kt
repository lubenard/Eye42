package com.lubenard.eye42

import android.content.Context
import android.util.Log
import android.view.View
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.navigation.NavController
import com.lubenard.eye42.ui.theme.Eye42Theme
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
fun setContentScreen(context: Context, navController: NavController, pageTitle: String, callback: @Composable (() -> Unit)) : View {
    return ComposeView(context).apply {
        setContent {
            Eye42Theme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text(text = pageTitle) },
                                navigationIcon = {
                                    if (navController.previousBackStackEntry != null) {
                                        IconButton(onClick = { navController.navigateUp() }) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowBack,
                                                contentDescription = "Back"
                                            )
                                        }
                                    }
                                }
                            )
                        },
                        content = {
                            Column(modifier = Modifier.fillMaxSize().padding(it)) {
                                callback.invoke()
                            }
                        }
                    )
                }
            }
        }
    }
}

fun checkJsonFields(jsonObject: JSONObject, fieldList: List<String>) : Boolean {
    Log.d("checkJsonFields", "Return ${fieldList.none { !jsonObject.has(it) }}")
    return fieldList.none { !jsonObject.has(it) }
}