package pt.ubi.eventtrackingapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.MapView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
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
import java.util.HashMap;
import java.util.List;

public class EventMainCopy extends AppCompatActivity {

    private static final String TAG = "EventMainActivity";
    private MapView mMapView;
    private FirebaseFirestore mDb;
    private ArrayList<UserLocationParcelable> mUserLocations = new ArrayList<>();
    private ArrayList<User> mUsersList = new ArrayList<>();
    private String  eventID;
    private Session session;
    private Button btn_GoToChat, btn_GoToMap, btn_leaveEvent, btn_DeleteEvent, btn_closeEvent;
    private boolean isOwnerOfEvent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_main_copy);

        //setting up buttons

        btn_GoToChat = findViewById(R.id.btn_GoToChat);
        btn_GoToMap = findViewById(R.id.btn_GoToMap);
        btn_leaveEvent = findViewById(R.id.btn_leaveEvent);
        btn_DeleteEvent = findViewById(R.id.btn_DeleteEvent);
        btn_closeEvent = findViewById(R.id.btn_closeEvent);
        mDb = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        session = new Session(EventMainCopy.this);
        eventID = intent.getStringExtra("eventID");
        isOwnerOfEvent = intent.getBooleanExtra("isOwnerOfEvent", false);
        if(!isOwnerOfEvent) btn_DeleteEvent.setVisibility(View.INVISIBLE);
        if(!isOwnerOfEvent || session.getEvent().isClosed())   btn_closeEvent.setVisibility(View.INVISIBLE);

        if(eventID == null){
            Log.d(TAG, " Error getting Event");
            startActivity(new Intent(EventMainCopy.this, DashboardActivity.class));
        }


        // setting up button clicks
        btn_GoToChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chatIntent = new Intent(EventMainCopy.this, ChatActivity.class);
                chatIntent.putExtra("UsersList", mUsersList);
                chatIntent.putExtra("eventID", eventID);
                startActivity(chatIntent);
            }
        });

        btn_DeleteEvent.setOnClickListener(new View.OnClickListener() {
                                               @Override
                                               public void onClick(View v) {
                                                   deleteEvent();
                                               }
                                           });

        btn_closeEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeEvent();
            }
        });

        btn_GoToMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent mapIntent = new Intent(EventMainCopy.this, MapActivity.class);
                mapIntent.putExtra("UserLocations", mUserLocations);
                mapIntent.putExtra("UsersList", mUsersList);
                mapIntent.putExtra("eventID", eventID);
                startActivity(mapIntent);
            }
        });

        btn_leaveEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                leaveEvent();
            }
        });

        getUsersOfTheEvent();

    }

    private void closeEvent() {
     DocumentReference eventDocument =   mDb.collection("Events").document(eventID);
        HashMap<String,Object> closeEventHashMap = new HashMap();
        closeEventHashMap.put("isClosed", true);
        eventDocument.update(closeEventHashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Update Successful");
                    Toast.makeText(EventMainCopy.this, "Event Closed with success",
                            Toast.LENGTH_SHORT).show();
                    btn_closeEvent.setVisibility(View.INVISIBLE);
                   Event closedEvent = session.getEvent();
                    closedEvent.setClosed(true);
                    session.setEvent(closedEvent);
                } else {
                    Log.w(TAG, "Error updating document", task.getException());
                }
            }
        });

    }

    private void leaveEvent() {
        mDb.collection("Events").document(eventID).collection("Users").whereEqualTo("user_id",session.getUser().getUser_id()).get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for (DocumentSnapshot document : task.getResult()) {
                    mDb.collection("Events").document(eventID).collection("Users").document(document.getId()).delete();
                    Toast.makeText(EventMainCopy.this, "You are no longer in the Event!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(EventMainCopy.this, EventsListActivity.class));
                }
            }
        });
    }

    private void getUsersOfTheEvent() {
        mDb.collection("Events").document(eventID).collection("Users")
                .addSnapshotListener(EventMainCopy.this,new EventListener<QuerySnapshot>() {
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
                        addUserToEvent();
                    }
                });

    }

   public void deleteEventMainCollection() {
      DocumentReference eventDocument = mDb.collection("Events").document(eventID);
       eventDocument.collection("Users").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
           @Override
           public void onComplete(@NonNull Task<QuerySnapshot> task) {
               if(task.isSuccessful()) {
                   for(int i= 0; task.getResult().getDocuments().size() > i; i++)
                       task.getResult().getDocuments().get(i).getReference().delete();
               } else {
               Log.d(TAG, "Error deleting Users of event");
                }
           }
       });

       eventDocument.collection("ImageMarkers").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
           @Override
           public void onComplete(@NonNull Task<QuerySnapshot> task) {
               if(task.isSuccessful()) {
                   for(int i= 0; task.getResult().getDocuments().size() > i; i++)
                       task.getResult().getDocuments().get(i).getReference().delete();
               } else {
                   Log.d(TAG, "Error deleting Images of event");
               }

           }
       });
       eventDocument.delete();

    }

    public void deleteAllUsersPositionOfEvent() {
        mDb.collection("UserPositionsInEvent").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    for(int i= 0; task.getResult().getDocuments().size() >i ;i++) {
                        DocumentSnapshot document =  task.getResult().getDocuments().get(i);
                        Object userPositionKey = document.get("userPositionKey");
                        if(userPositionKey!= null && userPositionKey.toString().contains(eventID)) {
                            document.getReference().delete();
                            document.getReference().collection("UserPosition").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                    if(task.isSuccessful()) {
                                        for(int j= 0; task.getResult().getDocuments().size() >j ;j++) {
                                            task.getResult().getDocuments().get(j).getReference().delete();
                                        }
                                    } else {
                                        Log.d(TAG, "Error deleting document!");
                                }
                                }
                            });
                        }
                    }
                }else {
                    Log.d(TAG, "Error getting UserPositions!");
                }
            }
        });
    }

   public void deleteEvent() {
       deleteAllUsersPositionOfEvent();
       deleteEventMainCollection();
       Intent eventList = new Intent(EventMainCopy.this, EventsListActivity.class);
       startActivity(eventList);
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
