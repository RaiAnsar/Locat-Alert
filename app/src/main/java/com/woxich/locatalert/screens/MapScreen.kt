package com.woxich.locatalert.screens

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.woxich.locatalert.utils.CURRENT_CIRCLE_OPTIONS
import com.woxich.locatalert.utils.CURRENT_MARKER_OPTIONS

import com.woxich.locatalert.utils.rememberMapViewWithLifecycle
import com.woxich.locatalert.R
import com.woxich.locatalert.model.AlarmType
import com.woxich.locatalert.model.MapScreenViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionRequired
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.libraries.maps.CameraUpdateFactory
import com.google.android.libraries.maps.model.LatLng
import com.google.android.libraries.maps.model.Marker
import com.google.android.libraries.maps.model.MarkerOptions
import com.google.maps.android.ktx.awaitMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.*



val location = MutableStateFlow<Location>(getInitialLocation())
val addressText = mutableStateOf("")
var isMapEditable = mutableStateOf(true)
var timer: CountDownTimer? = null


@SuppressLint("MissingPermission")
@Composable
fun MapViewContainer(mapViewModel: MapScreenViewModel) {

    val map = rememberMapViewWithLifecycle { mapViewModel.onMapDestroyed() }
    val gAlarms by mapViewModel.alarms.observeAsState()
    val areaRadius by mapViewModel.areaRadius.observeAsState()
    val lastMarker by mapViewModel.lastMarker.observeAsState()
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalContext.current.resources


    AndroidView({ map }) { mapView ->

        coroutineScope.launch {
            val googleMap = mapView.awaitMap()

            Log.i("MapViewContainer", "coroutineScope : Map rendered")
            Log.i("mapUpdate", "Launching mapUpdate : ${gAlarms?.size}")


            mapView.getMapAsync { map ->

                //map.uiSettings.setAllGesturesEnabled(isEnabled)

                val location = location.value
                val position = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(position,  15f))

                map.setOnCameraIdleListener {
                    val cameraPosition = map.cameraPosition
                    updateLocation(cameraPosition.target.latitude, cameraPosition.target.longitude)
                }
            }
            mapViewModel.mapUpdate(googleMap)

            googleMap.setOnMarkerClickListener {

                if (it.position == lastMarker?.position)
                    mapViewModel.onMoveMarker(null, null)
                else
                    it.showInfoWindow()

                true
            }

            googleMap.setOnMapClickListener {

                val circle = googleMap.addCircle(
                    CURRENT_CIRCLE_OPTIONS
                        .center(it)
                        .radius((areaRadius ?: 0).toDouble())

                )



                val marker = googleMap.addMarker(
                    CURRENT_MARKER_OPTIONS
                        .position(it)
                        .title(resources.getString(R.string.location))
                        .snippet(
                            resources.getString(R.string.location_present_format)
                                .format(it.latitude, it.longitude)
                        )
                )

                mapViewModel.onMoveMarker(marker, circle)
                marker.showInfoWindow()

            }

            googleMap.setOnPoiClickListener { poi ->

                val circle = googleMap.addCircle(
                    CURRENT_CIRCLE_OPTIONS
                        .center(poi.latLng)
                        .radius((areaRadius ?: 0).toDouble())

                )


                val poiMarker = googleMap.addMarker(
                    MarkerOptions()
                        .position(poi.latLng)
                        .title(poi.name)
                        .snippet(
                            resources.getString(R.string.location_present_format).format(
                                poi.latLng.latitude,
                                poi.latLng.longitude
                            )
                        )
                )

                mapViewModel.onMoveMarker(poiMarker, circle)
                poiMarker.showInfoWindow()
            }
        }

    }
}


fun getInitialLocation() : Location {
    val initialLocation = Location("")
    initialLocation.latitude = 51.506874
    initialLocation.longitude = -0.139800
    return initialLocation
}

fun updateLocation(latitude: Double, longitude: Double){
    if(latitude != location.value.latitude) {
        val location = Location("")
        location.latitude = latitude
        location.longitude = longitude
        setLocation(location)
    }
}

fun setLocation(loc: Location) {
    location.value = loc
}

fun getAddressFromLocation(context: Context): String {
    val geocoder = Geocoder(context, Locale.getDefault())
    var addresses: List<Address>? = null
    val address: Address?
    var addressText = ""

    try {
        addresses = geocoder.getFromLocation(location.value.latitude, location.value.longitude, 1)
    }catch(ex: Exception){
        ex.printStackTrace()
    }

    address = addresses?.get(0)
    addressText = address?.getAddressLine(0) ?: ""


    return addressText
}

