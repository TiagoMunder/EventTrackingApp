package pt.ubi.eventtrackingapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    private EditText editChatForm;
    private static final String TAG = "ChatActivity";
    private FirebaseFirestore mDb;
    private ImageButton btn_send;
    private Session session;
    private String  eventID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        editChatForm = (EditText) findViewById(R.id.chatForm);
        btn_send = (ImageButton) findViewById(R.id.btn_send);
        mDb = FirebaseFirestore.getInstance();
        Intent intent = getIntent();

        eventID = intent.getStringExtra("eventID");
        if(eventID == null){
            Log.d(TAG, " Error getting Event");
            startActivity(new Intent(ChatActivity.this, DashboardActivity.class));
        }

        getMessagesFromServer();

        session = new Session(ChatActivity.this);
        btn_send.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String messageBody =   editChatForm.getText().toString().trim();;

                if(!messageBody.isEmpty()) {
                    addMessage(messageBody);
                } else {
                    Log.d(TAG, "Missing message text!");
                }

            }
        });

    }

    private void addMessage(String messageBody) {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mDb.setFirestoreSettings(settings);
        Long tsLong = System.currentTimeMillis()/1000;
        MessageServer message = new MessageServer("Tiago", messageBody, eventID, tsLong.toString());
        DocumentReference newMessageRef = mDb.collection("Chat").document();

        newMessageRef.set(message).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    editChatForm.getText().clear();
                }else {
                    Log.d(TAG, "Error sending Message!");
                }
            }
        });
    }

    protected void getMessagesFromServer() {
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
            mDb.setFirestoreSettings(settings);
        mDb.collection("Chat")
                .whereEqualTo("eventId", eventID).orderBy("time")
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed.", e);
                            return;
                        }
                        ListView myListView = (ListView) findViewById(R.id.messages_view);
                        ArrayList<Message> messageList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {

                            if (doc.get("messageBody") != null && doc.get("sender") != null  && doc.get("time") != null) {
                                boolean sendByUs = doc.get("sender").equals(session.getUsername());
                                Message message = new Message(doc.get("sender").toString(), doc.get("messageBody").toString(),sendByUs, doc.get("eventId").toString(), doc.get("time").toString());
                                messageList.add(message);
                            }
                        }
                        MessageAdapter adapter = new MessageAdapter(ChatActivity.this,0, messageList);
                        myListView.setAdapter(adapter);
                    }
                });
    }
}
