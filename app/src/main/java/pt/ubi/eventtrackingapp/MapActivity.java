package pt.ubi.eventtrackingapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.os.Message;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;

import static pt.ubi.eventtrackingapp.Constants.MAPVIEW_BUNDLE_KEY;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerClickListener,
        MarkerFragment.OnFragmentInteractionListener, GoogleMap.OnMapLongClickListener {

    private static final String TAG = "MapViewFragment";
    private MapView mMapView;
    private FirebaseFirestore mDb;
    private ArrayList<UserLocationParcelable> mUserLocations = new ArrayList<>();
    private ArrayList<MarkerObject> mImageMarkers = new ArrayList<>();
    private ArrayList<User> mUsersList = new ArrayList<>();
    private GoogleMap mGoogleMap;
    private LatLngBounds mMapBoundary;
    private UserLocationParcelable mUserLocation;
    private myClusterManagerRenderer clusterManagerRenderer;
    private ClusterManager<MyClusterItem> mClusterManager;
    private ArrayList<MyClusterItem> mClusterItems= new ArrayList<>();
    private SupportMapFragment mapFragment;
    Handler locationHandler;
    final static long REFRESH = 10 * 1000;
    final static int SUBJECT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        Intent intent = getIntent();
        mUsersList = intent.getParcelableArrayListExtra("UsersList");
        mUserLocations = intent.getParcelableArrayListExtra("UserLocations");
         mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        locationHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == SUBJECT) {
                    addMapMarkers();
                    this.sendEmptyMessageDelayed(SUBJECT, REFRESH);
                }
            }
        };


        /*

        for (UserLocation userLocation: mUserLocations) {
            Log.d(TAG, "onCreateView: User Location Longitude: " + userLocation.getGeoPoint().getLongitude()
                    + " User Location Latitude: "+ userLocation.getGeoPoint().getLongitude());
        }
        */

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
                 addMapMarkers();
            }
        });

        // now just use this to create the menu with options 1- choose image 2 - delete
        mGoogleMap.setOnInfoWindowClickListener(this);

        mGoogleMap.setOnMarkerClickListener(this);
        mGoogleMap.setOnMapLongClickListener(this);


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
            mClusterManager.cluster();
            setUserPosition();
            if(mUserLocation!= null) {
                setCameraView();
            }

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

    @Override
    public boolean onMarkerClick(Marker marker) {
        // mMapView.setVisibility(View.VISIBLE);  // show again
      //  mMapView.setVisibility(View.GONE); // hide map
        mapFragment.getView().setVisibility(View.INVISIBLE);
        CustomGeoPoint geoPoint = new CustomGeoPoint(marker.getPosition().latitude,marker.getPosition().longitude);
        Bundle args = new Bundle();
        args.putParcelable("geoPoint", geoPoint);

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction().addToBackStack("2");
        MarkerFragment fragment = new MarkerFragment();
        fragment.setArguments(args);

        fragmentTransaction.add(R.id.map_container, fragment,"markerFragment");
        fragmentTransaction.commit();
        return false;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
       if(mapFragment!= null &&  mapFragment.getView() != null &&  mapFragment.getView().getVisibility() == View.INVISIBLE)
            mapFragment.getView().setVisibility(View.VISIBLE);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mGoogleMap.addMarker(new MarkerOptions().position(latLng).title("Marker"));
    }
}
