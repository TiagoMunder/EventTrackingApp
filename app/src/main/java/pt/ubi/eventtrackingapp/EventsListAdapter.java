package pt.ubi.eventtrackingapp;



import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class EventsListAdapter extends ArrayAdapter<Event> {
    private Context mContext;
    private static final String TAG = "EventListAdapter";
    private int mResource;
    private int lastPosition = -1;


    private static class ViewHolder {
        TextView name, owner, description, eventDate, location;

    }

    public EventsListAdapter(Context context, int resource, ArrayList<Event> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String name = getItem(position).getName();
        String owner = getItem(position).getOwner();
        String description = getItem(position).getDescription();
        String event_date = getItem(position).getDate();
        String city = getItem(position).getCity() ;
        String country = getItem(position).getCountry();
        String street = getItem(position).getStreet() ;
        String location = street + ','+ city + ',' + country;
        final View result;
        ViewHolder holder;
        if(convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(mResource, parent,false);

            holder= new ViewHolder();
            holder.name = (TextView) convertView.findViewById(R.id.event_name);
            holder.owner = (TextView) convertView.findViewById(R.id.owner);
            holder.description = (TextView) convertView.findViewById(R.id.description);
            holder.eventDate = (TextView) convertView.findViewById(R.id.date);
            holder.location = (TextView) convertView.findViewById(R.id.location);
            result = convertView;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            result = convertView;
        }


        Animation animation = AnimationUtils.loadAnimation(mContext,
                (position> lastPosition) ? R.anim.loading_down_anim : R.anim.loading_up_anim);
        result.startAnimation(animation);
        lastPosition = position;

        holder.name.setText(name);
        holder.owner.setText(owner);
        holder.description.setText(description);
        holder.eventDate.setText(event_date);
        holder.location.setText(location);
        return convertView;
    }
}