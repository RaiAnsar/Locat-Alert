package com.woxich.locatalert.model

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.android.libraries.maps.model.LatLng
import java.util.*

enum class AlarmType{
    ON_ENTRY,
    ON_EXIT
}

@Entity(
    tableName = "alarms",
    indices = [
        Index("id", unique = true)
    ]
)
@Immutable
data class Alarm (
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "name") var name: String,
    @ColumnInfo(name = "location") val location: LatLng,
    @ColumnInfo(name = "radius") var radius: Int,
    @ColumnInfo(name = "type") var type: AlarmType,
    @ColumnInfo(name = "is_active") var is_active: Boolean,
    @ColumnInfo(name = "created_at") val created_at: Date
)