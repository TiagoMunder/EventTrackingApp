package pt.ubi.eventtrackingapp;
import android.app.ActivityManager;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

public class EventMainActivity extends AppCompatActivity {


    private SectionsPagerAdapter mSectionsPagerAdapter;

    private ViewPager mViewPager;
    private static final String TAG = "EventMainActivity";
    private MapView mMapView;
    private FirebaseFirestore mDb;
    private ArrayList<UserLocation> mUserLocations = new ArrayList<>();
    private ArrayList<User> mUsersList = new ArrayList<>();
    private String  eventID;
    private Session session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_main);

        mDb = FirebaseFirestore.getInstance();
        Intent intent = getIntent();
        session = new Session(EventMainActivity.this);
        eventID = intent.getStringExtra("eventID");
        if(eventID == null){
            Log.d(TAG, " Error getting Event");
            startActivity(new Intent(EventMainActivity.this, DashboardActivity.class));
        }
        getUsersOfTheEvent();

    }

    protected void onStart(){
        super.onStart();
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);

    }




    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = null;
            switch (position) {
                case 0:
                    fragment = getChatFragment();
                    break;
                case 1:
                    fragment = getMapFragment();
                    break;
            }
            return fragment;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Chat";
                case 1:
                    return "Maps";
            }
            return null;
        }
    }


    private Fragment getMapFragment() {
        /*
        for( User user: mUsersList)
        Fragment = getUserLocation(user);
        */
        MapViewActivity mapFragment = MapViewActivity.newInstance();
        Bundle mapBundle = new Bundle();
        mapBundle.putParcelableArrayList("UsersList", mUsersList);
        mapBundle.putParcelableArrayList("UserLocations", mUserLocations);
        mapFragment.setArguments(mapBundle);
        return mapFragment;
    }

    private Fragment getChatFragment() {

        Fragment  fragment = new ChatFragment();
        Bundle mapBundle = new Bundle();
        mapBundle.putParcelableArrayList("UsersList", mUsersList);
        mapBundle.putString("EventId", eventID);
        fragment.setArguments(mapBundle);
        return fragment;
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

                                User user = new User(doc.get("email").toString(), doc.get("username").toString(),doc.get("user_id").toString(),doc.get("mImageUrl")!=null ? doc.get("mImageUrl").toString() : null);
                                mUsersList.add(user);
                                addUserToEvent();
                                getUserLocation(user);

                            }
                        }

                    }
                });

    }
    /*
    private void getUserLocation(User user) {
        mDb.collection("User Locations")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }

                        for (QueryDocumentSnapshot doc : value) {

                            if (doc != null ) {
                                mUserLocations.add(doc.toObject(UserLocation.class));
                            }
                        }

                    }
                });

    }

*/

    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent serviceIntent = new Intent(this, LocationTrackingService.class);
//        this.startService(serviceIntent);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){

                EventMainActivity.this.startForegroundService(serviceIntent);
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

    public void getUserLocation(User user) {
        DocumentReference locationDocumentRef = mDb.collection("User Locations").document(user.getUser_id());

        locationDocumentRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    if(task.getResult().toObject(UserLocation.class) != null) {
                        mUserLocations.add(task.getResult().toObject(UserLocation.class));

                    }
                }
            }
        });
    }

    private void addUserToEvent() {
        boolean userAlreadyIntheEvent =false;
        for (int i=0 ; i<mUsersList.size(); i++){
            if(mUsersList.get(i).getUsername().equals(session.getUsername()))
                userAlreadyIntheEvent =  true;

        }
        if(!userAlreadyIntheEvent) {
            mUsersList.add(session.getUser());
            mDb.collection("Events").document(eventID).collection("Users").add(session.getUser());
        }
    }
}
