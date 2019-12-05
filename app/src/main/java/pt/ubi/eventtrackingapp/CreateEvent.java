package pt.ubi.eventtrackingapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CreateEvent extends AppCompatActivity {

    private EditText eventName, description, country, city, street;
    private Session session;
    private TextView mChooseDate;
    private DatePickerDialog.OnDateSetListener mDateSetListener;
    private String owner, eventChoosenDate;
    private Button btn_create, btn_cancel;
    private FirebaseFirestore mDb;
    private static final String TAG = "CreateEvent";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);
        eventName = findViewById(R.id.eventName);
        description = findViewById(R.id.description);
        country = findViewById(R.id.country);
        city = findViewById(R.id.city);
        street = findViewById(R.id.street);
        btn_create = findViewById(R.id.btn_create);
        btn_cancel = findViewById(R.id.btn_cancel);
        session = new Session(CreateEvent.this);
        mDb = FirebaseFirestore.getInstance();
        mChooseDate = (TextView) findViewById(R.id.eventDatePicker);
        mChooseDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Calendar cal = Calendar.getInstance();
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog dialog = new DatePickerDialog(CreateEvent.this,
                        android.R.style.Theme_DeviceDefault_Dialog_NoActionBar,
                        mDateSetListener,
                        year, month,day);
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                dialog.show();
            }
        });

        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                month = month +1;
                String date =year + "/" + month + "/" +dayOfMonth;
                eventChoosenDate = date;

                mChooseDate.setText(date);
            }
        };

        // get Owner from Session
        owner = session.getUsername();

        btn_create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createEvent();
            }
        });
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(CreateEvent.this, DashboardActivity.class));
            }
        });
    }

    protected void createEvent() {
        btn_create.setVisibility(View.GONE);
        final String eventName=this.eventName.getText().toString().trim();
        final String description=this.description.getText().toString().trim();
        final String country=this.country.getText().toString().trim();
        final String city=this.city.getText().toString().trim();
        final String street=this.street.getText().toString().trim();
        Event event = new Event(owner, eventName,description,street,city,country, eventChoosenDate.toString());

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mDb.setFirestoreSettings(settings);
        mDb.collection("Events")
                .add(event)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Map<String, Object> eventID = new HashMap<>();
                        eventID.put("eventID",documentReference.getId());
                        documentReference.set(eventID);
                        Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

    }
}
