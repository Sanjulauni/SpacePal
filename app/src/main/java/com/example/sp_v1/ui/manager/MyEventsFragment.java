package com.example.sp_v1.ui.manager;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView; // <-- IMPORT ADDED
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager; // <-- IMPORT ADDED
import androidx.recyclerview.widget.RecyclerView;

import com.example.sp_v1.R;
import com.example.sp_v1.adapters.EventPostAdapter; // <-- IMPORT ADDED
import com.example.sp_v1.models.EventPost; // <-- IMPORT ADDED
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth; // <-- IMPORT ADDED
import com.google.firebase.auth.FirebaseUser; // <-- IMPORT ADDED
import com.google.firebase.database.DataSnapshot; // <-- IMPORT ADDED
import com.google.firebase.database.DatabaseError; // <-- IMPORT ADDED
import com.google.firebase.database.DatabaseReference; // <-- IMPORT ADDED
import com.google.firebase.database.FirebaseDatabase; // <-- IMPORT ADDED
import com.google.firebase.database.Query; // <-- IMPORT ADDED
import com.google.firebase.database.ValueEventListener; // <-- IMPORT ADDED

import java.util.ArrayList; // <-- IMPORT ADDED
import java.util.Collections; // <-- IMPORT ADDED
import java.util.List; // <-- IMPORT ADDED

public class MyEventsFragment extends Fragment {

    private RecyclerView eventsRecyclerView;
    private FloatingActionButton fabAddEvent;
    private TextView emptyView; // <-- ADDED

    private EventPostAdapter adapter; // <-- ADDED
    private List<EventPost> eventPostList; // <-- ADDED
    private DatabaseReference eventsRef; // <-- ADDED
    private ValueEventListener eventsListener; // <-- ADDED
    private String currentManagerId; // <-- ADDED

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_events, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        eventsRecyclerView = view.findViewById(R.id.events_recycler_view);
        fabAddEvent = view.findViewById(R.id.fab_add_event);
        emptyView = view.findViewById(R.id.empty_view); // <-- ADDED

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Should not happen, but good to check
            Toast.makeText(getContext(), "Error: Not logged in.", Toast.LENGTH_SHORT).show();
            return;
        }
        currentManagerId = user.getUid(); // <-- ADDED

        setupRecyclerView(); // <-- ADDED
        loadManagerEvents(); // <-- ADDED

        fabAddEvent.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), CreateEventActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setupRecyclerView() {
        eventPostList = new ArrayList<>();
        adapter = new EventPostAdapter(getContext(), eventPostList);
        eventsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventsRecyclerView.setAdapter(adapter);
    }

    private void loadManagerEvents() {
        if (currentManagerId == null) return;

        eventsRef = FirebaseDatabase.getInstance().getReference("event_posts");
        // Query to get only events for this specific manager
        Query managerEventsQuery = eventsRef.orderByChild("managerId").equalTo(currentManagerId);

        eventsListener = managerEventsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventPostList.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                        EventPost event = eventSnapshot.getValue(EventPost.class);
                        if (event != null) {
                            eventPostList.add(event);
                        }
                    }
                    // Show newest events first
                    Collections.reverse(eventPostList);
                    adapter.notifyDataSetChanged();
                    emptyView.setVisibility(View.GONE);
                    eventsRecyclerView.setVisibility(View.VISIBLE);
                } else {
                    // No events found
                    emptyView.setVisibility(View.VISIBLE);
                    eventsRecyclerView.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MyEventsFragment", "Failed to load events: " + error.getMessage());
                Toast.makeText(getContext(), "Failed to load events.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up the listener to prevent memory leaks
        if (eventsRef != null && eventsListener != null) {
            eventsRef.removeEventListener(eventsListener);
        }
    }
}