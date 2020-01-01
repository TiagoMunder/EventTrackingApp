package pt.ubi.eventtrackingapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class DashboardActivity extends AppCompatActivity {

    private Button btn_createEvent, btn_ListEvents, btn_participateEvent;
    private String username;
    private TextView link_logOut;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        btn_createEvent = findViewById(R.id.btn_createEvent);
        btn_ListEvents = findViewById(R.id.btn_listEvents);
        btn_participateEvent = findViewById(R.id.btn_participateEvent);
        link_logOut = findViewById(R.id.link_logOut);

        btn_ListEvents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, EventsListActivity.class));
            }
        });

        btn_createEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, CreateEvent.class));
            }
        });

        btn_participateEvent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DashboardActivity.this, MapViewActivity.class));
            }
        });

        link_logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                        mAuth = FirebaseAuth.getInstance();
                        mAuth.signOut();
                        startActivity(new Intent(DashboardActivity.this, LoginActivity.class));
                    }
            });

    }
}
