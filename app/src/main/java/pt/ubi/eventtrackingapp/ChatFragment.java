package pt.ubi.eventtrackingapp;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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




public class ChatFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private EditText editChatForm;
    private static final String TAG = "ChatActivity";
    private FirebaseFirestore mDb;
    private ImageButton btn_send;
    private Session session;
    private String  eventID;
    private ArrayList<User> userList;
    private ArrayList<UserLocation> mUserListLocation;
    private ListView myListView;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;


    public ChatFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null) {
            userList = getArguments().getParcelableArrayList("UsersList");
            eventID = getArguments().getString("EventId");
        }
        mDb = FirebaseFirestore.getInstance();
        session = new Session(getActivity());
        getMessagesFromServer();



    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        editChatForm = (EditText) view.findViewById(R.id.chatForm);
        btn_send = (ImageButton) view.findViewById(R.id.btn_send);
         myListView = (ListView) view.findViewById(R.id.messages_view);

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

        // Inflate the layout for this fragment



        return view;

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

                        ArrayList<Message> messageList = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {

                            if (doc.get("messageBody") != null && doc.get("sender") != null  && doc.get("time") != null) {
                                boolean sendByUs = doc.get("sender").equals(session.getUsername());
                                Message message = new Message(doc.get("sender").toString(), doc.get("messageBody").toString(),sendByUs, doc.get("eventId").toString(), doc.get("time").toString());
                                messageList.add(message);
                            }
                        }
                        MessageAdapter adapter = new MessageAdapter(getActivity(),0, messageList);
                        myListView.setAdapter(adapter);
                    }
                });
    }
}
