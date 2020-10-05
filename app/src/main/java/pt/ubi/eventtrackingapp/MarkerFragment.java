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
import android.text.InputType;
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
import static pt.ubi.eventtrackingapp.Constants.EVENTSCOLLECTION;
import static pt.ubi.eventtrackingapp.Constants.IMAGEMARKERSCOLLECTION;


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
    private static final String ARG_PARAM2 = "isNewImage";
    private static final String ARG_PARAM3 = "filteredImages";
    private static final String ARG_PARAM4 = "currentPosition";
    private Button mButtonChooseImage;
    private Button mButtonBack;
    private Button mButtonSave;
    private ImageView mImageView;
    private EditText imageName;
    private EditText imageDescription;


    private CustomGeoPoint geoPoint;
    private boolean isNewImage;

    private StorageReference fileReference;
    private Uri mImageUri;
    private Session session;

    private StorageReference mStorageRef;
    private FirebaseFirestore mDb;


    private static final String TAG = "MarkerFragment";
    private static final int PICK_IMAGE_REQUEST = 1;


    private OnFragmentInteractionListener mListener;

    private ArrayList<User> mImageCaptureUri = new ArrayList<>();
    private ArrayList<MarkerObject> filteredImages = new ArrayList<>();
    private Fragment MyFragment;
    private boolean imageHasChanged = false;

    private int currentPosition;
    private MarkerObject currentImage;

    private String eventID;

    public MarkerFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param geoPoint Parameter 1.
     * @param isNewImage Parameter 2.
     * @return A new instance of fragment MarkerFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MarkerFragment newInstance(CustomGeoPoint geoPoint, boolean isNewImage, ArrayList<MarkerObject> filteredImages, int currentPosition) {

        MarkerFragment fragment = new MarkerFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAM1, geoPoint);
        args.putBoolean(ARG_PARAM2, isNewImage);
        args.putParcelableArrayList(ARG_PARAM3, filteredImages);
        args.putInt(ARG_PARAM4, currentPosition);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDb = FirebaseFirestore.getInstance();
        if (getArguments() != null) {
            geoPoint = getArguments().getParcelable(ARG_PARAM1);
            isNewImage = getArguments().getBoolean(ARG_PARAM2);
            filteredImages = getArguments().getParcelableArrayList(ARG_PARAM3);
            currentPosition = getArguments().getInt(ARG_PARAM4);

        }

        if(isNewImage) {
            MyFragment = (MarkerFragment) getActivity().getSupportFragmentManager().findFragmentByTag("imageFragment");
        } else MyFragment = (MarkerFragment)getActivity().getSupportFragmentManager().findFragmentByTag("android:switcher:viewPager:" + currentPosition);

        currentImage =  isNewImage  ? null : filteredImages.get(currentPosition);
        mStorageRef = FirebaseStorage.getInstance().getReference("uploadsMap");
        session = new Session(getContext());
        eventID = session.getEvent().getEventID();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mImageView = getView().findViewById(R.id.image_view);
        if(currentImage != null && currentImage.getImageUrl() != null){
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

        Picasso.get().load(currentImage.getImageUrl()).fit().centerCrop().into(mImageView);
        imageDescription.setText(currentImage.getDescription());
        imageName.setText(currentImage.getImageName());

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

    public void canOnlyViewImage() {
        mButtonBack.setVisibility(View.INVISIBLE);
        mButtonChooseImage.setVisibility(View.INVISIBLE);
        mButtonSave.setVisibility(View.INVISIBLE);
        imageDescription.setInputType(InputType.TYPE_NULL);
        imageName.setInputType(InputType.TYPE_NULL);

    }

    public void canDeleteImage() {
        mButtonSave.setText(R.string.delete_image);
        mButtonSave.setVisibility(View.VISIBLE);
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

        if(!isNewImage)
            this.canOnlyViewImage();

        boolean isOwner = session.getUser().getUser_id().equals(currentImage.getUser_id());
        if(isOwner && !this.isNewImage)
            canDeleteImage();

        mButtonChooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFileChooser();
            }
        });

        mButtonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isNewImage)
                    SaveImage();
                else deleteImage();
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

    public void deleteImage() {

            mDb.collection(EVENTSCOLLECTION).document(eventID).collection(IMAGEMARKERSCOLLECTION).document(currentImage.getImageId()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()) {
                        Toast.makeText(getContext(), "Image was deleted successfully!", Toast.LENGTH_SHORT);
                        goBack();
                    }
                }
            });

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
        if(currentImage != null) {

          HashMap<String,Object> markerHashMap = new HashMap();
            markerHashMap.put("imageUrl", uri != null ? uri.toString() :  (currentImage.getImageUrl() != null ? currentImage.getImageUrl()  : ' '));
            markerHashMap.put("description", imageDescription.getText().toString());
            markerHashMap.put("imageName", imageName.getText().toString());
            if(currentImage != null) {
                mDb.collection(EVENTSCOLLECTION).document(eventID).collection(IMAGEMARKERSCOLLECTION).document(currentImage.getId())
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
                    imageDescription.getText().toString(), imageName.getText().toString(), null, null);

            mDb.collection(EVENTSCOLLECTION).document(eventID).collection(IMAGEMARKERSCOLLECTION)
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