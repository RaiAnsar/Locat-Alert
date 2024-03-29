package com.woxich.locatalert

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woxich.locatalert.R
import com.woxich.locatalert.ui.theme.LocatAlertTheme
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import com.woxich.locatalert.view.*
import com.woxich.locatalert.viewmodel.*
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import javax.inject.Inject

/**
 * The single activity of the application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Handles navigation commands.
     */
    @Inject
    lateinit var navigationDispatcher: NavigationDispatcher

    @Inject
    lateinit var firebaseMessaging: FirebaseMessaging

    @ExperimentalComposeUiApi
    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //register for events
        EventBus.getDefault().register(this)
        //set lifecycle
        activityLifecycle = this
        //check for google play services
        if (isGooglePlayServicesAvailable(this)) {
            //show token for debug purposes
            logFirebaseMessagingToken()
            //make notification channel
            createNotificationChannel()

            //bundle

            //set composable content
            setContent {
                MainActivityContent(
                    navigationDispatcher = navigationDispatcher,
                    stay = intent.extras?.getBoolean("DATA"),
                    lifecycleOwner = this
                )
            }
        } else {
            //attempt to make Google play services ready
            GoogleApiAvailability.getInstance().makeGooglePlayServicesAvailable(this)
                .addOnCompleteListener { gpsResult ->
                    val message = if (gpsResult.isSuccessful) {
                        getString(R.string.google_play_success)
                    } else {
                        getString(R.string.google_play_fail)
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activityLifecycle = null
        EventBus.getDefault().unregister(this)
    }

    /**
     * Creates notification channel that the app uses for push notifications.
     */
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.gaspar_chat_notification_channel)
            val descriptionText = getString(R.string.gaspar_chat_notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                getString(R.string.gaspar_chat_notification_channel_id),
                name,
                importance
            )
            channel.description = descriptionText
            channel.enableLights(true)
            channel.lightColor = ContextCompat.getColor(this, R.color.purple_500)

            val pattern = LongArray(3)
            pattern[0] = 500; pattern[1] = 500; pattern[2] = 500
            channel.vibrationPattern = pattern

            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks for google play services.
     * @return True only if the GPS are available.
     */
    private fun isGooglePlayServicesAvailable(activity: Activity?): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(activity)
        if (status != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(status)) {
                googleApiAvailability.getErrorDialog(activity, status, 1000).show()
            }
            return false
        }
        return true
    }

    /**
     * On startup, logs the firebase cloud message token of the device.
     */
    private fun logFirebaseMessagingToken() {
        firebaseMessaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
            } else {
                // Get new FCM registration token
                val token = task.result
                // Log
                Log.d(TAG, "FCM token is $token")
            }
        }
    }

    /**
     * Called when the activity receives a new intent, most likely because a notification was clicked WHILE the app
     * was in the foreground.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null && intent.extras != null) {
            //this intent has extras
            if (intent.extras!!.containsKey(INTENT_CHAT_ROOM_ID)) {
                //the extras contain a chat room UID: load and open this chat room
                val chatRoomUid = intent.extras!!.getString(INTENT_CHAT_ROOM_ID)!!
                Log.d(TAG, "onNewIntent was called with a chat room UID: $chatRoomUid")
                EventBus.getDefault().post(ChatRoomChangedEvent(chatRoomUid))
            }
        } else {
            Log.d(TAG, "onNewIntent called without tag or extras. Ignoring...")
        }
    }

    /**
     * Points to selected images.
     */
    var tempUri: Uri? = null

    /**
     * Register the contract that opens gallery.
     */
    val galleryResultContract = registerGalleryResultContract(this)

    /**
     * Called when the activity should open a gallery for image selection.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: OpenGalleryEvent) {
        openGallery(this)
    }

    /**
     * Register the contract that opens camera
     */
    val cameraResultContract = registerCameraResultContract(this)

    /**
     * Called when the activity should open a camera for image capture.
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: OpenCameraEvent) {
        openCamera(this)
    }
}

/**
 * Navigation component that is displayed in the single activity. See classes in the view package
 * for the displayed [Composable]s.
 * @param navigationDispatcher Object used to listen for incoming navigation events.
 * @param lifecycleOwner The activity's lifecycle owner.
 */
@ExperimentalAnimationApi
@ExperimentalComposeUiApi
@Composable
fun MainActivityContent(
    navigationDispatcher: NavigationDispatcher,
    stay: Boolean?,
    lifecycleOwner: LifecycleOwner
) {
    LocatAlertTheme {
        Surface(color = MaterialTheme.colors.background) {
            //create nav controller
            val navController = rememberNavController()
            //create chat room view model, this needs to exist ASAP
            val chatRoomViewModel = hiltViewModel<ChatRoomViewModel>()
            //build nav host, start is HOME, but it redirects to LOGIN if needed
            NavHost(navController = navController, startDestination = NavDest.HOME) {
                //redirect to login screen
                composable(route = NavDest.LOGIN) {
                    val viewModel =
                        hiltViewModel<LoginViewModel>() //this view model will always reset when showing this screen
                    LoginContent(viewModel)
                }
                //redirect to register screen
                composable(route = NavDest.REGISTER) {
                    val viewModel =
                        hiltViewModel<RegisterViewModel>() //this view model will always reset when showing this screen
                    RegisterContent(viewModel)
                }
                //redirect to home
                composable(route = NavDest.HOME) {
                    HomeContent(stay)
                }
                //redirect to profile
                composable(route = NavDest.PROFILE) {
                    ProfileContent()
                }
                //redirect to search
                composable(route = NavDest.SEARCH) {
                    val viewModel =
                        hiltViewModel<SearchViewModel>() //this view model will always reset when showing this screen
                    SearchContent(viewModel = viewModel)
                }
                //redirect to a chat room: REQUIRES CHAT UID
                composable(route = NavDest.CHAT_ROOM) {
                    ChatRoomContent(viewModel = chatRoomViewModel)
                }
            }
            //observe incoming navigation commands
            navigationDispatcher.navigationEmitter.observe(lifecycleOwner) { navigationCommand ->
                navigationCommand.invoke(navController)
            }
        }
    }
}


/**
 * Lifecycle of the only activity in the app. Can be used to observe global events.
 */
var activityLifecycle: LifecycleOwner? = null