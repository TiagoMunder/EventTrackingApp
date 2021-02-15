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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import org.w3c.dom.Document;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static pt.ubi.eventtrackingapp.Constants.EVENTSCOLLECTION;
import static pt.ubi.eventtrackingapp.Constants.POSITIONSCOLLECTION;
import static pt.ubi.eventtrackingapp.Constants.USERPOSITIONS;
import static pt.ubi.eventtrackingapp.Constants.USERSCOLLECTION;

public class LocationTrackingService extends Service {
    private final static String TAG = "LocationTrackingService";
    private FusedLocationProviderClient mFusedLocationClient;
    private final static long UPDATE_INTERVAL = 5 * 1000;  /* 5 secs */
    private final static long FASTEST_INTERVAL = 2 * 1000; /* 2 sec */
    private final static int MIN_DISTANCE = 5; /* 5 meters */
    private Session session;
    private Location lastLocation;
    private static  final String CHANNEL_ID = "channel_location_tracking_1";
    private DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private Trace time_to_update_first_distance_trace;
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
        time_to_update_first_distance_trace = FirebasePerformance.getInstance().newTrace("time_to_update_first_distance");
        time_to_update_first_distance_trace.start();
        getLocation();
        return START_NOT_STICKY; // this will make the service run while the getLocation is running
    }

    public void updateDistanceTraveled( DocumentReference documentReference, DocumentSnapshot document , Location dist) {
        GeoPoint origen = getLastPosition(document);
        float distanceAlreadyTraveled = getDistanceTraveled(document);
        float newDistanceTraveled = getmetersToLocation(dist, origen.getLatitude(), origen.getLongitude());
        documentReference.update("distanceTraveled", (distanceAlreadyTraveled + newDistanceTraveled));
        time_to_update_first_distance_trace.stop();
        session.setCurrentDistanceTraveled(String.valueOf(newDistanceTraveled));

    }

    public void updateVelocityandDuration(final DocumentReference documentReference ,final Location dist,final Long  currentTime) {
        documentReference.collection(POSITIONSCOLLECTION)
                .orderBy("time", Query.Direction.valueOf("ASCENDING"))
                .limit(1)
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if( task.getResult().getDocuments().size() == 1) {
                    UserPosition firstPosition = task.getResult().getDocuments().get(0).toObject(UserPosition.class);
                    long secondsBetweenPoints =((( currentTime) - ( Long.parseLong(firstPosition.getTime()))) / 1000);
                    float distanceTraveled = getmetersToLocation(dist, firstPosition.getGeoPoint().getLatitude(), firstPosition.getGeoPoint().getLongitude());
                    documentReference.update("velocity", String.valueOf( decimalFormat.format(distanceTraveled/secondsBetweenPoints)));
                    documentReference.update("duration", String.valueOf( decimalFormat.format(secondsBetweenPoints/ 60)));

                }

            }
        });

    }

    private void addUserPosition(final Location location, final CustomGeoPoint geoPoint) {
        // final String key =  user.getUser_id() + '_' + EventID;
       DocumentReference eventCollectionRef = FirebaseFirestore.getInstance().collection(EVENTSCOLLECTION).document(session.getEvent().getEventID());
        eventCollectionRef.collection(USERSCOLLECTION).whereEqualTo("user_id", session.getUser().getUser_id()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.getResult().getDocuments().size() != 0 ) {
                    final DocumentReference  documentReference = task.getResult().getDocuments().get(0).getReference();
                    DocumentSnapshot document = task.getResult().getDocuments().get(0);
                    documentReference.collection(USERPOSITIONS).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.getResult().getDocuments().size() != 0 ) {
                                DocumentReference documentReference = task.getResult().getDocuments().get(0).getReference();
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                if(checkUserMoved(document, location)) {
                                    Long tsLong = System.currentTimeMillis();
                                    UserPosition customGeoPoint  = new UserPosition(location.getLatitude(), location.getLongitude(), tsLong.toString());
                                    documentReference.collection(POSITIONSCOLLECTION).add(customGeoPoint);
                                    documentReference.update("lastPosition",convertLocationToCustomGeoPoint(location));
                                    updateDistanceTraveled(documentReference, document, location);
                                    updateVelocityandDuration(documentReference, location, tsLong);
                                }
                            }else {
                                UserLocationPositionsInEvent docInfo = new UserLocationPositionsInEvent( 0, geoPoint, 0, 0);
                                documentReference.collection(USERPOSITIONS).add(docInfo).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentReference> task) {
                                        Long tsLong = System.currentTimeMillis();
                                        UserPosition customGeoPoint = new UserPosition(location.getLatitude(), location.getLongitude(), tsLong.toString());
                                        task.getResult().collection(POSITIONSCOLLECTION).add(customGeoPoint);
                                    }
                                });
                            }
                        }
                    });

                }else {
                    Log.d(TAG, "Error finding User in Event!" );

                }
            }
        });

    }

    private void getLocation() {

        // ---------------------------------- LocationRequest ------------------------------------
        // Create the location request to start receiving updates
        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(UPDATE_INTERVAL);
        mLocationRequestHighAccuracy.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequestHighAccuracy.setSmallestDisplacement(MIN_DISTANCE);


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

                        if (location != null && !session.getEvent().isClosed()) {
                            User user = session.getUser();
                            CustomGeoPoint geoPoint = new CustomGeoPoint(location.getLatitude(), location.getLongitude());
                            session.setCurrentLocation(geoPoint);
                            UserLocation userLocation = new UserLocation(geoPoint, null, user);
                            saveUserLocation(userLocation);
                            addUserPosition(location, geoPoint);
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
            return Float.parseFloat(document.getData().get("distanceTraveled").toString());
        }
        return 0;

    }



}
