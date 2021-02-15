package pt.ubi.eventtrackingapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.widget.ImageView;

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

import java.util.HashMap;
import java.util.concurrent.Executors;

public class ImageMarkerClusterManagerRenderer extends DefaultClusterRenderer<ImageMarkerClusterItem> {

    private final int markerWidth;
    private final int markerHeight;
    private final Context context;
    private final ImageView imageView;
    private final IconGenerator iconGenerator;
    private HashMap<String, ImageMarkerClusterItem> extraMarkerInfo = new HashMap<String, ImageMarkerClusterItem>();

    public ImageMarkerClusterManagerRenderer(Context context, GoogleMap map, ClusterManager<ImageMarkerClusterItem> clusterManager) {
        super(context, map, clusterManager);
        this.context = context.getApplicationContext();

        imageView = new ImageView(context.getApplicationContext());
        iconGenerator = new IconGenerator(context.getApplicationContext());

        markerWidth = (int) context.getResources().getDimension(R.dimen.custom_marker_image);
        markerHeight = (int) context.getResources().getDimension(R.dimen.custom_marker_image);

    }

    @Override
    protected void onBeforeClusterItemRendered(ImageMarkerClusterItem item, MarkerOptions markerOptions) {
        markerOptions.visible(false);
    }

    protected boolean shouldRenderAsCluster(Cluster<ImageMarkerClusterItem> cluster) {
      return false;
    }

    /**
     * Update the GPS coordinate of a ClusterItem
     * @param clusterMarker
     */
    public void setUpdateMarker(ImageMarkerClusterItem clusterMarker) {
        Marker marker = getMarker(clusterMarker);
        if (marker != null) {
            marker.setPosition(clusterMarker.getPosition());
        }
    }

    public  HashMap<String, ImageMarkerClusterItem> getExtraMarkerInfo() {
        return extraMarkerInfo;
    }

    public void clearExtraMarkerInfo() {
        extraMarkerInfo.clear();
    }



    @Override
    protected void onClusterItemRendered(final ImageMarkerClusterItem item, final Marker marker) {
        if(marker.getTag() == null)
            marker.setTag(ImageMarkerClusterItem.class);
        Picasso picasso = new Picasso.Builder(context).executor(Executors.newSingleThreadExecutor()).memoryCache(Cache.NONE).indicatorsEnabled(true).build();

        picasso.load(item.getIconPicture()).resize(80, 80).centerCrop().into(imageView, new Callback() {
            @Override
            public void onSuccess() {
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                Bitmap bitmap = drawable.getBitmap();
                if(marker != null && marker.getTag() != null) {
                    marker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
                    marker.setVisible(true);
                    extraMarkerInfo.put(marker.getId(), item);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.d("ImageMarkerCluster", e.getMessage());

            }

        });
    }



}
