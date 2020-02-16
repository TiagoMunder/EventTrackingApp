package pt.ubi.eventtrackingapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class GetBitMapRunnable implements Runnable {

    private User user;
    private Bitmap bitMap;

    public GetBitMapRunnable(User user, Bitmap bitMap) {
            this.user = user;
            this.bitMap = bitMap;
    }
    @Override
    public void run() {
         this.bitMap = getBitmapFromURL(user.getmImageUrl());

    }

    public Bitmap getAvatar() {
        return this.bitMap;
    }

    public static Bitmap getBitmapFromURL(String imgUrl) {
        try {
            URL url = new URL("https://firebasestorage.googleapis.com/v0/b/eventtacking.appspot.com/o/uploads%2F1581277370519.jpg?alt=media&token=0954bbf1-3549-4765-bcdd-b1d7a1dc610b");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(input);
            Bitmap myBitmap = bmp;

            return myBitmap;
        } catch (IOException e) {
            // Log exception
            return null;
        }
    }
}
