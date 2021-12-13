package com.woxich.locatalert.model

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.woxich.locatalert.GeoFenceBroadcastReceiver
import com.woxich.locatalert.model.repository.NoteRepositoryImpl
import com.woxich.locatalert.model.room.DATABASE_NAME
import com.woxich.locatalert.model.room.LocatAlertDatabase
import com.woxich.locatalert.navigation.NavigationManager
import com.woxich.locatalert.repository.NoteRepository
import com.woxich.locatalert.use_case.AddNote
import com.woxich.locatalert.use_case.DeleteNote
import com.woxich.locatalert.use_case.GetNote
import com.woxich.locatalert.use_case.GetNotes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.woxich.locatalert.use_case.NoteUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging {
        return FirebaseMessaging.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return FirebaseStorage.getInstance()
    }


    @Provides
    @Singleton
    fun provideNoteRepository(db: LocatAlertDatabase): NoteRepository {
        return NoteRepositoryImpl(db.noteDao)
    }

    @Provides
    @Singleton
    fun provideAlarmRepository(db: LocatAlertDatabase): AlarmsRepository {
        return AlarmsRepository(db.alarmsDao)
    }

    @Provides
    @Singleton
    fun provideNoteUseCases(repository: NoteRepository): NoteUseCases {
        return NoteUseCases(
            getNotes = GetNotes(repository),
            deleteNote = DeleteNote(repository),
            addNote = AddNote(repository),
            getNote = GetNote(repository)
        )
    }


}

@Module
@InstallIn(SingletonComponent::class)
class RoomModule {

    @Provides
    @Singleton
    fun provideRoomDatabase(@ApplicationContext context: Context): LocatAlertDatabase {
        return Room.databaseBuilder(context, LocatAlertDatabase::class.java, DATABASE_NAME).build()
    }


//    @Provides
//    @Singleton
//    fun providesDB(@ApplicationContext appContext: Context): AlarmsDao {
//        return Room.databaseBuilder(
//            appContext.applicationContext,
//            LocatAlertDatabase::class.java,
//            "geo_alarm_database"
//        )
//            .fallbackToDestructiveMigration()
//            .build()
//            .alarmsDao
//    }


}



@Module
@InstallIn(SingletonComponent::class)
object GeoFencingModule {

    @SuppressLint("UnspecifiedImmutableFlag")
    @Provides
    @Singleton
    fun provideGeoFencingIntent(@ApplicationContext appContext: Context) : PendingIntent {
        val geofencePendingIntent: PendingIntent by lazy {
            val intent = Intent(appContext, GeoFenceBroadcastReceiver::class.java)

            // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
            // addGeofences() and removeGeofences().

            PendingIntent.getBroadcast(appContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        return geofencePendingIntent
    }

    @Provides
    @Singleton
    fun provideGeoFencingClient(@ApplicationContext appContext: Context) : GeofencingClient {
        return LocationServices.getGeofencingClient(appContext)
    }

}

//@Module
//@InstallIn(SingletonComponent::class)
//object RepositoryModule {
//
//    @Provides
//    @Singleton
//    fun provideRepository(alarmsDao: AlarmsDao) : AlarmsRepository {
//        return AlarmsRepository(alarmsDao)
//    }
//
//}

@Module
@InstallIn(SingletonComponent::class)
object NavigationModule {

    @Provides
    @Singleton
    fun providesNavigationManager() : NavigationManager {
        return NavigationManager()
    }
}



