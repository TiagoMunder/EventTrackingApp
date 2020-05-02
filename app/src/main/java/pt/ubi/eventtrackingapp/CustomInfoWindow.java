package pt.ubi.eventtrackingapp;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public class CustomInfoWindow  implements GoogleMap.InfoWindowAdapter {


    private Context context;

    public CustomInfoWindow(Context ctx){
        context = ctx;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        View view = ((Activity)context).getLayoutInflater()
                .inflate(R.layout.map_custom_infowindow, null);

        TextView infoTitle_TV = view.findViewById(R.id.infoTitle);

        ImageView img = view.findViewById(R.id.infoImage);
        infoTitle_TV.setText(marker.getTitle());
        img.setImageResource(R.drawable.donald);
        return view;
    }
}
