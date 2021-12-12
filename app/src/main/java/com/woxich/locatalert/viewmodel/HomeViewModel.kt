package com.woxich.locatalert.viewmodel

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.woxich.locatalert.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val navigationDispatcher: NavigationDispatcher,
    val snackbarDispatcher: SnackbarDispatcher,
    val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _loading = MutableStateFlow(false)
    private val _stay = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    val stay: StateFlow<Boolean> = _stay


    fun redirectToLogin() {
        Log.e(TAG, "redirectToLogin: 1" )
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.popBackStack()
            navController.navigate(NavDest.LOGIN)
        }
    }

    fun redirectToGeoActivity() {
        val geoIntent = Intent(context, MainGeoActivity::class.java)

        geoIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        //  geoIntent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP)

        startActivity(
            context,
            geoIntent,
            null
        )
    }

    fun changedValueOfStay(value: Boolean?) {
        if (value != null)
            _stay.value = value
    }


    fun redirectToProfile() {
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.navigate(NavDest.PROFILE)
        }
    }

    fun redirectToSearch() {
        navigationDispatcher.dispatchNavigationCommand { navController ->
            navController.navigate(NavDest.SEARCH)
        }
    }
}