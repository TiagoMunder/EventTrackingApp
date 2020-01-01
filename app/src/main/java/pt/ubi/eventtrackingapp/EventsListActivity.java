package pt.ubi.eventtrackingapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class EventsListActivity extends AppCompatActivity {

    private static final String TAG = "EventsListActivity";
    private FirebaseFirestore mDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events_list);
        mDb = FirebaseFirestore.getInstance();
        getEvents();
    }

    private void getEvents() {
        mDb.collection("Events")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ListView myListView = (ListView) findViewById(R.id.events_list_View);
                            ArrayList<Event> eventsList = new ArrayList<>();
                            List<Event> events = task.getResult().toObjects(Event.class);
                            eventsList.addAll(events);
                            EventsListAdapter adapter = new EventsListAdapter(EventsListActivity.this,R.layout.adapter_view_layout,eventsList);
                            myListView.setAdapter(adapter);

                            myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    // Get the selected item text from ListView
                                    Event selectedItem = (Event) parent.getItemAtPosition(position);
                                    if(selectedItem.getEventID() != null) {
                                        Intent eventMain = new Intent(EventsListActivity.this, ChatActivity.class);
                                        eventMain.putExtra("eventID", selectedItem.getEventID());
                                        startActivity(eventMain);
                                        Log.d(TAG,"EventID: " + selectedItem.getEventID());
                                    }
                                    else
                                        Log.d(TAG,"Error getting the information of the Event!!");

                                }
                            });
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }
}
