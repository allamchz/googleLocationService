package cr.ac.una.googlelocationservice

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient


class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var notificationManager: NotificationManager
    private var contNotificacion =2
    private lateinit var   buttonIntent :Intent
    private lateinit var buttonPendingIntent: PendingIntent

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        buttonIntent = Intent(this, MainActivity::class.java).apply {
            action = "OPEN_FRAGMENT"
            putExtra("param_key", "param_value")
        }
        buttonPendingIntent = PendingIntent.getActivity(this, 0, buttonIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        Places.initialize(applicationContext, "AIzaSyBLiFVeg7U_Ugu5bMf7EQ_TBEfPE3vOSF4")
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        this.startForeground(1, createNotification("Service running"))

        requestLocationUpdates()


    }

    private fun createNotificationChannel() {

        val serviceChannel = NotificationChannel(
            "locationServiceChannel",
            "Location Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(serviceChannel)

    }




    private fun createNotification(message: String): Notification {
        return NotificationCompat.Builder(this, "locationServiceChannel")
            .setContentTitle("Location Service")
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .build()
    }

    private fun requestLocationUpdates() {


        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10000
        ).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()


        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach { location ->
                getPlaceName(location.latitude, location.longitude)
            }
        }
    }


    @SuppressLint("MissingPermission")
    private fun  getPlaceName(latitude: Double, longitude: Double) {

        val placeFields: List<Place.Field> = listOf(Place.Field.NAME)


        val request: FindCurrentPlaceRequest = FindCurrentPlaceRequest.newInstance(placeFields)
        val placesClient: PlacesClient = Places.createClient(this)


        val placeResponse = placesClient.findCurrentPlace(request)
        placeResponse.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val response = task.result
                val topPlaces = response.placeLikelihoods
                    .sortedByDescending { it.likelihood }
                    .take(2)
                topPlaces.forEach { placeLikelihood ->
                    sendNotification("Lugar: ${placeLikelihood.place.name}, Probabilidad: ${placeLikelihood.likelihood}")
                    println("Lugar: ${placeLikelihood.place.name}, Probabilidad: ${placeLikelihood.likelihood}")
                }
            } else {
                val exception = task.exception
                if (exception is ApiException) {
                    Log.e(TAG, "Lugar no encontrado: ${exception.statusCode}")
                }
            }
        }
    }

    private fun sendNotification(message: String) {
        contNotificacion++
        val notificationLayout = RemoteViews(packageName, R.layout.notificacion).apply {
            setTextViewText(R.id.title, "Punto de interés")
            setTextViewText(R.id.message, message)
            setOnClickPendingIntent(R.id.boton_notificacion, buttonPendingIntent)
        }
        val notification = NotificationCompat.Builder(this, "locationServiceChannel")
            .setSmallIcon(com.google.android.material.R.drawable.ic_m3_chip_check)
            .setCustomContentView(notificationLayout)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(contNotificacion, notification)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

/**
 * onCreate del main activity
 *
 *         if (intent?.action == "OPEN_FRAGMENT") {
 *             val paramValue = intent.getStringExtra("param_key")
 *             openSpecificFragment(paramValue)
 *         }
 *
 *         private fun openSpecificFragment(paramValue: String?) {
 *         val specificFragment = SpecificFragment().apply {
 *             arguments = Bundle().apply {
 *                 putString("param_key", paramValue)
 *             }
 *         }
 *
 *         supportFragmentManager.beginTransaction()
 *             .replace(R.id.fragment_container, specificFragment)
 *             .commit()
 *     }
 *
 *     onCreatedView del fragmento
 *
 *
 *         arguments?.getString("param_key")?.let { paramValue ->
 *             val textView = view.findViewById<TextView>(R.id.param_text_view)
 *             textView.text = paramValue
 *         }
 *
 */
