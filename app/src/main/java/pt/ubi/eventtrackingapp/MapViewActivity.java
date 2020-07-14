package pt.ubi.eventtrackingapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.android.clustering.ClusterManager;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;


import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import static pt.ubi.eventtrackingapp.Constants.MAPVIEW_BUNDLE_KEY;

public class MapViewActivity extends Fragment implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener,
        MarkerFragment.OnFragmentInteractionListener {

    private static final String TAG = "MapViewFragment";
    private MapView mMapView;
    private FirebaseFirestore mDb;
    private ArrayList<UserLocation> mUserLocations = new ArrayList<>();
    private ArrayList<User> mUsersList = new ArrayList<>();
    private GoogleMap mGoogleMap;
    private LatLngBounds mMapBoundary;
    private UserLocation mUserLocation;
    private myClusterManagerRenderer clusterManagerRenderer;
    private ClusterManager<MyClusterItem> mClusterManager;
    private ArrayList<MyClusterItem> mClusterItems= new ArrayList<>();


    public static MapViewActivity newInstance(){
        return new MapViewActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mDb = FirebaseFirestore.getInstance();
        super.onCreate(savedInstanceState);
        // still need to make sure the mUserLocations doesn't duplicate when going back
        if(getArguments() != null) {
            mUsersList = getArguments().getParcelableArrayList("UsersList");
            mUserLocations = getArguments().getParcelableArrayList("UserLocations");

        }


    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_map_view, container, false);
        mMapView = view.findViewById(R.id.eventMap);

        initGoogleMap(savedInstanceState);

        for (UserLocation userLocation: mUserLocations) {
            Log.d(TAG, "onCreateView: User Location Longitude: " + userLocation.getGeoPoint().getLongitude()
            + " User Location Latitude: "+ userLocation.getGeoPoint().getLongitude());
        }

        return view;
    }

    private void addMapMarkers(){

        if(mGoogleMap != null){

            if(mClusterManager == null){
                mClusterManager = new ClusterManager<MyClusterItem>(getActivity().getApplicationContext(), mGoogleMap);
            }
            if(clusterManagerRenderer == null){
                clusterManagerRenderer = new myClusterManagerRenderer(
                        getActivity(),
                        mGoogleMap,
                        mClusterManager
                );
                mClusterManager.setRenderer(clusterManagerRenderer);
            }

            for(UserLocation userLocation: mUserLocations){

                Log.d(TAG, "addMapMarkers: location: " + userLocation.getGeoPoint().toString());
                try{
                    String snippet = "";
                    if(userLocation.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())){
                        snippet = "This is you";
                    }
                    else{
                        snippet = "Determine route to " + userLocation.getUser().getUsername() + "?";
                    }
                    String avatar = null;


                    avatar = userLocation.getUser().getmImageUrl();
                    MyClusterItem newClusterMarker = new MyClusterItem(
                            new LatLng(userLocation.getGeoPoint().getLatitude(), userLocation.getGeoPoint().getLongitude()),
                            userLocation.getUser().getUsername(),
                            snippet,
                            avatar,
                            userLocation.getUser()
                    );
                    mClusterManager.addItem(newClusterMarker);
                    mClusterItems.add(newClusterMarker);

                }catch (NullPointerException e){
                    Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage() );
                }

            }
            mClusterManager.cluster();
            setUserPosition();
            if(mUserLocation!= null) {
                setCameraView();
            }

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


    private void initGoogleMap(Bundle savedInstanceState){
        // *** IMPORTANT ***
        // MapView requires that the Bundle you pass contain _ONLY_ MapView SDK
        // objects or sub-Bundles.
        Bundle mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY);
        }

        mMapView.onCreate(mapViewBundle);

        mMapView.getMapAsync(this);
    }

    private void setUserPosition() {
        for(UserLocation userL: mUserLocations){
            if(userL.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid()));
                mUserLocation = userL;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY);
        if (mapViewBundle == null) {
            mapViewBundle = new Bundle();
            outState.putBundle(MAPVIEW_BUNDLE_KEY, mapViewBundle);
        }

        mMapView.onSaveInstanceState(mapViewBundle);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapView.onStop();
    }

    @Override
    public void onMapReady(GoogleMap map) {
        map.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));

        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mGoogleMap = map;
        map.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
               // for( User user: mUsersList)
                  //  getUserLocation(user);
                 addMapMarkers();
            }
        });

        // now just use this to create the menu with options 1- choose image 2 - delete
        map.setOnInfoWindowClickListener(this);

        map.setOnMarkerClickListener(this);

    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private static final int LOCATION_UPDATE_INTERVAL = 3000;

    private void startUserLocationsRunnable(){
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

    @Override
    public void onInfoWindowClick(Marker marker) {

    }

    @Override
    public boolean onMarkerClick(Marker marker)

    {

        // mMapView.setVisibility(View.VISIBLE);  // show again
        mMapView.setVisibility(View.GONE); // hide map

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        MarkerFragment fragment = new MarkerFragment();
        fragmentTransaction.replace(R.id.map_container, fragment,"markerFragment").addToBackStack("2");
        fragmentTransaction.commit();
        return false;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    /*
    Isto fica comentado por agora porque ainda não sei se uso esta função em vez de enviar logo as localizações do EventMainActivity ou se as vou buscar aqui
    e vou adicionando ao cluster

    public void getUserLocation(User user) {
        DocumentReference locationDocumentRef = mDb.collection("User Locations").document(user.getUser_id());

        locationDocumentRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    if(task.getResult().toObject(UserLocation.class) != null) {
                        mUserLocations.add(task.getResult().toObject(UserLocation.class));
                        // addMapMarker(task.getResult().toObject(UserLocation.class));

                    }
                }
            }
        });


    }

    private void addMapMarker(UserLocation userLocation){

        if(mGoogleMap != null){

            if(mClusterManager == null){
                mClusterManager = new ClusterManager<MyClusterItem>(getActivity().getApplicationContext(), mGoogleMap);
            }
            if(clusterManagerRenderer == null){
                clusterManagerRenderer = new myClusterManagerRenderer(
                        getActivity(),
                        mGoogleMap,
                        mClusterManager
                );
                mClusterManager.setRenderer(clusterManagerRenderer);
            }

                Log.d(TAG, "addMapMarkers: location: " + userLocation.getGeoPoint().toString());
                try{
                    String snippet = "";
                    if(userLocation.getUser().getUser_id().equals(FirebaseAuth.getInstance().getUid())){
                        snippet = "This is you";
                    }
                    else{
                        snippet = "Determine route to " + userLocation.getUser().getUsername() + "?";
                    }

                    int avatar = R.drawable.donald;


                    Vou ter de criar avatars para cada  user mas por agora ainda não tenho

                    try{
                        avatar =  R.drawable.donald;
                    }catch (NumberFormatException e){
                        Log.d(TAG, "addMapMarkers: no avatar for " + userLocation.getUser().getUsername() + ", setting default.");
                    }

                    MyClusterItem newClusterMarker = new MyClusterItem(
                            new LatLng(userLocation.getGeoPoint().getLatitude(), userLocation.getGeoPoint().getLongitude()),
                            userLocation.getUser().getUsername(),
                            snippet,
                            avatar,
                            userLocation.getUser()
                    );
                    mClusterManager.addItem(newClusterMarker);
                    mClusterItems.add(newClusterMarker);

                }catch (NullPointerException e){
                    Log.e(TAG, "addMapMarkers: NullPointerException: " + e.getMessage() );
                }


            mClusterManager.cluster();
            if(mUserLocation!= null) {
                setCameraView();
            }

        }
    }
    */



}
