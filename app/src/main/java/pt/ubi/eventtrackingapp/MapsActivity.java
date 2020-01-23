package pt.ubi.eventtrackingapp;

import android.app.ActivityManager;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;


public class MapsActivity extends AppCompatActivity {

    private static final String TAG = "MapViewActivity";
    private MapView mMapView;
    private FirebaseFirestore mDb;
    private ArrayList<UserLocation> mUserLocations = new ArrayList<>();
    private ArrayList<User> mUsersList = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mDb = FirebaseFirestore.getInstance();
        getUsersOfTheEvent();



    }

    private void addFragment(Fragment fragment, boolean addToBackStack, String tag) {

        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = manager.beginTransaction();
        Bundle mapBundle = new Bundle();
        mapBundle.putParcelableArrayList("UserLocations", mUserLocations);
        mapBundle.putParcelableArrayList("UsersList", mUsersList);
        fragment.setArguments(mapBundle);
        if (addToBackStack) {
            fragmentTransaction.addToBackStack(tag);
        }
        fragmentTransaction.replace(R.id.container_frame_back, fragment, tag);
        fragmentTransaction.commitAllowingStateLoss();

    }

    private void getUsersOfTheEvent() {
        mDb.collection("Events").document("JoluaQw7PB8usY4KR0A6").collection("Users")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        for (QueryDocumentSnapshot doc : value) {

                            if (doc.get("username") != null && doc.get("email") != null) {

                                User user = new User(doc.get("email").toString(), doc.get("username").toString(),doc.get("user_id").toString());
                                mUsersList.add(user);
                                getUserLocation(user);
                            }
                        }

                    }
                });

    }

    public void getUserLocation(User user) {
        DocumentReference locationDocumentRef = mDb.collection("User Locations").document(user.getUser_id());

        locationDocumentRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    if(task.getResult().toObject(UserLocation.class) != null) {
                        mUserLocations.add(task.getResult().toObject(UserLocation.class));

                    }
                    startLocationService();
                    addFragment(new MapViewActivity(),false, "Map");
                }
            }
        });


    }

    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent serviceIntent = new Intent(this, LocationTrackingService.class);
//        this.startService(serviceIntent);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){

                MapsActivity.this.startForegroundService(serviceIntent);
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
