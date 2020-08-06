package pt.ubi.eventtrackingapp;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.os.Message;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.HashMap;


public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener,
        MarkerFragment.OnFragmentInteractionListener, MapFooterFragment.OnFragmentInteractionListener,GoogleMap.OnMapLongClickListener, MapFooterFragment.ButtonCallback {

    private static final String TAG = "MapFragmentActivity";
    private MapView mMapView;
    private FirebaseFirestore mDb;
    private ArrayList<UserLocationParcelable> mUserLocations = new ArrayList<>();
    private ArrayList<MarkerObject> mImageMarkersList = new ArrayList<>();
    private ArrayList<User> mUsersList = new ArrayList<>();
    private GoogleMap mGoogleMap;
    private LatLngBounds mMapBoundary;
    private UserLocationParcelable mUserLocation;
    private myClusterManagerRenderer clusterManagerRenderer;
    private ClusterManager<MyClusterItem> mClusterManager;
    private ArrayList<MyClusterItem> mClusterItems= new ArrayList<>();
    private ImageMarkerClusterManagerRenderer imageClusterManagerRenderer;
    private ClusterManager<ImageMarkerClusterItem> mImageMarkerClusterManager;
    private ArrayList<ImageMarkerClusterItem> mImageMarkersClusterItems= new ArrayList<>();
    private ArrayList<Marker> temporaryMarkers = new ArrayList<>();

    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;


    private boolean isOnMarkerFragment = false;
    private SupportMapFragment mapFragment;
    private LinearLayout child1_Linear_layout;

    private boolean isUserLocationRunning = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        child1_Linear_layout =(LinearLayout)findViewById(R.id.map_container);
        Intent intent = getIntent();
        mUsersList = intent.getParcelableArrayListExtra("UsersList");
        mUserLocations = intent.getParcelableArrayListExtra("UserLocations");
         mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mDb = FirebaseFirestore.getInstance();
        startLocationService();



        /*

        for (UserLocation userLocation: mUserLocations) {
            Log.d(TAG, "onCreateView: User Location Longitude: " + userLocation.getGeoPoint().getLongitude()
                    + " User Location Latitude: "+ userLocation.getGeoPoint().getLongitude());
        }
        */

    }




    protected void onStart() {
        super.onStart();
        if(!isUserLocationRunning)
            startUserLocationsRunnable();
    }

    protected void onResume() {
        super.onResume();
        if(!isUserLocationRunning)
            startUserLocationsRunnable();
    }

    protected void onPause() {
        super.onPause();
        stopLocationUpdates();

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
                // for( User user: mUsersList)
                //  getUserLocation(user);
                getImageMarkers();

                addMapMarkers();
            }
        });



        if(mImageMarkerClusterManager == null){
            mImageMarkerClusterManager = new ClusterManager<ImageMarkerClusterItem>(getApplicationContext(), mGoogleMap);
        }

        // now just use this to create the menu with options 1- choose image 2 - delete
        mGoogleMap.setOnInfoWindowClickListener(this);

        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnMapLongClickListener(this);


    }

    private void updateSelfUserLocation(Location location) {

    }

    private void addMapMarkers(){

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

            for(UserLocationParcelable userLocationParcelable: mUserLocations){

                Log.d(TAG, "addMapMarkers: location: " + userLocationParcelable.getGeoPoint().toString());
                try{
                    String snippet = "";
                    if(userLocationParcelable.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())){
                        snippet = "This is you";
                    }
                    else{
                        snippet = "Determine route to " + userLocationParcelable.getUser().getUsername() + "?";
                    }
                    String avatar = null;


                    avatar = userLocationParcelable.getUser().getmImageUrl();
                    MyClusterItem newClusterMarker = new MyClusterItem(
                            new LatLng(userLocationParcelable.getGeoPoint().getLatitude(), userLocationParcelable.getGeoPoint().getLongitude()),
                            userLocationParcelable.getUser().getUsername(),
                            snippet,
                            avatar,
                            userLocationParcelable.getUser()
                    );
                    mClusterManager.addItem(newClusterMarker);
                    mClusterItems.add(newClusterMarker);


                }catch (NullPointerException e){
                    Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage() );
                }

            }

            setUserPosition();
            mClusterManager.cluster();
            if(mUserLocation!= null) {
                setCameraView();
            }

        }
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
                mImageMarkersClusterItems.clear();

            }

            for(MarkerObject imageMarker: mImageMarkersList){

                Log.d(TAG, "addMapMarkers: location: " + imageMarker.getGeoPoint().toString());
                try{
                    String snippet = "Not sure what to place here"; // TODO

                    String avatar = null;

                    String title = imageMarker.getImageName() != null ? imageMarker.getImageName() : "TODO";


                    avatar = imageMarker.getImageUrl();
                    ImageMarkerClusterItem newClusterMarker = new ImageMarkerClusterItem(
                            new LatLng(imageMarker.getGeoPoint().getLatitude(), imageMarker.getGeoPoint().getLongitude()),
                            title,
                            snippet,
                            avatar,
                            imageMarker.getUser_id(),
                            imageMarker.getEventId(),
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
            if(userL.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid()));
            mUserLocation = userL;
        }
    }



    private void setCameraView() {

        // Overall map view Window

        double bottomBoundary = mUserLocation.getGeoPoint().getLatitude() - .1;
        double leftBoundary = mUserLocation.getGeoPoint().getLongitude() - .1;
        double rightBoundary = mUserLocation.getGeoPoint().getLongitude() + .1;
        double topBoundary = mUserLocation.getGeoPoint().getLatitude() + .1;

        mMapBoundary = new LatLngBounds(new LatLng(bottomBoundary, leftBoundary), new LatLng(topBoundary, rightBoundary));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mMapBoundary, 0));

    }


    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    private void addingMarkerDynamically(MarkerObject imageMarker) {
        Log.d(TAG, "addMapMarkers: location: " + imageMarker.getGeoPoint().toString());
        try{
            String snippet = "Not sure what to place here"; // TODO

            String avatar = null;

            String title = imageMarker.getImageName() != null ? imageMarker.getImageName() : "TODO";


            avatar = imageMarker.getImageUrl();
            LatLng lat =   new LatLng(imageMarker.getGeoPoint().getLatitude(), imageMarker.getGeoPoint().getLongitude());
            ImageMarkerClusterItem newClusterMarker = new ImageMarkerClusterItem(
                lat,
                    title,
                    snippet,
                    avatar,
                    imageMarker.getUser_id(),
                    imageMarker.getEventId(),
                    imageMarker.getDescription(),
                    imageMarker.getId()
            );
            mImageMarkerClusterManager.addItem(newClusterMarker);
            mImageMarkersClusterItems.add(newClusterMarker);
            mImageMarkerClusterManager.cluster();

        }catch (NullPointerException e){
            Log.e(TAG, "addImageMapMarkers: NullPointerException: " + e.getMessage() );
        }


    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        // mMapView.setVisibility(View.VISIBLE);  // show again
      //  mMapView.setVisibility(View.GONE); // hide map
        MapFooterFragment footer = (MapFooterFragment) getSupportFragmentManager().findFragmentByTag("fragmentFooter");
        if (footer != null && footer.isVisible()) {
            onBackPressed();
        }

        if(marker != null && marker.getTag() != null  && marker.getTag().hashCode()== MyClusterItem.class.hashCode()){
            Log.d(TAG, "This is an User!");
            return false;
        }

        ImageMarkerClusterItem imageMarker = imageClusterManagerRenderer.getExtraMarkerInfo().get(marker.getId());
        addMapFooter( marker, imageMarker);
        return false;
    }

    public void callMarkerFragment(CustomGeoPoint point, ImageMarkerClusterItem imageMarker) {
        onBackPressed();
        isOnMarkerFragment = true;
        CustomGeoPoint geoPoint = new CustomGeoPoint(point.getLatitude(),point.getLongitude());
        Bundle args = new Bundle();
        args.putParcelable("geoPoint", geoPoint);
        args.putParcelable("imageMarker", imageMarker);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction().addToBackStack("markerFrag");
        MarkerFragment fragment = new MarkerFragment();
        fragment.setArguments(args);
        fragmentTransaction.add(R.id.general_container, fragment,"markerFragment");
        fragmentTransaction.commit();
    }


    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    public void addMapFooter(Marker marker, ImageMarkerClusterItem imageMarker) {
        setLayoutWeight(child1_Linear_layout,80);
        CustomGeoPoint geoPoint = new CustomGeoPoint(marker.getPosition().latitude,marker.getPosition().longitude);
        Bundle args = new Bundle();
        args.putParcelable("geoPoint", geoPoint);
        args.putParcelable("imageMarker", imageMarker);
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment frag = fragmentManager.findFragmentByTag("footerFrag");
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

        // cleaning all the markers after coming from MarkerFragment
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

            mDb.collection("ImageMarkers").whereEqualTo("eventId","JoluaQw7PB8usY4KR0A6")
                    .addSnapshotListener(new EventListener<QuerySnapshot>() {
                        @Override
                        public void onEvent(@Nullable QuerySnapshot value,
                                            @Nullable FirebaseFirestoreException e) {
                            if (e != null) {
                                Log.w(TAG, "Listen failed.", e);
                                return;
                            }
                            mImageMarkersList.clear();

                            for (QueryDocumentSnapshot doc : value) {

                                if (doc.get("imageUrl") != null && doc.get("user_id") != null && doc.get("geoPoint") != null) {
                                    CustomGeoPoint geoPoint = new CustomGeoPoint(doc.getGeoPoint("geoPoint").getLatitude(),doc.getGeoPoint("geoPoint").getLongitude());
                                    MarkerObject marker = new MarkerObject(geoPoint, valueIntoString(doc.get("user_id")), valueIntoString(doc.get("imageUrl")),
                                            valueIntoString(doc.get("eventId")), valueIntoString(doc.get("description")),valueIntoString(doc.get("imageName")),valueIntoString(doc.getId()));

                                    mImageMarkersList.add(marker);
                                    // This is not making anything right now but i think i can use it to improve code
                                    // addingMarkerDynamically(marker);
                                    Log.d(TAG, marker.toString());
                                }
                            }
                            addImageMarkers();

                        }
                    });

        }

    @Override
    public void launchAction(int action, CustomGeoPoint geoPoint, ImageMarkerClusterItem imageMarker) {
     if(action == 1)
         callMarkerFragment(geoPoint,imageMarker);
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

    private void retrieveUserLocations(){
        Log.d(TAG, "retrieveUserLocations: retrieving location of all users in the chatroom.");

        try{
            for(final MyClusterItem clusterItem: mClusterItems){

                DocumentReference userLocationRef = FirebaseFirestore.getInstance()
                        .collection("User Locations")
                        .document(clusterItem.getUser().getUser_id());

                userLocationRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if(task.isSuccessful()){

                            final UserLocation updatedUserLocation = task.getResult().toObject(UserLocation.class);

                            // update the location
                            for (int i = 0; i < mClusterItems.size(); i++) {
                                try {
                                    if (mClusterItems.get(i).getUser().getUser_id().equals(updatedUserLocation.getUser().getUser_id())) {

                                        LatLng updatedLatLng = new LatLng(
                                                updatedUserLocation.getGeoPoint().getLatitude(),
                                                updatedUserLocation.getGeoPoint().getLongitude()
                                        );

                                        mClusterItems.get(i).setPosition(updatedLatLng);
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



}
