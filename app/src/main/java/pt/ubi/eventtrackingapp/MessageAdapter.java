package pt.ubi.eventtrackingapp;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class MessageAdapter extends ArrayAdapter<Message> {
    private Context mContext;
    private static final String TAG = "MessageAdapter";
    private int lastPosition = -1;


    private static class ViewHolder {
        TextView messageBody, name;
        View avatar;
    }

    public MessageAdapter(Context context, int resource, ArrayList<Message> objects) {
        super(context, resource, objects);
        mContext = context;

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        String messageBody = getItem(position).getMessageBody();
        String owner = getItem(position).getMessageOwner();
        Boolean sendByUs = getItem(position).getSendByUs();
        String date = getItem(position).getDate();
        String eventId = getItem(position).getEventId();
        Boolean isAdmin = getItem(position).getIsAdmin();


        Message message = new Message(messageBody, owner, sendByUs,eventId,date, isAdmin);

        final View result;
        ViewHolder holder;

        LayoutInflater inflater = LayoutInflater.from(mContext);

        holder= new ViewHolder();
        if(sendByUs) {
            convertView = inflater.inflate(R.layout.my_message,null);
            holder.messageBody = (TextView) convertView.findViewById(R.id.message_body);
            holder.messageBody.setText(messageBody);

        } else {
            convertView = inflater.inflate(R.layout.others_messages,null);
            holder.messageBody = (TextView) convertView.findViewById(R.id.message_body);
            holder.name = (TextView) convertView.findViewById(R.id.name);
            holder.name.setText(owner);
            holder.avatar = (View) convertView.findViewById(R.id.avatar);
            holder.messageBody.setText(messageBody);
            GradientDrawable drawable = (GradientDrawable) holder.avatar.getBackground();
            if(isAdmin) drawable.setColor(Color.parseColor("#eaed2d"));
            else drawable.setColor(Color.parseColor("#7d7d6f"));


        }
        result = convertView;
        convertView.setTag(holder);

        Animation animation = AnimationUtils.loadAnimation(mContext,
                (position> lastPosition) ? R.anim.loading_down_anim : R.anim.loading_up_anim);
        result.startAnimation(animation);
        lastPosition = position;

        return convertView;
    }
}
