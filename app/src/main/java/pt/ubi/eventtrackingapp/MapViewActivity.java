package pt.ubi.eventtrackingapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.os.Bundle;
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
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.android.clustering.ClusterManager;


import java.util.ArrayList;

import static pt.ubi.eventtrackingapp.Constants.MAPVIEW_BUNDLE_KEY;

public class MapViewActivity extends Fragment implements OnMapReadyCallback {

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // still need to make sure the mUserLocations doesn't duplicate when going back
        if(getArguments() != null) {
            mUserLocations = getArguments().getParcelableArrayList("UserLocations");
            mUsersList = getArguments().getParcelableArrayList("UsersList");
        }
        setUserPosition();

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

                    int avatar = R.drawable.donald;

                    /*
                    Vou ter de criar avatars para cada  user mas por agora ainda não tneho

                    try{
                        avatar =  R.drawable.donald;
                    }catch (NumberFormatException e){
                        Log.d(TAG, "addMapMarkers: no avatar for " + userLocation.getUser().getUsername() + ", setting default.");
                    }
                    */
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

            setCameraView();
        }
    }

    private void setCameraView() {

        // Overall map view Window
        double bottomBoundary = mUserLocation.getGeoPoint().getLatitude() - .1;
        double leftBoundary = mUserLocation.getGeoPoint().getLongitude() - .1;
        double rightBoundary = mUserLocation.getGeoPoint().getLongitude() + .1;
        double topBoundary = mUserLocation.getGeoPoint().getLatitude() + .1;

        mMapBoundary = new LatLngBounds(new LatLng(bottomBoundary, leftBoundary), new LatLng(topBoundary,rightBoundary));
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(mMapBoundary,0));
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
                addMapMarkers();
            }
        });

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


}
