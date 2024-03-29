package com.woxich.locatalert.model.room

import androidx.room.TypeConverter
import com.woxich.locatalert.model.AlarmType
import com.google.android.libraries.maps.model.LatLng
import java.util.*

class TypeConverters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun latLngToString(location: LatLng?): String {
        return "${location?.latitude},${location?.longitude}"
    }

    @TypeConverter
    fun fromLatLngString(string: String?): LatLng?{

        if (string == null) return null

        val latLong = string.split(",").toTypedArray()
        val latitude = latLong[0].toDouble()
        val longitude = latLong[1].toDouble()

        return LatLng(latitude, longitude)
    }

    @TypeConverter
    fun alarmTypeToInt(type: AlarmType?): Int? {
        return type?.ordinal
    }

    @TypeConverter
    fun fromOrdinal(num:Int?): AlarmType? {

        return num?.let{ AlarmType.values()[it] }
    }
}