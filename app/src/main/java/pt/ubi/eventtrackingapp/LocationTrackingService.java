package pt.ubi.eventtrackingapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationTrackingService extends Service {
    private final static String TAG = "LocationTrackingService";
    private FusedLocationProviderClient mFusedLocationClient;
    private final static long UPDATE_INTERVAL = 5 * 1000;  /* 5 secs */
    private final static long FASTEST_INTERVAL = 2 * 1000; /* 2 sec */
    private Session session;
    private Location lastLocation;
    private static  final String CHANNEL_ID = "channel_location_tracking_1";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        session = new Session(LocationTrackingService.this);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Event Tracking!",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Event Tracking App")
                    .setContentText("Location Tracking").build();

            startForeground(1, notification);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: called.");
        getLocation();
        return START_NOT_STICKY; // this will make the service run while the getLocation is running
    }

    private void getLocation() {

        // ---------------------------------- LocationRequest ------------------------------------
        // Create the location request to start receiving updates
        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(UPDATE_INTERVAL);
        mLocationRequestHighAccuracy.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequestHighAccuracy.setSmallestDisplacement(10);


        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "getLocation: stopping the location service.");
            stopSelf();
            return;
        }

        Log.d(TAG, "getLocation: getting location information.");
        mFusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {

                        Log.d(TAG, "onLocationResult: got location result.");

                        Location location = locationResult.getLastLocation();

                        if (location != null ) {
                            User user = session.getUser();
                            session.setCurrentLocation(location);
                            CustomGeoPoint geoPoint = new CustomGeoPoint(location.getLatitude(), location.getLongitude());
                            UserLocation userLocation = new UserLocation(geoPoint, null, user);
                            saveUserLocation(userLocation);
                            addUserPosition(user, session.getEvent().getEventID(), location, geoPoint);
                        }
                    }
                },
                Looper.myLooper()); // Looper.myLooper tells this to repeat forever until thread is destroyed
    }

    // not using this for now
    private boolean getLocationChangedState(Location location) {
        if(lastLocation !=null) {
            if(lastLocation.getLatitude() == location.getLatitude() && lastLocation.getLongitude() == location.getLongitude())
                return false;
        }
        lastLocation = location;
        return true;
    }

    private void saveUserLocation(final UserLocation userLocation) {

        try{
            DocumentReference locationRef = FirebaseFirestore.getInstance()
                    .collection(getString(R.string.fire_store_users_locations))
                    .document(FirebaseAuth.getInstance().getUid());

            locationRef.set(userLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG, "onComplete: \ninserted user location into database." +
                                "\n latitude: " + userLocation.getGeoPoint().getLatitude() +
                                "\n longitude: " + userLocation.getGeoPoint().getLongitude());
                    }
                }
            });
        }catch (NullPointerException e){
            Log.e(TAG, "saveUserLocation: User instance is null, stopping location service.");
            Log.e(TAG, "saveUserLocation: NullPointerException: "  + e.getMessage() );
            stopSelf();
        }

    }

    private boolean comparePositions( Location location, GeoPoint geoPoint) {
        if(geoPoint == null)
            return false;
        return geoPoint.getLongitude() != location.getLongitude() || geoPoint.getLatitude() != location.getLatitude();
    }

    private GeoPoint getLastPosition(DocumentSnapshot document) {
        if(document.getData() != null){
            return (GeoPoint) document.getData().get("lastPosition");
        }
        return null;
    }

    private boolean checkUserMoved(DocumentSnapshot document, Location location) {
        GeoPoint geoPoint = getLastPosition(document);
            return comparePositions(location, geoPoint);
    }

    private CustomGeoPoint convertLocationToCustomGeoPoint(Location location) {
        return new CustomGeoPoint(location.getLatitude(), location.getLongitude());

    }

    public static float getmetersToLocation(Location dist, double latOrigen, double longOrigen){

        Location origen = new Location("");
        origen.setLatitude(latOrigen);
        origen.setLongitude(longOrigen);
        float distanceInMeters = origen.distanceTo(dist);
        return distanceInMeters;
    }

    public static float getDistanceTraveled(DocumentSnapshot document) {
        if(document.getData() != null){
            return (float) document.getData().get("distanceTraveled");
        }
        return 0;

    }

    public void updateDistanceTraveled( DocumentReference documentReference, DocumentSnapshot document , Location dist) {
        GeoPoint origen = getLastPosition(document);
        float distanceAlreadyTraveled = getDistanceTraveled(document);
        float newDistanceTraveled = getmetersToLocation(dist, origen.getLatitude(), origen.getLongitude());
        documentReference.update("distanceTraveled", (distanceAlreadyTraveled + newDistanceTraveled));

    }


    private void addUserPosition(final User user, final String EventID,final Location location, final CustomGeoPoint geoPoint) {
        final String key =  user.getUser_id() + '_' + EventID;
         FirebaseFirestore.getInstance().collection("UserPositionsInEvent").whereEqualTo("userPositionKey", key).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                 @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.getResult().getDocuments().size() != 0 ) {
                            DocumentReference documentReference = task.getResult().getDocuments().get(0).getReference();
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            if(checkUserMoved(document, location)) {
                                Long tsLong = System.currentTimeMillis()/1000;
                                UserPosition customGeoPoint  = new UserPosition(location.getLatitude(), location.getLongitude(), tsLong.toString());
                                documentReference.collection("UserPosition").add(customGeoPoint);
                                documentReference.update("lastPosition",convertLocationToCustomGeoPoint(location));
                                updateDistanceTraveled(documentReference, document, location);
                            }
                        }else {
                            UserLocationPositionsInEvent docInfo = new UserLocationPositionsInEvent(key, 0, geoPoint);
                            FirebaseFirestore.getInstance().collection("UserPositionsInEvent").add(docInfo).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                               @Override
                               public void onComplete(@NonNull Task<DocumentReference> task) {
                                   Long tsLong = System.currentTimeMillis()/1000;
                                   UserPosition customGeoPoint = new UserPosition(location.getLatitude(), location.getLongitude(), tsLong.toString());
                                   task.getResult().collection("UserPosition").add(customGeoPoint);
                               }
                           });
                        }
                    }
                });

    }



}
