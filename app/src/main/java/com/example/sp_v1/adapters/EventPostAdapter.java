package com.example.sp_v1.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sp_v1.R;
import com.example.sp_v1.models.EventPost;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EventPostAdapter extends RecyclerView.Adapter<EventPostAdapter.EventViewHolder> {

    private final Context mContext;
    private final List<EventPost> mEventList;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public EventPostAdapter(Context context, List<EventPost> eventList) {
        mContext = context;
        mEventList = eventList;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_event_post, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventPost event = mEventList.get(position);

        holder.eventName.setText(event.getEventName());

        // Format and set dates
        String dateRange = sdf.format(event.getStartDate()) + " - " + sdf.format(event.getEndDate());
        holder.eventDates.setText(dateRange);

        // Set Status text and color
        holder.eventStatus.setText(event.getStatus());
        holder.eventStatus.setBackground(getStatusDrawable(event.getStatus()));

        // Load banner image using Glide
        Glide.with(mContext)
                .load(event.getBannerImageUrl())
                .placeholder(R.drawable.avatar_placeholder) // You have this drawable
                .error(R.drawable.ic_launcher_background) // Fallback
                .into(holder.bannerImage);
    }

    private GradientDrawable getStatusDrawable(String status) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(20); // Make it rounded

        int color;
        switch (status) {
            case EventPost.STATUS_ACTIVE:
                color = ContextCompat.getColor(mContext, R.color.bright_green); // You'll need to define this color
                break;
            case EventPost.STATUS_REJECTED:
                color = ContextCompat.getColor(mContext, R.color.bright_red); // You have this
                break;
            case EventPost.STATUS_PENDING_APPROVAL:
                color = ContextCompat.getColor(mContext, R.color.bright_yellow); // You have this
                break;
            default: // EXPIRED or other
                color = Color.GRAY;
                break;
        }
        drawable.setColor(color);
        return drawable;
    }

    @Override
    public int getItemCount() {
        return mEventList.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImage;
        TextView eventName, eventDates, eventStatus;

        EventViewHolder(@NonNull View itemView) {
            super(itemView);
            bannerImage = itemView.findViewById(R.id.event_banner_image);
            eventName = itemView.findViewById(R.id.event_name_text);
            eventDates = itemView.findViewById(R.id.event_dates_text);
            eventStatus = itemView.findViewById(R.id.event_status_text);
        }
    }
}