package pt.ubi.eventtrackingapp;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;


public class RegisterActivity extends AppCompatActivity {
    private TextInputEditText name, email, password, c_password, age, phoneNumber, username, nationality;
    private Button btn_regist;
    private ImageView mUserImage;

    private Uri mImageUri;


    private StorageReference mStorageRef;


    private String imageUri = "";

    private Session session;
    private FirebaseFirestore mDb;
    private  StorageReference fileReference;

    private static final String TAG = "RegisterActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mStorageRef = FirebaseStorage.getInstance().getReference("uploads");
        // name = findViewById(R.id.name);
        username = findViewById(R.id.Username);
        session = new Session(RegisterActivity.this);
        // phoneNumber = findViewById(R.id.phoneNumber);
        email = findViewById(R.id.email);
        //age = findViewById(R.id.age);
        // nationality = findViewById(R.id.nationality);
        password =  findViewById(R.id.password);
        c_password = findViewById(R.id.c_password);
        btn_regist = findViewById(R.id.btn_regist);
        mUserImage = findViewById(R.id.image_view);


        mDb = FirebaseFirestore.getInstance();
       // final String name=this.name.getText().toString().trim();

      //  final String age=this.age.getText().toString().trim();

      //  final String phoneNumber=this.phoneNumber.getText().toString().trim();
      //  final String nationality=this.nationality.getText().toString().trim();

        btn_regist.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(validateInfo())
                registerNewEmail();
            }
        });
        mUserImage.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseUserPicture();
            }
        });
    }

    public void chooseUserPicture() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK
                && data != null && data.getData() != null){
            mImageUri = data.getData();
            mUserImage.setImageURI(mImageUri);



        }
    }

    private boolean validateInfo() {
        final String password=this.password.getText().toString().trim();
        final String c_password = this.c_password.getText().toString().trim();
        if(password.length() < 6) {
            Log.d(TAG, "The password needs at least 6 characters!");
            Toast.makeText(RegisterActivity.this, "The password needs at least 6 characters!" , Toast.LENGTH_SHORT);
            return false;
        }
        if(!password.equals(c_password)) {
            Log.d(TAG, "The passwords do not match!");
            Toast.makeText(RegisterActivity.this, "The passwords do not match! " , Toast.LENGTH_SHORT);
            return false;
        }

        return true;
    }

    private String getFileExtension(Uri uri) {
        ContentResolver cR = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(cR.getType(uri));
    }



    public void registerNewEmail(){
        // i will change this because i need to send default image if the user doesn't upload any image
        // what i will probably do is have a default image in Storage and just send the imageUrl of that image in that case
        if(mImageUri!=null) {
          fileReference  = mStorageRef.child(System.currentTimeMillis()
                    + "." + getFileExtension(mImageUri));
            fileReference.putFile(mImageUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                                                      @Override
                                                                      public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                                                          fileReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                                                              @Override
                                                                              public void onSuccess(Uri uri) {
                                                                                   createUserWithFirebase(uri);
                                                                              }
                                                                          });

                                                                      }
                                                                  }
            ).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(RegisterActivity.this, "Failed to save image in Storage! ", Toast.LENGTH_SHORT);
                }
            });

        }

    }



   public void createUserWithFirebase(Uri imageURL) {
        final String imageURLFinal = imageURL.toString();
       final String email=this.email.getText().toString().trim();
       final String password=this.password.getText().toString().trim();

       FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
               .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                   @Override
                   public void onComplete(@NonNull Task<AuthResult> task) {
                       Log.d(TAG, "createUserWithEmail:onComplete:" + task.isSuccessful());
                       if (task.isSuccessful()){
                           Log.d(TAG, "onComplete: AuthState: " + FirebaseAuth.getInstance().getCurrentUser().getUid());
                           User user = new User();
                           user.setEmail(email);
                           user.setUsername(username.getText().toString().trim());
                           user.setUser_id(FirebaseAuth.getInstance().getUid());
                          user.setmImageUrl(imageURLFinal);

                           user.setPassword(password);
                           session.setUserInfo(user);

                           DocumentReference newUserRef = mDb
                                   .collection(getString(R.string.fire_store_users))
                                   .document(FirebaseAuth.getInstance().getUid());

                           FirebaseUser userteste = FirebaseAuth.getInstance().getCurrentUser();
                           if (userteste != null) {
                               Log.d(TAG,"working");

                           }

                           newUserRef.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
                               @Override
                               public void onComplete(@NonNull Task<Void> task) {
                                   btn_regist.setVisibility(View.VISIBLE);

                                   if(task.isSuccessful()){
                                       Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
                                       loginIntent.putExtra("email",email);
                                       loginIntent.putExtra("username", username.getText().toString().trim());
                                       startActivity(loginIntent);
                                   }else{
                                       Toast.makeText(RegisterActivity.this, "Register Failed! " , Toast.LENGTH_SHORT);
                                   }
                               }
                           });
                       }
                       else {

                           Toast.makeText(RegisterActivity.this, "Register Failed!", Toast.LENGTH_SHORT);
                           btn_regist.setVisibility(View.VISIBLE);
                       }


                   }
               });

    }

}
