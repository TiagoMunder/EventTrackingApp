package pt.ubi.eventtrackingapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class EventInfoActivity extends AppCompatActivity {

    private TextView TV_Street,TV_City,TV_Country,TV_description,TV_owner,TV_date;
    private String eventID;
    private static final String TAG = "EventInfoActivity";
    private FirebaseFirestore mDb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_info);

        TV_Street = findViewById(R.id.street);
        TV_Country = findViewById(R.id.country);
        TV_City = findViewById(R.id.city);
        TV_description = findViewById(R.id.description);
        TV_date = findViewById(R.id.date);
        TV_owner = findViewById(R.id.owner);
        eventID = getIntent().getStringExtra("eventID");
        if (eventID.equals(null)) {
            Toast.makeText(EventInfoActivity.this, "Can't get Event Info!!! Redirecting to Dashboard!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(EventInfoActivity.this, DashboardActivity.class));
        }
        mDb = FirebaseFirestore.getInstance();
        getEventInfo();
    }

    private void getEventInfo() {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mDb.setFirestoreSettings(settings);
        DocumentReference docRef = mDb.collection("Events").document(eventID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());

                        Event event = new Event(document.get("owner").toString().trim(), document.get("eventName").toString().trim(),document.get("description").toString().trim(),
                                document.get("street").toString().trim(), document.get("city").toString().trim(),
                                document.get("country").toString().trim(),document.get("eventChoosenDate").toString().trim());
                        setInfo(event);
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });


    }


    public void setInfo(Event event){
        TV_Street.setText(event.getStreet());
        TV_City.setText(event.getCity());
        TV_Country.setText(event.getCountry());
        TV_date.setText(event.getDate());
        TV_description.setText(event.getDescription());
        TV_owner.setText(event.getOwner());
        System.out.println(event);
        Toast.makeText(EventInfoActivity.this, "Success Fetching Event Info. \n Event Name : "+event.getName(), Toast.LENGTH_SHORT).show();
    }
}
