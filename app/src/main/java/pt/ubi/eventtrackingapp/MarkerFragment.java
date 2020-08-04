package pt.ubi.eventtrackingapp;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MarkerFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MarkerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MarkerFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "geoPoint";
    private static final String ARG_PARAM2 = "imageMarker";
    private Button mButtonChooseImage;
    private Button mButtonBack;
    private Button mButtonSave;
    private ImageView mImageView;
    private EditText imageName;
    private EditText imageDescription;

    // TODO: Rename and change types of parameters
    private CustomGeoPoint geoPoint;
    private ImageMarkerClusterItem imageMarker;

    private StorageReference fileReference;
    private Uri mImageUri;
    private Session session;

    private StorageReference mStorageRef;
    private FirebaseFirestore mDb;


    private static final String TAG = "MarkerFragment";
    private static final int PICK_IMAGE_REQUEST = 1;


    private OnFragmentInteractionListener mListener;

    private ArrayList<User> mImageCaptureUri = new ArrayList<>();
    private Fragment MyFragment;
    private boolean imageHasChanged = false;

    public MarkerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param geoPoint Parameter 1.
     * @param imageMarker Parameter 2.
     * @return A new instance of fragment MarkerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MarkerFragment newInstance(CustomGeoPoint geoPoint, String imageMarker) {

        MarkerFragment fragment = new MarkerFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAM1, geoPoint);
        args.putString(ARG_PARAM2, imageMarker);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MyFragment = (MarkerFragment) getActivity().getSupportFragmentManager().findFragmentByTag("markerFragment");
        mDb = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        mDb.setFirestoreSettings(settings);
        // MyFragment = getActivity().getSupportFragmentManager().getFragment(savedInstanceState, "markerFragment");
        if (getArguments() != null) {
            geoPoint = getArguments().getParcelable(ARG_PARAM1);
            imageMarker = getArguments().getParcelable(ARG_PARAM2);
        }
        Log.d(TAG, "addMapMarkers: location: " + geoPoint.getLatitude() + ' ' + geoPoint.getLongitude());

        mStorageRef = FirebaseStorage.getInstance().getReference("uploadsMap");
        session = new Session(getContext());



    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mImageView = getView().findViewById(R.id.image_view);
        if(imageMarker != null && imageMarker.getIconPicture() != null){
            setPreviousInfo();
        }
        super.onViewCreated(view, savedInstanceState);
    }

    public void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        MyFragment.startActivityForResult(intent,PICK_IMAGE_REQUEST);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            imageHasChanged = true;
            mImageUri = data.getData();
            mImageView.setImageURI(mImageUri);
        }
    }

    private void setPreviousInfo() {

        Picasso.get().load(imageMarker.getIconPicture()).fit().centerCrop().into(mImageView);
        imageDescription.setText(imageMarker.getDescription());
        imageName.setText(imageMarker.getTitle());

    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContext().getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState !=null && savedInstanceState.containsKey("cameraMediaOutputUri"))
            mImageCaptureUri = savedInstanceState.getParcelableArrayList("cameraMediaOutputUri");
    }



    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putParcelableArrayList("cameraMediaOutputUri", mImageCaptureUri);
        super.onSaveInstanceState(outState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_marker, container, false);
        mButtonChooseImage = view.findViewById(R.id.button_choose_image);
        mButtonBack = view.findViewById(R.id.button_back);
        mImageView = view.findViewById(R.id.image_view);
        mButtonSave = view.findViewById(R.id.button_save);
        imageName = view.findViewById(R.id.edit_text_file_name);
        imageDescription =  view.findViewById(R.id.edit_text_description);

        mButtonChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        mButtonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SaveImage();
            }
        });

        mButtonBack.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {

                goBack();
            }
        });

        return view;
    }

    public void SaveImage() {
            if(imageHasChanged) {
                fileReference = mStorageRef.child(System.currentTimeMillis()
                        + "." + getFileExtension(mImageUri));

                fileReference.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                                          @Override
                                                                          public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                                              fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                                                  @Override
                                                                                  public void onSuccess(Uri uri) {

                                                                                      saveEditMarkerObject(uri);
                                                                                  }
                                                                              });

                                                                          }
                                                                      }
                ).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getContext(), "Failed to save image in Storage! ", Toast.LENGTH_SHORT);
                    }
                });
            } else  saveEditMarkerObject(null);
    }

    @SuppressWarnings("unchecked")
   public void saveEditMarkerObject(Uri uri)  {
        if(imageMarker != null) {

          HashMap<String,Object> markerHashMap = new HashMap();
            markerHashMap.put("imageUrl", uri != null ? uri.toString() :  (imageMarker != null ? imageMarker.getIconPicture() : ' '));
            markerHashMap.put("description", imageDescription.getText().toString());
            markerHashMap.put("imageName", imageName.getText().toString());
            if(imageMarker.getId() != null) {
                mDb.collection("ImageMarkers").document(imageMarker.getId())
                        .update(markerHashMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Update Successful");
                            goBack();
                        } else {
                            Log.w(TAG, "Error updating document", task.getException());
                        }
                    }
                });

            } else {
                Toast.makeText(getContext(), "There was a problem getting the image Marker!", Toast.LENGTH_SHORT).show();
            }
        } else {

            // Não sei se é muito correcto o objecto ter o id a null neste caso apesar de eu depois ir buscar o id ao document e não ao objecto
            MarkerObject markerObject = new MarkerObject(geoPoint, session.getUser().getUser_id(), uri.toString(), session.getEvent().getEventID(),
                    imageDescription.getText().toString(), imageName.getText().toString(), null);

            mDb.collection("ImageMarkers")
                    .add(markerObject)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {

                            Log.d(TAG, "DocumentSnapshot written with ID: " + documentReference.getId());
                            goBack();
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


    public void goBack() {
        getActivity().onBackPressed();

    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


}