package pt.ubi.eventtrackingapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventMainCopy extends AppCompatActivity {

    private static final String TAG = "EventMainActivity";
    private MapView mMapView;
    private FirebaseFirestore mDb;
    private ArrayList<UserLocationParcelable> mUserLocations = new ArrayList<>();
    private ArrayList<User> mUsersList = new ArrayList<>();
    private String  eventID;
    private Session session;
    private Button btn_GoToChat, btn_GoToMap, btn_leaveEvent,btn_GoBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_main_copy);

        //setting up buttons

        btn_GoToChat = findViewById(R.id.btn_GoToChat);
        btn_GoToMap = findViewById(R.id.btn_GoToMap);
        btn_leaveEvent = findViewById(R.id.btn_leaveEvent);
        btn_GoBack = findViewById(R.id.btn_GoBack);

        mDb = FirebaseFirestore.getInstance();
        Intent intent = getIntent();
        session = new Session(EventMainCopy.this);
        eventID = intent.getStringExtra("eventID");
        if(eventID == null){
            Log.d(TAG, " Error getting Event");
            startActivity(new Intent(EventMainCopy.this, DashboardActivity.class));
        }

        btn_GoToChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EventMainCopy.this, ChatActivity.class));
            }
        });

        btn_GoToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(EventMainCopy.this, MapActivity.class);
                mapIntent.putExtra("UserLocations", mUserLocations);
                mapIntent.putExtra("UsersList", mUsersList);
                startActivity(mapIntent);
            }
        });

        btn_leaveEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ainda não sei se vou fazer alguma coisa com isto
            }
        });

        btn_GoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // ainda não sei se vou fazer alguma coisa com isto
            }
        });
        getUsersOfTheEvent();

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
                                getUserLocation(user);

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

                    }
                }
            }
        });
    }
}