fun onTextChanged(context: Context, text: String){
    if(text == "")
        return
    timer?.cancel()
    timer = object : CountDownTimer(1000, 1500) {
        override fun onTick(millisUntilFinished: Long) { }
        override fun onFinish() {
            location.value = getLocationFromAddress(context, text)
        }
    }.start()
}

fun getLocationFromAddress(context: Context, strAddress: String): Location {
    val geocoder = Geocoder(context, Locale.getDefault())
    val addresses: List<Address>?
    val address: Address?

    addresses = geocoder.getFromLocationName(strAddress, 1)

    if (addresses.isNotEmpty()) {
        address = addresses[0]

        var loc = Location("")
        loc.latitude = address.getLatitude()
        loc.longitude = address.getLongitude()
        return loc
    }

    return location.value
}





@RequiresApi(Build.VERSION_CODES.M)
@SuppressLint("InlinedApi")
@ExperimentalPermissionsApi
@Composable
fun MarkerSaveMenu(mapViewModel: MapScreenViewModel) {

    val alarmName by mapViewModel.alarmName.observeAsState("")
    val areaRadius by mapViewModel.areaRadius.observeAsState()
    val sliderValue by mapViewModel.sliderPosition.observeAsState(0f)
    val alarmType by mapViewModel.alarmType.observeAsState(AlarmType.ON_ENTRY)

    val context = LocalContext.current
    val resources = context.resources
    val theme = context.theme

    val bgLocationPermissionState =
        rememberPermissionState(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)


    PermissionRequired(
        permissionState = bgLocationPermissionState,
        permissionNotGrantedContent = { BackgroundLocationAccessRationale(bgLocationPermissionState) },
        permissionNotAvailableContent = { BackgroundLocationAccessDenied(context) }
    ) {

        Column(
            modifier = Modifier
                .background(Color(resources.getColor(R.color.dark_blue, theme)))
                .wrapContentHeight()
        ) {

            OutlinedTextField(
                value = alarmName,
                onValueChange = { mapViewModel.onAlarmNameChange(it) },
                label = { Text(resources.getString(R.string.alarm_name_label)) },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color.White,
                    cursorColor = Color.White,
                    unfocusedBorderColor = Color.LightGray,
                    unfocusedLabelColor = Color.DarkGray,
                    focusedBorderColor = Color.LightGray,
                    focusedLabelColor = Color.LightGray
                ),
                modifier = Modifier
                    .padding(20.dp, 10.dp)
                    .fillMaxWidth()
            )

            Text(
                buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(resources.getString(R.string.radius_label))
                    }
                    append(resources.getString(R.string.radius_format).format(areaRadius))

                },
                color = Color.White,
                modifier = Modifier.padding(23.dp, 10.dp, 10.dp, 0.dp)
            )

            Slider(
                value = sliderValue,
                onValueChange = { mapViewModel.onChangeSlider(it) },
                modifier = Modifier.padding(15.dp, 0.dp, 15.dp, 5.dp),
                colors = SliderDefaults.colors(
                    thumbColor = Color(resources.getColor(R.color.light_purple, theme)),
                    activeTrackColor = Color(resources.getColor(R.color.light_purple, theme)),
                    inactiveTrackColor = Color(resources.getColor(R.color.dark_purple, theme))
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 0.dp, 0.dp, 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(
                    onClick = { mapViewModel.onChangeAlarmType(AlarmType.ON_ENTRY) },
                    modifier = Modifier
                        .background(
                            if (alarmType == AlarmType.ON_ENTRY) Color(
                                resources.getColor(
                                    R.color.light_green,
                                    theme
                                )
                            ) else Color.White
                        )
                        .fillMaxWidth(0.35F)
                ) {
                    Text(
                        resources.getString(R.string.entry),
                        color = if (alarmType == AlarmType.ON_ENTRY) Color.White else Color.Black
                    )

                }
                TextButton(
                    onClick = { mapViewModel.onChangeAlarmType(AlarmType.ON_EXIT) },
                    modifier = Modifier
                        .background(
                            if (alarmType == AlarmType.ON_EXIT) Color(
                                resources.getColor(
                                    R.color.light_green,
                                    theme
                                )
                            ) else Color.White
                        )
                        .fillMaxWidth(0.5F)
                ) {
                    Text(
                        resources.getString(R.string.exit),
                        color = if (alarmType == AlarmType.ON_EXIT) Color.White else Color.Black
                    )

                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {

                IconButton(onClick = { mapViewModel.onMoveMarker(null, null) }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = resources.getString(R.string.cancel),
                        tint = Color(resources.getColor(R.color.light_red, theme))
                    )
                }

                TextButton(onClick = {
                    mapViewModel.addAlarm(false, context)
                    mapViewModel.onMoveMarker(null, null)
                }) {
                    Text(
                        resources.getString(R.string.save),
                        color = Color(resources.getColor(R.color.light_purple, theme))
                    )
                }

                TextButton(onClick = {
                    mapViewModel.addAlarm(true, context)
                    mapViewModel.onMoveMarker(null, null)
                }) {
                    Text(
                        resources.getString(R.string.start),
                        color = Color(resources.getColor(R.color.light_green, theme))
                    )
                }

            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.M)
@ExperimentalPermissionsApi
@ExperimentalAnimationApi
@Composable
fun MainMapScreen(
    mapViewModel: MapScreenViewModel = hiltViewModel()
) {

    //Store context
    val context = LocalContext.current
    val resources = context.resources
    val theme = context.theme
    var text by remember { addressText }

    val lastMarker: Marker? by mapViewModel.lastMarker.observeAsState()

    val locationPermissionState =
        rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    Log.i("Screen", "MainMapScreen")

    PermissionRequired(
        permissionState = locationPermissionState,
        permissionNotGrantedContent = { LocationAccessRationale(locationPermissionState) },
        permissionNotAvailableContent = { LocationAccessDenied(context) }
    ) {

        Scaffold(
            topBar = {
                HomeTopBar(

                    onMenuClicked = {
                        mapViewModel.goToAlarmsScreen()
                        mapViewModel.onMoveMarker(null, null)
                    },
                    onChatClicked = {
                        mapViewModel.goToChatActivity()
                    },
                    onNoteClicked = {
                        mapViewModel.goToNoteActivity()
                    },
                    onLogoutClicked = {
                        mapViewModel.goToLoginActivity()
                    }
                )

            }
        ) {

            Column(Modifier.fillMaxWidth()) {
                Box {
                    Box {
                        TextField(
                            value = text,
                            onValueChange = {
                                text = it
                                if (!isMapEditable.value)
                                    onTextChanged(context, text)
                            },
                            modifier = Modifier.fillMaxWidth().padding(end = 0.1.dp),
                            enabled = !isMapEditable.value,
                            colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent)
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp)
                                .padding(bottom = 0.5.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Button(
                                onClick = {
                                    isMapEditable.value = !isMapEditable.value
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF131E37))
                            ) {
                                Text(text = if (isMapEditable.value) "Edit" else "Search" ,color = Color.White)
                            }
                        }

                    }
                }
                Box {

                Box {
                    MapViewContainer(mapViewModel = mapViewModel)
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .align(Alignment.BottomCenter)
                ) {

                    androidx.compose.animation.AnimatedVisibility(
                        visible = (lastMarker != null),
                        enter = fadeIn(animationSpec = tween(durationMillis = 1000)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 1000))
                    ) {

                        MarkerSaveMenu(mapViewModel)
                    }
                }
            }
        }
        }
    }


}


@Composable
fun HomeTopBar(
    onMenuClicked: () -> Unit,
    onChatClicked: () -> Unit,
    onNoteClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
) {


    TopAppBar(
        backgroundColor = Color(0xFF131E37),
        title = {
            Text(
                text = stringResource(
                    id = R.string.app_name
                ), color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClicked) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = stringResource(id = R.string.menu),
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = onChatClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.icon_chat),
                    contentDescription = stringResource(id = R.string.chat_title),
                    tint = Color.White
                )
            }
            IconButton(onClick = onNoteClicked) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_baseline_speaker_notes_24),
                    contentDescription = stringResource(id = R.string.note_title),
                    tint = Color.White
                )
            }
            IconButton(onClick = onLogoutClicked) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = stringResource(id = R.string.profile_log_out),
                    tint = Color.White
                )
            }

        }
    )
}


// TODO : make notification vibrate and emit a sound with a full screen activity

