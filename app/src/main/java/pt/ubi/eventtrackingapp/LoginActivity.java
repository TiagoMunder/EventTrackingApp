package pt.ubi.eventtrackingapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import static pt.ubi.eventtrackingapp.Constants.ERROR_DIALOG_REQUEST;
import static pt.ubi.eventtrackingapp.Constants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION;
import static pt.ubi.eventtrackingapp.Constants.PERMISSIONS_REQUEST_ENABLE_GPS;


public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Session session;
    private EditText email, password;
    private Button btn_login, btn_login_google;
    private RelativeLayout rl_loading, rl_body;
    private TextView link_regist;
    private static final String TAG = "LoginActivity";
    private FirebaseFirestore mDb;
    private boolean mLocationPermissionGranted = false;
    private boolean isEverythingOK = false;
    private FusedLocationProviderClient mFusedLocationClient;
    private UserLocation mUserLocation;
    private ProgressBar spinner;
    private final int RC_SIGN_IN = 3;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount account;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        session = new Session(LoginActivity.this);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btn_login = findViewById(R.id.btn_login);
        btn_login_google = findViewById(R.id.btn_login_google);
        link_regist = findViewById(R.id.link_regist);
        rl_loading = findViewById(R.id.loading);
        rl_body= findViewById(R.id.body);
        mDb = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        FirebaseUser user = mAuth.getCurrentUser();
        Intent registerIntent = getIntent(); // gets the previously created intent
        String createdEmail = registerIntent.getStringExtra("email");
        String createdUsername = registerIntent.getStringExtra("username");

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .build();
        mDb.setFirestoreSettings(settings);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        if (user != null && createdEmail != null && createdUsername != null) {
            saveUser(user.getEmail());
            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
        }



        btn_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mEmail = email.getText().toString().trim();
                String mPassword = password.getText().toString().trim();

                if (!mEmail.isEmpty() || !mPassword.isEmpty()) {
                    Login(mEmail, mPassword);
                } else {
                    email.setError("You need to insert an Email");
                    password.setError("You need to insert an Password ");
                    Toast.makeText(LoginActivity.this, "Signed In!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_login_google.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    LoginWithGoogle();
            }
        });

        link_regist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

    }

    private void setLoading()  {
        rl_loading.setVisibility(View.VISIBLE);
        rl_body.setVisibility(View.INVISIBLE);

    }

    private void turnLoadingOff()  {
        rl_loading.setVisibility(View.INVISIBLE);
        rl_body.setVisibility(View.VISIBLE);

    }


    private void LoginWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    

    private void getLastKnownLocation() {

        Log.d(TAG, "getLastKnownLocation: called");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if(task.isSuccessful()) {
                    Location location  = task.getResult();
                    if(location != null) {
                        CustomGeoPoint geoPoint = new CustomGeoPoint(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "onComplete:  Latitude: " + geoPoint.getLatitude());
                        Log.d(TAG, "onComplete: Longitude: " + geoPoint.getLongitude());
                        mUserLocation.setGeoPoint(geoPoint);
                        //  when passing null firebase already sets the correct timestamp
                        mUserLocation.setTimestamp(null);
                        saveUserLocation();
                        startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                    } else turnLoadingOff();
                } else {
                    turnLoadingOff();
                }
            }
        });
    }


    private void  saveUserLocation() {
        if(mUserLocation != null) {
            DocumentReference locationReference = mDb.collection("User Locations").document(FirebaseAuth.getInstance().getUid());

            locationReference.set(mUserLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG, "onComplete: savedUserLocation ");
                    }
                }
            });
        }
    }



    private void getUserDetails() {
        if(FirebaseAuth.getInstance().getUid() != null){
            setLoading();
            mUserLocation = new UserLocation();
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .build();
            mDb.setFirestoreSettings(settings);
            DocumentReference userRef = mDb.collection("Users").document(FirebaseAuth.getInstance().getUid());
            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()) {
                        Log.d(TAG, "onComplete: sucessfully got the User Details.");
                        User user = task.getResult().toObject(User.class);
                        if(user == null) {
                            Toast.makeText(LoginActivity.this, "There was a problem getting the User!",
                                    Toast.LENGTH_SHORT).show();
                            turnLoadingOff();
                            return;
                        }
                        saveUser(user.getEmail());
                        mUserLocation.setUser(user);
                        getLastKnownLocation();
                    } else {
                        turnLoadingOff();
                        Toast.makeText(LoginActivity.this, "There was a problem getting the User!",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }

    protected  void onResume() {
        super.onResume();
        if(checkMapServices()){
            if(mLocationPermissionGranted){
                isEverythingOK = true;
                 getUserDetails();
            } else{
                getLocationPermission();
            }
        }

    }

    private void Login(final String email, final String password) {


            FirebaseAuth.getInstance().signInWithEmailAndPassword(email,
                    password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "signInWithEmail:success");
                                saveUser(email);
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "signInWithEmail:failure", task.getException());
                                Toast.makeText(LoginActivity.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(LoginActivity.this, "Authentication Failed", Toast.LENGTH_SHORT).show();

                }
            });
    }


    private void saveUser(String email){

            mDb.collection("Users")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    Log.d(TAG, document.getId() + " => " + document.get("username"));
                                    session.setUserInfo(new User(document.get("email").toString().trim() ,document.get("username").toString().trim(),document.getId(),
                                            document.get("mImageUrl")!=null ? document.get("mImageUrl").toString() : null));
                                }
                            } else {
                                Log.d(TAG, "Error getting documents: ", task.getException());

                            }
                        }
                    });
    }


    private boolean checkMapServices(){
        if(isServicesOK()){
            if(isMapsEnabled()){
                return true;
            }
        }
        return false;
    }

    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    // Veriffy if the cellphone has gps enabled
    public boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            isEverythingOK = true;
             getUserDetails();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public boolean isServicesOK(){
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(LoginActivity.this);

        if(available == ConnectionResult.SUCCESS){
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        }
        else if(GoogleApiAvailability.getInstance().isUserResolvableError(available)){
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(LoginActivity.this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        }else{
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: called.");
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ENABLE_GPS: {
                if(mLocationPermissionGranted){
                    isEverythingOK = true;
                    getUserDetails();
                }
                else{
                    getLocationPermission();
                }
                break;
            } case RC_SIGN_IN: {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                     account = task.getResult(ApiException.class);
                    firebaseAuthWithGoogle(account.getIdToken());
                } catch (ApiException e) {
                    Log.w(TAG, "Google sign in failed", e);
                }
                break;
            }

        }

    }

    private void setUserInfoWithGoogle() {
        DocumentReference userRef = mDb.collection("Users").document(FirebaseAuth.getInstance().getUid());
        mUserLocation = new UserLocation();
        userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if(task.isSuccessful()) {
                    Log.d(TAG, "onComplete: sucessfully got the User Details.");
                    User user = task.getResult().toObject(User.class);
                    if(user == null) {
                        addUserInfoToFirebase();
                    }
                    else {
                        saveUser(user.getEmail());
                        mUserLocation.setUser(user);
                        getLastKnownLocation();
                    }
                }
            }
        });

    }

    private void addUserInfoToFirebase() {
        final User user = new User(account.getEmail(), account.getDisplayName(), FirebaseAuth.getInstance().getUid(), account.getPhotoUrl().toString());
        DocumentReference newUserRef = mDb
                .collection(getString(R.string.fire_store_users))
                .document(FirebaseAuth.getInstance().getUid());
        newUserRef.set(user).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    mUserLocation.setUser(user);
                    getLastKnownLocation();
                }else{
                    Toast.makeText(LoginActivity.this, "Logging with Google failed! " , Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        setLoading();
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            setUserInfoWithGoogle();
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            turnLoadingOff();

                        }
                    }
                });
    }



}
