package pt.ubi.eventtrackingapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;
import com.google.maps.android.ui.IconGenerator;
import com.squareup.picasso.Cache;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;

public class myClusterManagerRenderer extends DefaultClusterRenderer<MyClusterItem> {


    private final int markerWidth;
    private final int markerHeight;
    private final Context context;
    private final ImageView imageView;
    private final IconGenerator iconGenerator;
    private HashMap<String, MyClusterItem> extraMarkerInfo = new HashMap<String, MyClusterItem>();



    public myClusterManagerRenderer(Context context, GoogleMap map, ClusterManager<MyClusterItem> clusterManager) {
        super(context, map, clusterManager);
        this.context = context.getApplicationContext();
        imageView = new ImageView(context.getApplicationContext());
        iconGenerator = new IconGenerator(context.getApplicationContext());


        markerWidth = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
        markerHeight = (int) context.getResources().getDimension(R.dimen.custom_marker_image);


    }




    @Override
    protected void onBeforeClusterItemRendered(MyClusterItem item, MarkerOptions markerOptions) {
        markerOptions.visible(false);
    }

    /**
     * Update the GPS coordinate of a ClusterItem
     * @param clusterMarker
     */
    public void setUpdateMarker(MyClusterItem clusterMarker) {
        Marker marker = getMarker(clusterMarker);
        if (marker != null) {
            marker.setPosition(clusterMarker.getPosition());
        }
    }

    public void setUpdateMarkerSnippet(MyClusterItem clusterMarker, String snippet) {
        Marker marker = getMarker(clusterMarker);
        if (marker != null) {
            marker.setSnippet(snippet);
        } else clusterMarker.setSnippet(snippet);
    }

    public  HashMap<String, MyClusterItem> getExtraMarkerInfo() {
        return extraMarkerInfo;
    }



    @Override
    protected void onClusterItemRendered(final MyClusterItem item, final Marker marker) {

        Picasso picasso = new Picasso.Builder(context).executor(Executors.newSingleThreadExecutor()).memoryCache(Cache.NONE).indicatorsEnabled(true).build();

        picasso.load(item.getIconPicture()).resize(markerWidth, markerHeight).centerCrop().into(imageView, new Callback() {
            @Override
            public void onSuccess() {
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                bitmap = getCircledBitmap(bitmap, item, context);
                marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
                marker.setVisible(true);
                marker.setTag(MyClusterItem.class);
                extraMarkerInfo.put(marker.getId(), item);
            }


            @Override
            public void onError(Exception e) {
                Log.d("MyClusterManagerRender", e.getMessage());
                marker.setVisible(true);
            }

        });
    }
    public static Bitmap getCircledBitmap(Bitmap bitmap, MyClusterItem item, Context context) {
        Session session = new Session(context);
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        Paint borderPaint = new Paint();
        int borderColor = item.getUser().getUsername().equals(session.getEvent().getOwner()) ? Color.YELLOW : Color.RED;
        borderPaint.setColor(borderColor);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setAntiAlias(true);
        borderPaint.setStrokeWidth(2);
        canvas.drawCircle(bitmap.getWidth()/2, bitmap.getHeight()/2, bitmap.getWidth() / 2, borderPaint);


        return output;
    }

}
