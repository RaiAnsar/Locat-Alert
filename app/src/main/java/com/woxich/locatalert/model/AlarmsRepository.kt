package com.woxich.locatalert.model

import androidx.lifecycle.LiveData
import com.woxich.locatalert.model.room.AlarmsDao
import com.google.android.libraries.maps.model.LatLng
import javax.inject.Inject

class AlarmsRepository @Inject constructor(private val alarmsDao : AlarmsDao) {

    fun getAlarmsLive() : LiveData<List<Alarm>> {
        return alarmsDao.getAllAlarmsLive()
    }

    fun isAlarmActiveLive(id: Int) : LiveData<Boolean> {
        return alarmsDao.isAlarmActive(id)
    }

    suspend fun addAlarm(alarm: Alarm) {
        alarmsDao.insert(alarm)
    }

    suspend fun getAlarmByLocation(location: LatLng) : Alarm? {
        return alarmsDao.get(location)
    }

    suspend fun getAlarmById(id: Int) : Alarm? {
        return alarmsDao.get(id)
    }

    suspend fun getActiveAlarms() : List<Alarm> {
        return alarmsDao.getActiveAlarms()
    }

    suspend fun updateAlarm(alarm: Alarm) {
        alarmsDao.update(alarm)
    }

    suspend fun deleteAlarm(alarm: Alarm){
        alarmsDao.delete(alarm)
    }


}