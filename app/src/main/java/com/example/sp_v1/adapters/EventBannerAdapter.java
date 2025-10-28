package com.example.sp_v1.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.sp_v1.R;
import com.example.sp_v1.models.EventPost;

import java.util.List;

public class EventBannerAdapter extends RecyclerView.Adapter<EventBannerAdapter.BannerViewHolder> {

    private final Context mContext;
    private final List<EventPost> mEventList;
    private final OnEventClickListener mListener;

    // Interface for click events
    public interface OnEventClickListener {
        void onEventClick(EventPost eventPost);
    }

    public EventBannerAdapter(Context context, List<EventPost> eventList, OnEventClickListener listener) {
        mContext = context;
        mEventList = eventList;
        mListener = listener;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_event_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        EventPost event = mEventList.get(position);

        // Load banner image using Glide
        Glide.with(mContext)
                .load(event.getBannerImageUrl())
                .placeholder(R.drawable.avatar_placeholder)
                .into(holder.bannerImage);

        // Set the click listener
        holder.itemView.setOnClickListener(v -> mListener.onEventClick(event));
    }

    @Override
    public int getItemCount() {
        return mEventList.size();
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        ImageView bannerImage;

        BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            bannerImage = itemView.findViewById(R.id.event_banner_image);
        }
    }
}