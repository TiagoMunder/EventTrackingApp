package pt.ubi.eventtrackingapp;

import android.Manifest;
import android.app.ActivityManager;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;


import android.os.Bundle;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;

import static pt.ubi.eventtrackingapp.Constants.EVENTSCOLLECTION;
import static pt.ubi.eventtrackingapp.Constants.GOOGLE_MAP_API_KEY;
import static pt.ubi.eventtrackingapp.Constants.IMAGEMARKERSCOLLECTION;
import static pt.ubi.eventtrackingapp.Constants.POSITIONSCOLLECTION;
import static pt.ubi.eventtrackingapp.Constants.USERPOSITIONS;
import static pt.ubi.eventtrackingapp.Constants.USERSCOLLECTION;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
        MarkerFragment.OnFragmentInteractionListener, MapFooterFragment.OnFragmentInteractionListener,ImageTabsFragment.OnFragmentInteractionListener,GoogleMap.OnMapLongClickListener, MapFooterFragment.ButtonCallback{

    private static final String TAG = "MapFragmentActivity";
    private static final int slowPathMaxTimeInMin = 5;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;
    private static final int CHECK_USER_POSITIONS_INTERVAL = 3000;

    private FirebaseFirestore mDb;
    private ArrayList<UserLocationParcelable> mUserLocations = new ArrayList<>();
    private ArrayList<MarkerObject> mImageMarkersList = new ArrayList<>();
    private GoogleMap mGoogleMap;
    private UserLocationParcelable mUserLocation;
    private myClusterManagerRenderer clusterManagerRenderer;
    private ClusterManager<MyClusterItem> mClusterManager;
    private ArrayList<MyClusterItem> mClusterItems= new ArrayList<>();
    private ImageMarkerClusterManagerRenderer imageClusterManagerRenderer;
    private ClusterManager<ImageMarkerClusterItem> mImageMarkerClusterManager;
    private ArrayList<ImageMarkerClusterItem> mImageMarkersClusterItems= new ArrayList<>();
    private ArrayList<Marker> temporaryMarkers = new ArrayList<>();
    private ArrayList<UserPosition> myPositions = new ArrayList<>();
    private ArrayList<MarkerObject> filteredImages = new ArrayList<>();

    private ListenerRegistration listenUsers, listenUserPositions, listenerImages;

    private Handler mHandler = new Handler();
    private Runnable mRunnable;

    private Handler mhandlePositionsListener = new Handler();
    private Runnable mRunPositionsListener;

    private String eventID;

    private boolean isOnMarkerFragment = false;
    private SupportMapFragment mapFragment;
    private LinearLayout child1_Linear_layout;

    private boolean isUserLocationRunning = false;

    private MyClusterItem currentClusterItem;

    private boolean drawFirstTime = true;

    private Session session;
    private DecimalFormat decimalFormat = new DecimalFormat("#");

    private static final int PATTERN_GAP_LENGTH_PX = 20;
    private ArrayList<Polyline>  polyLines = new ArrayList<Polyline>();
    private static final PatternItem DOT = new Dot();
    private static final PatternItem GAP = new Gap(PATTERN_GAP_LENGTH_PX);
    // Create a stroke pattern of a gap followed by a dot.
    private static final List<PatternItem> PATTERN_POLYLINE_DOTTED = Arrays.asList(GAP, DOT);

    private boolean isEventClosed, newImageAdded = false;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        child1_Linear_layout =(LinearLayout)findViewById(R.id.map_container);
        Intent intent = getIntent();
        eventID = intent.getStringExtra("eventID");
        mUserLocations = intent.getParcelableArrayListExtra("UserLocations");
         mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mDb = FirebaseFirestore.getInstance();
        session = new Session(MapActivity.this);
        startLocationService();
        isEventClosed = session.getEvent().isClosed();
    }

    protected void onStart() {
        super.onStart();
        if(!isUserLocationRunning)
            startUserLocationsRunnable();
        if(listenUserPositions == null)
            startCheckingUserPositions();
    }

    protected void onResume() {
        super.onResume();
        if(mClusterManager != null) mClusterManager.clearItems();
        if(mImageMarkerClusterManager != null) mImageMarkerClusterManager.clearItems();
        if(!isUserLocationRunning)
            startUserLocationsRunnable();
    }

    protected void onPause() {
        super.onPause();
        stopLocationUpdates();

    }

    protected void onDestroy() {
        super.onDestroy();
        listenerImages.remove();
        listenUserPositions.remove();
        listenUsers.remove();
        currentClusterItem = null;

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mGoogleMap = googleMap;
        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                getUsersOfTheEvent();
                if(listenerImages == null)
                 getImageMarkers();
                if(isEventClosed) {
                    updateMyCurrentPosition();
                }

            }
        });



        if(mImageMarkerClusterManager == null){
            mImageMarkerClusterManager = new ClusterManager<ImageMarkerClusterItem>(getApplicationContext(), mGoogleMap);
        }

        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnMapLongClickListener(this);
        mGoogleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {

            @Override
            public View getInfoWindow(Marker arg0) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                LinearLayout info = new LinearLayout(MapActivity.this);
                info.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(MapActivity.this);
                title.setTextColor(Color.BLACK);
                title.setGravity(Gravity.CENTER);
                title.setTypeface(null, Typeface.BOLD);
                title.setText(marker.getTitle());

                TextView snippet = new TextView(MapActivity.this);
                snippet.setTextColor(Color.GRAY);
                snippet.setText(marker.getSnippet());

                info.addView(title);
                info.addView(snippet);

                return info;
            }
        });
    }



    private boolean checkUserIsCurrentUser(String userId) {
        return userId.equals(FirebaseAuth.getInstance().getUid());
    }

    private void addMapMarkersDynamically(UserLocationParcelable userLocationParcelable) {

        if(mGoogleMap != null){

            if(mClusterManager == null){
                mClusterManager = new ClusterManager<MyClusterItem>(getApplicationContext(), mGoogleMap);
            }
            if(clusterManagerRenderer == null){
                clusterManagerRenderer = new myClusterManagerRenderer(
                        this,
                        mGoogleMap,
                        mClusterManager
                );
                mClusterManager.setRenderer(clusterManagerRenderer);
            }

            Log.d(TAG, "addMapMarkers: location: " + userLocationParcelable.getGeoPoint().toString());
            try{
                String snippet = "";
                String avatar = null;
                avatar = userLocationParcelable.getUser().getmImageUrl();
                MyClusterItem newClusterMarker = new MyClusterItem(
                        new LatLng(userLocationParcelable.getGeoPoint().getLatitude(), userLocationParcelable.getGeoPoint().getLongitude()),
                        userLocationParcelable.getUser().getUsername(),
                        snippet,
                        avatar,
                        userLocationParcelable.getUser()
                );
                if(checkUserIsCurrentUser(userLocationParcelable.getUser().getUser_id()) || !isEventClosed) {
                    mClusterManager.addItem(newClusterMarker);
                    mClusterItems.add(newClusterMarker);

                }
                if(checkUserIsCurrentUser(userLocationParcelable.getUser().getUser_id()))
                    currentClusterItem = newClusterMarker;
            }catch (NullPointerException e){
                Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage() );
            }

            setUserPosition();
            mClusterManager.cluster();
            updateDistanceTraveled();

        }
    }

    public void getUserLocation(User user) {
        DocumentReference locationDocumentRef = mDb.collection("User Locations").document(user.getUser_id());

        locationDocumentRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    if(task.getResult().toObject(UserLocation.class) != null) {
                        UserLocation oldUserLocation = task.getResult().toObject(UserLocation.class);
                        CustomGeoPoint geoPoint = new CustomGeoPoint(oldUserLocation.getGeoPoint().getLatitude(),oldUserLocation.getGeoPoint().getLongitude());
                        User user = new User(oldUserLocation.getUser().getEmail(), oldUserLocation.getUser().getUsername(),oldUserLocation.getUser().getUser_id(),oldUserLocation.getUser().getmImageUrl().toString());
                        UserLocationParcelable newUserLocation = new UserLocationParcelable(geoPoint, task.getResult().toObject(UserLocation.class).getTimestamp(), user);
                        mUserLocations.add(newUserLocation);
                        addMapMarkersDynamically(newUserLocation);

                    }
                }
            }
        });
    }

    private void getUsersOfTheEvent() {
        listenUsers = mDb.collection("Events").document(eventID).collection("Users")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        mUserLocations.clear();

                        for (QueryDocumentSnapshot doc : value) {

                            if (doc.get("username") != null && doc.get("email") != null) {

                                User user = new User(doc.get("email").toString(), doc.get("username").toString(),doc.get("user_id").toString(),doc.get("mImageUrl")!=null ? doc.get("mImageUrl").toString() : null);
                                getUserLocation(user);
                            }
                        }

                    }
                });

    }

    private void filterImagesByGeoPoint(GeoPoint geoPoint, Marker marker, ImageMarkerClusterItem imageMarker, MyClusterItem userMarker) {
        filteredImages.clear();
        for (MarkerObject image : mImageMarkersList) {
            if(image.getGeoPoint().equals(geoPoint))
                filteredImages.add(image);
        }
        addMapFooter( marker, imageMarker, userMarker);
    }

    private boolean checkIfAnotherMarkerInThisPosition(CustomGeoPoint newMarkerPosition) {
        LatLng newMarkerLat = new LatLng(newMarkerPosition.getLatitude(), newMarkerPosition.getLongitude());
        for(ImageMarkerClusterItem imageMarker: mImageMarkersClusterItems) {
            if(imageMarker.getPosition().equals(newMarkerLat))
                return true;
        }
        for(MyClusterItem userMarker: mClusterItems) {
            if(userMarker.getPosition().equals(newMarkerLat))
                return true;
        }
        return false;
    }

    private void addImageMarkers() {
        if(mGoogleMap != null){

            if(imageClusterManagerRenderer == null){
                imageClusterManagerRenderer = new ImageMarkerClusterManagerRenderer(
                        this,
                        mGoogleMap,
                        mImageMarkerClusterManager
                );
                mImageMarkerClusterManager.setRenderer(imageClusterManagerRenderer);
            } else {
                mImageMarkerClusterManager.clearItems();
                imageClusterManagerRenderer.clearExtraMarkerInfo();
                mImageMarkersClusterItems.clear();
            }

            for(MarkerObject imageMarker: mImageMarkersList){
                if(checkIfAnotherMarkerInThisPosition(imageMarker.getGeoPoint()))
                    continue;
                Log.d(TAG, "addMapMarkers: location: " + imageMarker.getGeoPoint().toString());
                try{
                    String snippet = !imageMarker.getDescription().isEmpty() ? imageMarker.getDescription() : "";

                    String avatar = imageMarker.getImageUrl();

                    String title = imageMarker.getImageName() != null ? imageMarker.getImageName() : "";

                    ImageMarkerClusterItem newClusterMarker = new ImageMarkerClusterItem(
                            new LatLng(imageMarker.getGeoPoint().getLatitude(), imageMarker.getGeoPoint().getLongitude()),
                            title,
                            snippet,
                            avatar,
                            imageMarker.getEventId(),
                            imageMarker.getUser_id(),
                            imageMarker.getDescription(),
                            imageMarker.getId()
                    );
                    mImageMarkerClusterManager.addItem(newClusterMarker);
                    mImageMarkersClusterItems.add(newClusterMarker);



                }catch (NullPointerException e){
                    Log.e(TAG, "addImageMapMarkers: NullPointerException: " + e.getMessage() );
                }

            }
            mImageMarkerClusterManager.cluster();
        }
    }

    private void setUserPosition() {
        for(UserLocationParcelable userL: mUserLocations){
            if(userL.getUser().getUser_id().equals(session.getUser().getUser_id())) {
                mUserLocation = userL;
                setCameraView();
            }
        }
    }

    private void setCameraView() {

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(mUserLocation.getGeoPoint().getLatitude(), mUserLocation.getGeoPoint().getLongitude() ), 19);
        mGoogleMap.animateCamera(cameraUpdate);
    }

    private String velocityInKMh(String velocity) {
        DecimalFormat decimalFormat2decimals = new DecimalFormat("#.##");
        velocity = velocity.replaceAll(",", ".");
        return String.valueOf(decimalFormat2decimals.format(Float.parseFloat(velocity) * 3.6));
    }
    private Query getUserPathInfo() {
        DocumentReference eventCollectionRef = FirebaseFirestore.getInstance().collection(EVENTSCOLLECTION).document(session.getEvent().getEventID());
       return eventCollectionRef.collection(USERSCOLLECTION).whereEqualTo("user_id", session.getUser().getUser_id());
    }

    private String  getDuration(String durationInMinutes) {
       int auxminutes = Integer.parseInt(durationInMinutes) ;
        int hours = auxminutes / 60;
        int minutes = auxminutes % 60;
      return "Duration: " + hours + "h:" + minutes + "m";
    }

    private void updateDistanceTraveled() {
        getUserPathInfo().get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.getResult().getDocuments().size() != 0 ) {
                     task.getResult().getDocuments().get(0).getReference().collection(USERPOSITIONS).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.getResult().getDocuments().size() != 0 ) {
                                DocumentReference documentReference = task.getResult().getDocuments().get(0).getReference();
                                listenUserPositions =  documentReference.addSnapshotListener(new EventListener<DocumentSnapshot>() {
                                    @Override
                                    public void onEvent(@javax.annotation.Nullable DocumentSnapshot documentSnapshot, @javax.annotation.Nullable FirebaseFirestoreException e) {
                                        if (e != null) {
                                            Log.w(TAG, "Listen failed.", e);
                                            return;
                                        }
                                        if(documentSnapshot.get("distanceTraveled") != null) {
                                            Log.d(TAG, documentSnapshot.get("distanceTraveled").toString());
                                            if(currentClusterItem != null) {
                                                float trimDistance = Float.parseFloat(documentSnapshot.get("distanceTraveled").toString());
                                                String velocityKM = velocityInKMh(documentSnapshot.get("velocity").toString());
                                                String newSnippet = "Distance traveled: " + decimalFormat.format(trimDistance) + "m"+ "\n"+ "Velocity: " + velocityKM +"km/h" + "\n"
                                                        + getDuration(documentSnapshot.get("duration").toString());
                                                clusterManagerRenderer.setUpdateMarkerSnippet(currentClusterItem, newSnippet);
                                                session.setCurrentDistanceTraveled(documentSnapshot.get("distanceTraveled").toString());
                                            }
                                        }
                                    }
                                });

                            }
                        }
                    });

                }else {
                    Log.d(TAG, "Error!!!!!" );
                }
            }
        });
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        MapFooterFragment footer = (MapFooterFragment) getSupportFragmentManager().findFragmentByTag("fragmentFooter");
        if (footer != null && footer.isVisible()) {
            onBackPressed();
        }
        GeoPoint geoPoint = new GeoPoint(marker.getPosition().latitude,marker.getPosition().longitude);
        if(marker.getTag() != null  && marker.getTag().hashCode()== MyClusterItem.class.hashCode()){
            Log.d(TAG, "This is an User!");
            MyClusterItem userMarker = clusterManagerRenderer.getExtraMarkerInfo().get(marker.getId());
            filterImagesByGeoPoint(geoPoint, marker, null, userMarker);
            return false;
        }

        ImageMarkerClusterItem imageMarker = imageClusterManagerRenderer.getExtraMarkerInfo().get(marker.getId());

        filterImagesByGeoPoint(geoPoint, marker, imageMarker, null);

        return false;
    }

    public void callMarkerFragment(CustomGeoPoint point, boolean newImage) {
        onBackPressed();
        isOnMarkerFragment = true;
        CustomGeoPoint geoPoint = new CustomGeoPoint(point.getLatitude(),point.getLongitude());
        Bundle args = new Bundle();
        args.putParcelable("geoPoint", geoPoint);
        args.putParcelableArrayList("filteredImages", filteredImages);
        args.putBoolean("isNewImage", newImage);
        FragmentManager fragmentManager = getSupportFragmentManager();


        if(!newImage) {
            ImageTabsFragment fragment = new ImageTabsFragment();
            fragment.setArguments(args);
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction().addToBackStack("mapFragment");
            fragmentTransaction.add(R.id.general_container, fragment,"imageTabsFragment");
            fragmentTransaction.commit();

        } else {
            MarkerFragment fragment = new MarkerFragment();
            fragment.setArguments(args);
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction().addToBackStack("mapFragment");
            fragmentTransaction.add(R.id.general_container, fragment,"imageFragment");
            fragmentTransaction.commit();
        }


    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void addMapFooter(Marker marker, ImageMarkerClusterItem imageMarker, MyClusterItem userMarker) {

        setLayoutWeight(child1_Linear_layout,80);
        CustomGeoPoint geoPoint = new CustomGeoPoint(marker.getPosition().latitude,marker.getPosition().longitude);
        boolean isAnUserClick = userMarker != null;
        Bundle args = new Bundle();
        args.putParcelable("geoPoint", geoPoint);
        args.putBoolean("isAnUserClick", isAnUserClick);
        args.putBoolean("hasImages", filteredImages.size() > 0);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction().addToBackStack("footerFrag");
        MapFooterFragment fragment = new MapFooterFragment();
        fragment.setArguments(args);
        fragmentTransaction.add(R.id.fragmentFooter, fragment,"fragmentFooter");
        fragmentTransaction.commit();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(mapFragment!= null &&  mapFragment.getView() != null &&  mapFragment.getView().getVisibility() == View.INVISIBLE)
           mapFragment.getView().setVisibility(View.VISIBLE);


        if(isOnMarkerFragment) {
           cleanTemporaryMarkers();
           isOnMarkerFragment = false;
        }

        if(mapFragment!= null &&  mapFragment.getView() != null)
           setLayoutWeight(child1_Linear_layout, 100);
    }

    public void setLayoutWeight( LinearLayout linearLayout, int weight) {
        LinearLayout.LayoutParams childParam = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0);
        childParam.weight = weight;
        linearLayout.setLayoutParams(childParam);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Marker marker =  mGoogleMap.addMarker(new MarkerOptions().position(latLng));


        temporaryMarkers.add(marker);

    }

    public String valueIntoString(Object value) {
        if(value != null)
            return value.toString();
        return "";
    }

    public void cleanTemporaryMarkers() {
        for (Marker marker: temporaryMarkers ){
            marker.remove();
        }
    }

    public void getImageMarkers() {

        listenerImages = mDb.collection(EVENTSCOLLECTION).document(eventID).collection(IMAGEMARKERSCOLLECTION)
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                Log.w(TAG, "Listen failed.", e);
                                return;
                            }
                            int auxNumberOfImages = mImageMarkersList.size();
                            mImageMarkersList.clear();
                            if(!newImageAdded)
                            newImageAdded = (value != null ? value.size() : 0) > auxNumberOfImages ;
                            for (QueryDocumentSnapshot doc : value) {

                                if (doc.get("imageUrl") != null && doc.get("user_id") != null && doc.get("geoPoint") != null) {
                                    CustomGeoPoint geoPoint = new CustomGeoPoint(doc.getGeoPoint("geoPoint").getLatitude(),doc.getGeoPoint("geoPoint").getLongitude());
                                    MarkerObject marker = new MarkerObject(geoPoint, valueIntoString(doc.get("user_id")), valueIntoString(doc.get("imageUrl")),
                                            valueIntoString(doc.get("eventId")), valueIntoString(doc.get("description")),valueIntoString(doc.get("imageName")),valueIntoString(doc.getId()), doc.getId());

                                    mImageMarkersList.add(marker);
                                    Log.d(TAG, marker.toString());
                                }
                            }
                            addImageMarkers();
                        }
                    });
        }

    private void calculatePathToUser(CustomGeoPoint dist) {
        CustomGeoPoint origin = session.getCurrentLocation();
        LatLng originLatLng = new LatLng(origin.getLatitude(), origin.getLongitude());
        LatLng distLatLng = new LatLng(dist.getLatitude(), dist.getLongitude());

        String url = getDirectionsUrl( originLatLng,  distLatLng);

        DownloadTask calculatePathTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        calculatePathTask.execute(url);
    }

    public void deleteOwnTrack() {

        getUserPathInfo().get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.getResult().getDocuments().size() != 0 ) {
                    task.getResult().getDocuments().get(0).getReference().collection(USERPOSITIONS).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                          if(task.getResult().getDocuments().size() != 0) {
                                task.getResult().getDocuments().get(0).getReference().update("distanceTraveled", 0);
                                task.getResult().getDocuments().get(0).getReference().update("velocity", 0);
                               task.getResult().getDocuments().get(0).getReference().update("duration", 0);
                                task.getResult().getDocuments().get(0).getReference().collection(POSITIONSCOLLECTION).orderBy("time" , Query.Direction.valueOf("ASCENDING")).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                        int eliminateAllExceptCurrentPosition = task.getResult().getDocuments().size() -1;
                                        for(int i = 0; eliminateAllExceptCurrentPosition > i ; i++){
                                            task.getResult().getDocuments().get(i).getReference().delete();
                                        }
                                    }
                                });
                            }
                            if(polyLines != null && polyLines.size() > 0){
                                for(int k=0; k< polyLines.size(); k++)
                                    polyLines.get(k).remove();
                            }
                        }
                    });
                }
            }
        });
    }


    @Override
    public void launchAction(int action, CustomGeoPoint geoPoint) {

        switch(action) {
            case 1:
                callMarkerFragment(geoPoint, false);
                break;
            case 2:
                callMarkerFragment(geoPoint, true);
                break;
            case 3:
                deleteOwnTrack();
                break;
            default:
                return;
        }
    }

    private void startCheckingUserPositions() {
        mhandlePositionsListener.postDelayed(mRunPositionsListener = new Runnable() {
            @Override
            public void run() {
                updateDistanceTraveled();
                if(listenUserPositions == null) mhandlePositionsListener.postDelayed(mRunPositionsListener, CHECK_USER_POSITIONS_INTERVAL);
            }
        }, CHECK_USER_POSITIONS_INTERVAL);
    }


    private void startUserLocationsRunnable(){
        isUserLocationRunning = true;
        Log.d(TAG, "startUserLocationsRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                retrieveUserLocations();
                mHandler.postDelayed(mRunnable, LOCATION_UPDATE_INTERVAL);
            }
        }, LOCATION_UPDATE_INTERVAL);
    }


    private void stopLocationUpdates(){
        mHandler.removeCallbacks(mRunnable);
        isUserLocationRunning = false;
    }

    private int calculateMinutesDiffBetweenPoints(String time1, String time2) {

        long secondsOfFirstPoint = 0;

        if(time2 != null) {
            secondsOfFirstPoint =  Long.parseLong(time2);
        }
        return time2 != null ? (int)(((  Long.parseLong(time1) - secondsOfFirstPoint)/ 1000)/60) : 0;

    }

    protected void addMyPositionsToMap() {
        boolean changingState = true;
        boolean isSlow = false;
        int color = Color.GREEN;
        Log.d(TAG, "Adding my positions to Map");
        String oldTime = null;
        if(polyLines != null && polyLines.size() > 0){
            for(int k=0; k< polyLines.size(); k++)
                polyLines.get(k).remove();
        polyLines.clear();
        }

        ArrayList points = null;
        ArrayList<PolylineOptions> polyLineArray = new ArrayList<PolylineOptions>();
        points = new ArrayList();
        LatLng oldPoint = null;
        PolylineOptions lineOptions = null;
        for (int i = 0; i < myPositions.size(); i++) {
            if(changingState) {
                lineOptions = new PolylineOptions();
                changingState = false;
                if(oldPoint != null)
                    points.add(oldPoint);
            }
            LatLng position = new LatLng(myPositions.get(i).getGeoPoint().getLatitude(), myPositions.get(i).getGeoPoint().getLongitude());
            points.add(position);
           int timeMax = calculateMinutesDiffBetweenPoints(myPositions.get(i).getTime(), oldTime);

            if(timeMax >= slowPathMaxTimeInMin && !isSlow) {
                points.remove(points.size()-1);
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(color);
                color = Color.RED;
                lineOptions.geodesic(true);
                polyLineArray.add(lineOptions);
                changingState = true;
                isSlow = true;
                points.clear();
                if(oldPoint != null)
                    points.add( oldPoint);
            }
            if(timeMax < slowPathMaxTimeInMin && isSlow) {
                points.remove(points.size()-1);
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(color);
                color = Color.GREEN;
                lineOptions.geodesic(true);
                polyLineArray.add(lineOptions);
                changingState = true;
                isSlow = false;
                points.clear();
                if(oldPoint != null)
                    points.add( oldPoint);
            }
            oldPoint = position;
            oldTime = myPositions.get(i).getTime();

        }
        if(points.size()> 0) {
            if(changingState) {
                lineOptions = new PolylineOptions();
                if (oldPoint != null)
                    points.add(oldPoint);
            }
            lineOptions.addAll(points);
            lineOptions.width(10);
            lineOptions.color(color);
            lineOptions.geodesic(true);
            polyLineArray.add(lineOptions);
        }

        for(int j= 0; j < polyLineArray.size(); j++) {
            polyLines.add(mGoogleMap.addPolyline(polyLineArray.get(j)));
            polyLines.get(j).setPattern(PATTERN_POLYLINE_DOTTED);
        }
    }

    private void updateMyCurrentPosition() {
        final String key =  session.getUser().getUser_id() + '_' + eventID;
        getUserPathInfo().get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.getResult().getDocuments().size() != 0 ) {
                    task.getResult().getDocuments().get(0).getReference().collection(USERPOSITIONS).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if(task.getResult().getDocuments().size() != 0 ) {
                                retrieveCurrentUserPositions(task.getResult().getDocuments().get(0));
                            }
                        }
                    });


                }else {
                    Log.d(TAG, "Can't get any position");
                }
            }
        });

    }

    private void retrieveCurrentUserPositions(DocumentSnapshot documentSnapshot) {

        documentSnapshot.getReference().collection(POSITIONSCOLLECTION).orderBy("time").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                List<DocumentSnapshot>  documents = task.getResult().getDocuments();
                myPositions.clear();
                for (int i = 0; i <   documents.size(); i++) {
                    myPositions.add(documents.get(i).toObject(UserPosition.class));
                }
                addMyPositionsToMap();
        }});
    }

    private void retrieveUserLocations(){
        Log.d(TAG, "retrieveUserLocations: retrieving location of all users in the Event.");
        try{
            for(final MyClusterItem clusterItem: mClusterItems){
                DocumentReference userLocationRef = FirebaseFirestore.getInstance()
                        .collection("User Locations")
                        .document(clusterItem.getUser().getUser_id());

                userLocationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){
                            final UserLocation updatedUserLocation =
                                    task.getResult().toObject(UserLocation.class);
                            // update the location
                            for (int i = 0; i < mClusterItems.size(); i++) {
                                try {
                                    if (mClusterItems.get(i).getUser().getUser_id().
                                            equals(updatedUserLocation.getUser().getUser_id())) {

                                        LatLng updatedLatLng = new LatLng(
                                                updatedUserLocation.getGeoPoint().getLatitude(),
                                                updatedUserLocation.getGeoPoint().getLongitude()
                                        );
                                        boolean hasChanges = !mClusterItems.get(i).getPosition()
                                                .equals(updatedLatLng);
                                        if((drawFirstTime || hasChanges) && mClusterItems.get(i).getUser().
                                                getUser_id().equals(session.getUser().getUser_id())) {
                                            updateMyCurrentPosition();
                                            drawFirstTime = false;
                                        }
                                        if(!hasChanges) continue;
                                        mClusterItems.get(i).setPosition(updatedLatLng);
                                        if(mClusterItems.get(i).getUser().
                                                getUser_id().equals(session.getUser().getUser_id()) && newImageAdded){
                                            addImageMarkers();
                                            newImageAdded = false;
                                        }
                                        clusterManagerRenderer.setUpdateMarker(mClusterItems.get(i));

                                    }
                                } catch (NullPointerException e) {
                                    Log.e(TAG, "retrieveUserLocations: NullPointerException: " + e.getMessage());
                                }
                            }
                        }
                    }
                });
            }
        }catch (IllegalStateException e){
            Log.e(TAG, "retrieveUserLocations: Fragment was destroyed during Firestore query. Ending query." + e.getMessage() );
        }
    }

    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent serviceIntent = new Intent(this, LocationTrackingService.class);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){

                MapActivity.this.startForegroundService(serviceIntent);
            }else{
                startService(serviceIntent);
            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            Log.d(TAG, service.service.getClassName());
            if("pt.ubi.eventtrackingapp.LocationTrackingService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.");
        return false;
    }

    private class DownloadTask extends AsyncTask {
        @Override
        protected void onPostExecute(Object object) {
            super.onPostExecute(object.toString());

            ParserTask parserTask = new ParserTask();


            parserTask.execute(object.toString());
        }

        @Override
        protected Object doInBackground(Object[] objects) {
            String data = "";

            try {
                data = downloadUrl(objects[0].toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }
    }

    public class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>>> {
        @Override
        protected List<List<HashMap<String,String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String,String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJsonParser parser = new DirectionsJsonParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String,String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String,String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat").toString());
                    double lng = Double.parseDouble(point.get("lng").toString());
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);

            }

            mGoogleMap.addPolyline(lineOptions);
        }

    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String mode = "mode=walking";
        String key = "key=" + GOOGLE_MAP_API_KEY;

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }







}

