package com.lubenard.eye42

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment

class MainActivity : AppCompatActivity() {

    val TAG = this::class.simpleName

    companion object {
        var networkManager: NetworkManager? = null
    }

    var navController: NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        val host = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment?
        navController = host?.navController

        navController?.addOnDestinationChangedListener { controller, destination, arguments ->
            Log.d(TAG, "Controller $controller, destination $destination, arguments $arguments")
        }

        networkManager = NetworkManager(context = this)

        Log.d(TAG, "navController is $navController, navigating to search...")
    }
}