package com.woxich.locatalert.model

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.woxich.locatalert.MainActivity
import com.woxich.locatalert.utils.ENTER_CIRCLE_OPTIONS
import com.woxich.locatalert.utils.ENTER_MARKER_OPTIONS
import com.woxich.locatalert.utils.EXIT_CIRCLE_OPTIONS
import com.woxich.locatalert.utils.EXIT_MARKER_OPTIONS
import com.woxich.locatalert.navigation.Directions
import com.woxich.locatalert.navigation.NavigationManager
import com.woxich.locatalert.presentation.MainNoteActivity
import com.woxich.locatalert.utils.addGeofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.libraries.maps.GoogleMap
import com.google.android.libraries.maps.model.Circle
import com.google.android.libraries.maps.model.CircleOptions
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MapScreenViewModel @Inject constructor(
    private val repository: AlarmsRepository,
    private val navigationManager: NavigationManager,
    private val geofencingClient: GeofencingClient,
    val firebaseAuth: FirebaseAuth,
    private val geofencePendingIntent: PendingIntent,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _lastMarker = MutableLiveData<Marker?>(null)
    val lastMarker: LiveData<Marker?>
        get() = _lastMarker

    private val _lastCircle = MutableLiveData<Circle?>(null)
    private val lastCircle: LiveData<Circle?>
        get() = _lastCircle

    private val _alarmName = MutableLiveData("")
    val alarmName: LiveData<String>
        get() = _alarmName

    private val _sliderPosition = MutableLiveData(0f)
    val sliderPosition: LiveData<Float>
        get() = _sliderPosition

    private val _alarmType = MutableLiveData(AlarmType.ON_ENTRY)
    val alarmType: LiveData<AlarmType>
        get() = _alarmType


    val areaRadius = Transformations.map(sliderPosition) {
        sliderPosition.value?.times(1000)?.toInt()
    }

    val alarms = repository.getAlarmsLive()

    private var googleMapMarkers = mutableListOf<Marker>()
    private var googleMapCircles = mutableListOf<Circle>()

    private var isMapInitialized = false


    fun onMoveMarker(marker: Marker?, circle: Circle?) {
        lastMarker.value?.remove()
        lastCircle.value?.remove()
        _lastMarker.value = marker
        _lastCircle.value = circle
    }

    fun onChangeSlider(num: Float) {
        _sliderPosition.value = num
        (areaRadius.value?.toDouble() ?: 0.0).also { lastCircle.value?.radius = it }
    }

    fun onAlarmNameChange(name: String) {
        _alarmName.value = name
    }

    fun onChangeAlarmType(type: AlarmType) {
        _alarmType.value = type
    }

    fun addAlarm(is_active: Boolean, context: Context){

        viewModelScope.launch {

            lastMarker.value?.let {


                val alarm = Alarm(
                    name = alarmName.value!!,
                    location = it.position,
                    radius = areaRadius.value ?: 1,
                    type = alarmType.value!!,
                    is_active = is_active,
                    created_at = Date()
                )

                repository.addAlarm(alarm)

                if (is_active) {

                    // Alarm is again retrieved from database using location since
                    // alarm id is auto generated upon insertion and the geofencing request id
                    // is the same as the id of the alarm.

                    repository.getAlarmByLocation(alarm.location)?.let { alarm1 ->
                        addGeofence(
                            geofencingClient,
                            geofencePendingIntent,
                            alarm1,
                            context,
                            success = { Log.i("addAlarm", "Geofence successfully added") },
                            failure = { error, _ -> Log.e("addAlarm", error) }
                            )
                    }
                }


            }

        }
    }


    @SuppressLint("LongLogTag", "MissingPermission")
    fun mapUpdate(googleMap: GoogleMap) {

        // Update google maps with the creation and deletion of markers

        Log.i("mapUpdate", "Updating Map")

        viewModelScope.launch {

            var isPresent: Boolean
            var index: Int
            val changedMarkers = mutableListOf<Marker>()
            var markerOptions: MarkerOptions
            var circleOptions: CircleOptions
            // LiveData alarms does not update value properly, use the suspend function inside a coroutine to get the list instead.
            val activeAlarms = repository.getActiveAlarms()

            if (!isMapInitialized) {

                googleMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
                googleMap.isMyLocationEnabled = true

                isMapInitialized = true

                Log.i("mapUpdate", "Initializing map")
            }

            if (alarms.value == null)
                return@launch



            Log.i(
                "mapUpdate",
                "activeAlarms:${activeAlarms.size} googleMapMarkers:${googleMapMarkers.size} LiveData:${alarms.value?.filter { it.is_active }?.size}"
            )

            // An alarm has been deactivated or deleted
            if (googleMapMarkers.size > activeAlarms.size) {

                for (marker in googleMapMarkers) {

                    isPresent = false

                    for (alarm in activeAlarms) {
                        if (marker.position == alarm.location)
                            isPresent = true
                    }

                    if (!isPresent)
                        changedMarkers.add(marker)
                }

                for (marker in changedMarkers) {
                    index = googleMapMarkers.lastIndexOf(marker)
                    googleMapMarkers.removeAt(index)
                    googleMapCircles.removeAt(index)
                }

            }
            // An alarm has been activated or created
            else if (googleMapMarkers.size < activeAlarms.size) {

                for (alarm in activeAlarms) {
                    isPresent = false

                    for (marker in googleMapMarkers) {
                        if (marker.position == alarm.location)
                            isPresent = true
                    }

                    if (!isPresent) {


                        if (alarm.type == AlarmType.ON_ENTRY) {
                            markerOptions = ENTER_MARKER_OPTIONS
                            circleOptions = ENTER_CIRCLE_OPTIONS
                        } else {
                            markerOptions = EXIT_MARKER_OPTIONS
                            circleOptions = EXIT_CIRCLE_OPTIONS
                        }

                        googleMapMarkers.add(
                            googleMap.addMarker(
                                markerOptions
                                    .position(alarm.location)
                                    .title(alarm.name)
                                    .snippet(
                                        ("Lat: %.4f Long: %.4f").format(
                                            alarm.location.latitude,
                                            alarm.location.longitude
                                        )
                                    )
                            )
                        )

                        googleMapCircles.add(
                            googleMap.addCircle(
                                circleOptions
                                    .center(alarm.location)
                                    .radius(alarm.radius.toDouble())
                            )
                        )
                    }

                }
            }
        }

    }

    fun goToAlarmsScreen() {
        navigationManager.navigate(Directions.Alarms)
    }

    fun onMapDestroyed() {

        isMapInitialized = false

        for (marker in googleMapMarkers) {
            marker.remove()
        }

        for (circle in googleMapCircles) {
            circle.remove()
        }

        googleMapMarkers.clear()
        googleMapCircles.clear()
    }

    fun goToChatActivity() {
        val geoIntent = Intent(context, MainActivity::class.java)

        geoIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        //  geoIntent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val bundle = Bundle()
        bundle.putBoolean("DATA", true)
        geoIntent.putExtras(bundle)
        ContextCompat.startActivity(
            context,
            geoIntent,
            bundle
        )
    }

    fun goToLoginActivity() {
        val geoIntent = Intent(context, MainActivity::class.java)
        firebaseAuth.signOut()
        geoIntent.flags =  Intent.FLAG_ACTIVITY_CLEAR_TASK
        geoIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK


    //    val bundle = Bundle()
    //    bundle.putBoolean("DATA", true)
     //   geoIntent.putExtras(bundle)
        ContextCompat.startActivity(
            context,
            geoIntent,
            null
        )
    }


    fun goToNoteActivity() {
        val geoIntent = Intent(context, MainNoteActivity::class.java)

        geoIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        //  geoIntent.addFlags( Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val bundle = Bundle()
        bundle.putBoolean("DATA", true)
        ContextCompat.startActivity(
            context,
            geoIntent,
            null
        )
    }

}