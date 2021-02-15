package pt.ubi.eventtrackingapp;

import android.content.Intent;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class EventsListActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "EventsListActivity";
    private FirebaseFirestore mDb;
    private Session session;
    private TextInputEditText text_edit_content;
    private Button btn_filter, btn_clear;

    private String filterType = "owner";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = new Session(this);
        setContentView(R.layout.activity_events_list);
        mDb = FirebaseFirestore.getInstance();
        //get the spinner from the xml.
        Spinner dropdown = findViewById(R.id.filterType);
        text_edit_content = findViewById(R.id.filterContent);
        btn_filter = findViewById(R.id.filter);
        btn_clear = findViewById(R.id.clear);

        String[] items = new String[]{"Owner", "Event Name"};

        btn_filter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getEventsWithFilter();
            }
        });

        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                text_edit_content.setText("");
                getEvents();
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, items);
        dropdown.setAdapter(adapter);
        dropdown.setOnItemSelectedListener(this);

    }

    protected void onResume() {
        super.onResume();
        getEvents();
    }
    private void populateListWithEvents(ArrayList<Event> eventsList) {
        ListView myListView = (ListView) findViewById(R.id.events_list_View);
        EventsListAdapter adapter = new EventsListAdapter(EventsListActivity.this, R.layout.adapter_view_layout, eventsList);
        myListView.setAdapter(adapter);

        myListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected item text from ListView
                Event selectedItem = (Event) parent.getItemAtPosition(position);
                if (selectedItem.getEventID() != null) {
                    Intent eventMain = new Intent(EventsListActivity.this, EventMainCopy.class);
                    boolean isOwnerOfEvent = selectedItem.getOwner().equals(session.getUser().getUsername());
                    eventMain.putExtra("isOwnerOfEvent", isOwnerOfEvent);
                    eventMain.putExtra("eventID", selectedItem.getEventID());
                    session.setEvent(selectedItem);
                    startActivity(eventMain);
                    Log.d(TAG, "EventID: " + selectedItem.getEventID());
                } else
                    Log.d(TAG, "Error getting the information of the Event!!");

            }
        });
    }


    private void getEvents() {
        mDb.collection("Events")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<Event> eventsList = new ArrayList<>();
                            for (int k=0; k < task.getResult().getDocuments().size(); k++) {
                              Event event  = task.getResult().getDocuments().get(k).toObject(Event.class);
                               boolean isClosed =  task.getResult().getDocuments().get(k).get("isClosed")!= null && (boolean) task.getResult().getDocuments().get(k).get("isClosed") ;
                               if(isClosed) event.setClosed(isClosed);
                              eventsList.add(event);
                            }
                            populateListWithEvents(eventsList);
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        switch (i) {
            case 0:
                filterType = "owner";
                break;
            case 1:
                filterType = "name";
                break;

        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    public void getEventsWithFilter() {
        String Content = text_edit_content.getText() != null ? text_edit_content.getText().toString() : "";
        mDb.collection("Events").whereEqualTo(filterType, Content)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<Event> eventsList = new ArrayList<>();
                            for (int k=0; k < task.getResult().getDocuments().size(); k++) {
                                Event event  = task.getResult().getDocuments().get(k).toObject(Event.class);
                                boolean isClosed =  task.getResult().getDocuments().get(k).get("isClosed")!= null && (boolean) task.getResult().getDocuments().get(k).get("isClosed") ;
                                if(isClosed) event.setClosed(isClosed);
                                eventsList.add(event);
                            }
                            populateListWithEvents(eventsList);
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

    }
}
