package pt.ubi.eventtrackingapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.firestore.DocumentReference;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import static pt.ubi.eventtrackingapp.Constants.CHATCOLLECTION;
import static pt.ubi.eventtrackingapp.Constants.EVENTSCOLLECTION;

public class ChatActivity extends AppCompatActivity {

    private EditText editChatForm;
    private static final String TAG = "ChatActivity";
    private FirebaseFirestore mDb;
    private ImageButton btn_send;
    private Session session;
    private String  eventID;
    private String currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        editChatForm = (EditText) findViewById(R.id.chatForm);
        btn_send = (ImageButton) findViewById(R.id.btn_send);
        mDb = FirebaseFirestore.getInstance();
        session = new Session(ChatActivity.this);
        currentUser = this.session.getUsername();
        Intent intent = getIntent();
        eventID = intent.getStringExtra("eventID");
        if(eventID == null){
            Log.d(TAG, " Error getting Event");
            startActivity(new Intent(ChatActivity.this, DashboardActivity.class));
        }



        session = new Session(ChatActivity.this);
        if(session.getEvent().isClosed()){
            editChatForm.setVisibility(View.INVISIBLE);
            btn_send.setVisibility(View.INVISIBLE);
        }

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

    protected void onResume() {
        super.onResume();
        getMessagesFromServer();
    }

    private void addMessage(String messageBody) {
        Long tsLong = System.currentTimeMillis()/1000;
        MessageServer message = new MessageServer(currentUser, messageBody, eventID, tsLong.toString());
        DocumentReference newMessageRef = mDb.collection(EVENTSCOLLECTION).document(eventID).collection(CHATCOLLECTION).document();

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
        mDb.collection(EVENTSCOLLECTION).document(eventID).collection(CHATCOLLECTION)
                .whereEqualTo("eventId", eventID).orderBy("time")
                .addSnapshotListener(ChatActivity.this, new EventListener<QuerySnapshot>() {
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
                                boolean sendByUs = doc.get("sender").equals(currentUser);
                                boolean isAdmin = doc.get("sender").equals(session.getEvent().getOwner());
                                Message message = new Message(doc.get("sender").toString(), doc.get("messageBody").toString(),
                                        sendByUs, doc.get("eventId").toString(), doc.get("time").toString(), isAdmin);
                                messageList.add(message);
                            }
                        }
                        MessageAdapter adapter = new MessageAdapter(ChatActivity.this,0, messageList);
                        myListView.setAdapter(adapter);
                    }
                });
    }


}
