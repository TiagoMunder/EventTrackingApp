package pt.ubi.eventtrackingapp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

public class UserInfoActivity extends AppCompatActivity {

    private Session session;
    private TextView userName_TextView, userDate_EditText;
    private ImageView image_ImageV;


    private void initializeUserInfo() {
        userName_TextView.setText(session.getUsername());
        Picasso.get().load(session.getUser().getmImageUrl()).fit().centerCrop().into(image_ImageV);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        session = new Session(UserInfoActivity.this);
        userName_TextView = findViewById(R.id.userName_TextView);
        image_ImageV  = findViewById(R.id.image_ImageV);
        initializeUserInfo();

    }
}
