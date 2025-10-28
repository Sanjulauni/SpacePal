package com.example.sp_v1.ui;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;

import com.example.sp_v1.R;
import com.example.sp_v1.models.ParkingSpot;
import com.example.sp_v1.utils.DatabaseHelper;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

import de.hdodenhof.circleimageview.CircleImageView;

public class MapFragment extends Fragment implements OnMapReadyCallback {

    private FirebaseAuth mAuth;
    private GoogleMap mMap;
    private DatabaseHelper dbHelper;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private MaterialCardView spotDetailsCard, searchCard;
    private TextView spotNameText, spotAvailabilityText, spotDistanceText;
    private View spotStatusIndicator;
    private MaterialButton reserveButton;
    private FloatingActionButton recenterFab;
    private CircleImageView profileImageMap;
    private TextView searchText;

    private Polyline currentRoute;
    private Marker carMarker;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean isNavigating = false;
    private boolean initialZoomDone = false;

    private String spotIdToFocus;
    private String filterByManagerId;


    private final ActivityResultLauncher<Intent> startAutocomplete = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Place place = Autocomplete.getPlaceFromIntent(result.getData());
                    Log.i("MapFragment", "Place selected: " + place.getName() + ", " + place.getLatLng());
                    if (place.getLatLng() != null && mMap != null) {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 15f));
                        searchText.setText(place.getName());
                        initialZoomDone = true;
                    }
                } else if (result.getResultCode() == AutocompleteActivity.RESULT_ERROR) {
                    Status status = Autocomplete.getStatusFromIntent(result.getData());
                    Log.e("MapFragment", "Autocomplete Error: " + status.getStatusMessage());
                    if(getContext() != null) Toast.makeText(getContext(), "Error searching", Toast.LENGTH_SHORT).show();
                } else if (result.getResultCode() == AutocompleteActivity.RESULT_CANCELED) {
                    Log.i("MapFragment", "User canceled autocomplete");
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        initialZoomDone = false;

        if (getArguments() != null) {
            spotIdToFocus = getArguments().getString("SPOT_ID_TO_FOCUS");
            filterByManagerId = getArguments().getString("FILTER_BY_MANAGER_ID");
        }

        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        dbHelper = new DatabaseHelper();
        if (getContext() != null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());
        }

        setupLocationTracking();

        spotDetailsCard = view.findViewById(R.id.spot_details_card);
        spotNameText = view.findViewById(R.id.spot_name_text);
        spotAvailabilityText = view.findViewById(R.id.spot_availability_text);
        spotDistanceText = view.findViewById(R.id.spot_distance_text);
        spotStatusIndicator = view.findViewById(R.id.spot_status_indicator);
        reserveButton = view.findViewById(R.id.reserve_button);
        recenterFab = view.findViewById(R.id.recenter_fab);
        profileImageMap = view.findViewById(R.id.profile_image_map);
        searchText = view.findViewById(R.id.search_text);
        searchCard = view.findViewById(R.id.search_card);

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if (filterByManagerId != null) {
            searchCard.setVisibility(View.GONE);
            profileImageMap.setVisibility(View.GONE);
        }

        recenterFab.setOnClickListener(v -> zoomToUserLocation());

        profileImageMap.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToAccount();
                BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation_view);
                if (bottomNav != null) {
                    bottomNav.getMenu().findItem(R.id.nav_account).setChecked(true);
                }
            }
        });

        searchCard.setOnClickListener(v -> launchAutocomplete());
        searchText.setOnClickListener(v -> launchAutocomplete());
    }


    private void launchAutocomplete() {
        if (!Places.isInitialized() || getContext() == null) {
            Log.e("MapFragment", "Places SDK not initialized or context null.");
            if(getContext() != null) Toast.makeText(getContext(), "Search unavailable.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(requireContext());
        startAutocomplete.launch(intent);
    }

    private void setupLocationTracking() {
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(3000)
                .setFastestInterval(1500);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (!isNavigating || mMap == null || carMarker == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    LatLng currentPos = new LatLng(location.getLatitude(), location.getLongitude());
                    carMarker.setPosition(currentPos);
                    if (location.hasBearing()) {
                        float bearing = location.getBearing();
                        carMarker.setRotation(bearing);
                        CameraPosition cameraPosition = new CameraPosition.Builder().target(currentPos).zoom(18f).bearing(bearing).tilt(45).build();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 750, null);
                    } else {
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPos, 18f), 750, null);
                    }
                }
            }
        };
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        Log.d("MapFragment", "Map Ready.");
        fetchAndDisplayParkingSpots();
        setupMyLocationLayer();
        if (mMap != null) mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.setOnMarkerClickListener(marker -> {
            if (marker.getTag() instanceof ParkingSpot) {
                showSpotDetails((ParkingSpot) marker.getTag());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), 16f), 500, null);
            }
            return true;
        });
        mMap.setOnMapClickListener(latLng -> hideSpotDetails());
    }

    private void setupMyLocationLayer() {
        if (getContext() == null || mMap == null) { Log.w("MapFragment", "setupMyLocationLayer: Context or Map null."); return; }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                Log.d("MapFragment", "Setting MyLocationEnabled true.");
                mMap.setMyLocationEnabled(true);
                if (!initialZoomDone && filterByManagerId == null) {
                    zoomToUserLocation();
                    initialZoomDone = true;
                }
            } catch (SecurityException e) { Log.e("MapFragment", "Map error.", e); }
        } else {
            Log.d("MapFragment", "Requesting location permission.");
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }


    private void zoomToUserLocation() {
        if (getContext() == null || mMap == null || fusedLocationClient == null) { Log.w("MapFragment", "zoomToUserLocation: Context/Map/Client null."); return; }
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ) { Log.w("MapFragment", "zoomToUserLocation: No permission."); return; }
        try {
            Log.d("MapFragment", "Requesting last location for zoom.");
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null && mMap != null) { Log.d("MapFragment", "Zooming to user: " + location.getLatitude() + "," + location.getLongitude()); mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15f)); }
                else { Log.w("MapFragment", "Could not get last location."); }
            }).addOnFailureListener(e -> Log.e("MapFragment", "Error getting last location", e));
        } catch (SecurityException e) { Log.e("MapFragment", "Permission missing", e); }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("MapFragment", "onRequestPermissionsResult: " + requestCode);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("MapFragment", "Permission granted.");
                setupMyLocationLayer();
            } else {
                Log.w("MapFragment", "Permission denied.");
                if (getContext() != null) Toast.makeText(getContext(), "Location permission is required to show your position.", Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        Log.d("MapFragment", "onResume. Navigating=" + isNavigating);
        if (isNavigating) {
            startLocationUpdates();
        }
    }

    private void fetchAndDisplayParkingSpots() {
        Log.d("MapFragment", "Starting fetchAndDisplayParkingSpots...");
        if (mMap == null || getContext() == null) return;

        mMap.clear();
        hideSpotDetails();

        DatabaseReference spotsRef = FirebaseDatabase.getInstance().getReference("parking_spots");
        Query spotsQuery;

        if (filterByManagerId != null && !filterByManagerId.isEmpty()) {
            Log.d("MapFragment", "Filtering by Manager ID: " + filterByManagerId);
            spotsQuery = spotsRef.orderByChild("managerId").equalTo(filterByManagerId);
            Toast.makeText(getContext(), "Showing event parking spots", Toast.LENGTH_SHORT).show();
        } else {
            Log.d("MapFragment", "Showing all verified spots.");
            spotsQuery = spotsRef.orderByChild("spotVerificationStatus")
                    .equalTo(ParkingSpot.STATUS_SPOT_TITLE_VERIFIED);
        }

        spotsQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mMap == null || getContext() == null) { Log.w("MapFragment", "onDataChange: Map or Context null."); return; }
                Log.d("MapFragment", "onDataChange. Snapshot exists: " + snapshot.exists());

                Marker markerToFocus = null;
                int markersAdded = 0;
                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();

                for (DataSnapshot spotSnapshot : snapshot.getChildren()) {
                    ParkingSpot spot = null;
                    try { spot = spotSnapshot.getValue(ParkingSpot.class); }
                    catch (Exception e) { Log.e("MapFragment", "Error parsing spot: " + spotSnapshot.getKey(), e); continue; }

                    if (spot == null) continue;

                    if (filterByManagerId != null) {
                        if (!spot.getSpotVerificationStatus().equals(ParkingSpot.STATUS_SPOT_TITLE_VERIFIED)) {
                            Log.w("MapFragment", "Skipping event spot, not verified: " + spot.getName());
                            continue;
                        }
                    }

                    // --- THIS IS THE UPDATED CHECK ---
                    if (spot.isListed() && spot.getName() != null && spotSnapshot.getKey() != null && spot.getLatitude() != 0.0 && spot.getLongitude() != 0.0) {
                        spot.setId(spotSnapshot.getKey());
                        LatLng spotLocation = new LatLng(spot.getLatitude(), spot.getLongitude());

                        if (filterByManagerId != null) {
                            boundsBuilder.include(spotLocation);
                        }

                        BitmapDescriptor markerIcon = getMarkerIcon(spot.getAvailable());
                        Marker marker = mMap.addMarker(new MarkerOptions().position(spotLocation).title(spot.getName()).icon(markerIcon).anchor(0.5f, 0.5f));

                        if (marker != null) {
                            marker.setTag(spot);
                            markersAdded++;
                        }

                        if (spot.getId() != null && spot.getId().equals(spotIdToFocus) && marker != null) {
                            markerToFocus = marker;
                        }
                    } else if (!spot.isListed()) {
                        Log.d("MapFragment", "Skipping spot (not listed): " + spot.getName());
                    } else {
                        Log.w("MapFragment", "Skipping spot - missing data: Key=" + spotSnapshot.getKey());
                    }
                    // --- END OF UPDATED CHECK ---
                }

                Log.d("MapFragment", "Finished processing. Markers added: " + markersAdded);

                if (filterByManagerId != null && markersAdded > 0) {
                    try {
                        LatLngBounds bounds = boundsBuilder.build();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                        initialZoomDone = true;
                    } catch (IllegalStateException e) {
                        Log.e("MapFragment", "Cannot zoom to bounds, map not ready or no markers.", e);
                        if (!initialZoomDone) {
                            zoomToUserLocation();
                            initialZoomDone = true;
                        }
                    }
                } else if (markerToFocus != null) {
                    ParkingSpot spot = (ParkingSpot) markerToFocus.getTag();
                    if (spot != null) {
                        Log.d("MapFragment", "Animating to focus spot: " + spot.getName());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markerToFocus.getPosition(), 16f), 1000, null);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> showSpotDetails(spot), 1100);
                    }
                    spotIdToFocus = null;
                }

                if (markersAdded == 0) {
                    if (filterByManagerId != null) {
                        Toast.makeText(getContext(), "No verified spots found for this event.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "No available spots found.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("MapFragment", "Firebase spots fetch failed: " + error.getMessage(), error.toException());
                if (getContext() != null) Toast.makeText(getContext(), "Error loading spots.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private BitmapDescriptor getMarkerIcon(boolean isAvailable) {
        if (getContext() == null) {
            Log.w("MapFragment", "getContext() is null in getMarkerIcon, returning default marker.");
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        }

        Drawable drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_local_parking);

        if (drawable == null) {
            Log.w("MapFragment", "ic_local_parking drawable not found, returning default marker.");
            return BitmapDescriptorFactory.defaultMarker(isAvailable ? BitmapDescriptorFactory.HUE_AZURE : BitmapDescriptorFactory.HUE_RED);
        }

        Drawable wrappedDrawable = DrawableCompat.wrap(drawable).mutate();
        int color = ContextCompat.getColor(requireContext(), isAvailable ? R.color.bright_blue : R.color.bright_red);
        DrawableCompat.setTint(wrappedDrawable, color);

        int width = 64;
        int height = 64;

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        wrappedDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        wrappedDrawable.draw(canvas);

        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void showSpotDetails(ParkingSpot spot) {
        if (getContext() == null) return;
        spotNameText.setText(spot.getName());
        String availability = spot.getAvailableSpaces() + (spot.getAvailableSpaces() == 1 ? " Space Available" : " Spaces Available");
        spotAvailabilityText.setText(availability);
        spotAvailabilityText.setTextColor(ContextCompat.getColor(requireContext(),
                spot.getAvailable() ? R.color.bright_blue : R.color.bright_red));

        Drawable background = spotStatusIndicator.getBackground();
        if (background instanceof GradientDrawable) ((GradientDrawable) background.mutate()).setColor(ContextCompat.getColor(requireContext(), spot.getAvailable() ? R.color.bright_blue : R.color.bright_red));
        else spotStatusIndicator.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), spot.getAvailable() ? R.color.bright_blue : R.color.bright_red));

        if (spotDistanceText != null) { spotDistanceText.setText("Calculating..."); calculateAndSetDistance(spot); }

        // --- THIS BLOCK IS UPDATED ---
        // It now shows "Reserve" and calls the reservation logic
        if (spot.getAvailable()) {
            reserveButton.setVisibility(View.VISIBLE);
            reserveButton.setText("Reserve"); // --- CHANGED ---
            reserveButton.setOnClickListener(v -> handleReservationClick(spot)); // --- CHANGED ---
        } else {
            reserveButton.setVisibility(View.GONE);
        }
        // --- END OF UPDATED BLOCK ---

        spotDetailsCard.setVisibility(View.VISIBLE);
        spotDetailsCard.setTranslationY(spotDetailsCard.getHeight());
        spotDetailsCard.animate().translationY(0).alpha(1.0f).setDuration(300).start();
    }

    private void calculateAndSetDistance(ParkingSpot spot) {
        if (getContext() == null || spotDistanceText == null) return;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { spotDistanceText.setText("Location needed"); return; }
        try { fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> { if (location != null && spotDistanceText != null) { Location spotLoc = new Location(""); spotLoc.setLatitude(spot.getLatitude()); spotLoc.setLongitude(spot.getLongitude()); float dist = location.distanceTo(spotLoc); String distTxt = dist < 1000 ? String.format(Locale.getDefault(), "%.0f m away", dist) : String.format(Locale.getDefault(), "%.1f km away", dist / 1000.0); spotDistanceText.setText(distTxt); } else if (spotDistanceText != null){ spotDistanceText.setText("Distance unknown"); }}).addOnFailureListener(e -> { if (spotDistanceText != null) spotDistanceText.setText("Distance error"); });
        } catch (SecurityException e) { spotDistanceText.setText("Permission needed"); }
    }
    private void hideSpotDetails() { if (spotDetailsCard != null && spotDetailsCard.getVisibility() == View.VISIBLE) { spotDetailsCard.animate().translationY(spotDetailsCard.getHeight()).alpha(0.0f).setDuration(200).withEndAction(() -> { if(spotDetailsCard != null) spotDetailsCard.setVisibility(View.INVISIBLE); }).start(); } }

    // --- This method is now used by the button again ---
    private void handleReservationClick(ParkingSpot spot) {
        if (getContext() == null) return;
        String userId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (userId == null) { Toast.makeText(getContext(), "Login required.", Toast.LENGTH_SHORT).show(); return; }
        dbHelper.checkForActiveReservation(userId, hasActive -> { if (getContext() == null) return; if (hasActive) { new AlertDialog.Builder(requireContext()).setTitle("Active Reservation").setMessage("View your active reservation?").setPositiveButton("View", (d, w) -> { if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateToReservations(); }).setNegativeButton("Cancel", null).show(); } else { new AlertDialog.Builder(requireContext()).setTitle("Confirm Reservation").setMessage("Reserve at " + spot.getName() + "?").setPositiveButton("Confirm", (d, w) -> proceedWithReservation(spot, userId)).setNegativeButton("Cancel", null).show(); } });
    }

    private void proceedWithReservation(ParkingSpot spot, String userId) {
        if (getContext() == null) return;
        dbHelper.createReservation(spot, userId, task -> { if (getContext() == null) return; if (task != null && task.isSuccessful()) { Toast.makeText(getContext(), "Reservation successful!", Toast.LENGTH_SHORT).show(); hideSpotDetails(); if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).navigateToReservations(); } else { String errorMsg = "Reservation failed."; if (task != null && task.getException() != null) errorMsg += " " + task.getException().getMessage(); Toast.makeText(getContext(), errorMsg, Toast.LENGTH_LONG).show(); fetchAndDisplayParkingSpots(); } });
    }

    private void startNavigation(ParkingSpot spot) {
        if (mMap == null || getContext() == null) return;
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { Toast.makeText(getContext(), "Location permission needed.", Toast.LENGTH_SHORT).show(); requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE); return; }
        try { fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> { if (location != null) fetchDirections(new LatLng(location.getLatitude(), location.getLongitude()), new LatLng(spot.getLatitude(), spot.getLongitude())); else { if (getContext() != null) Toast.makeText(getContext(), "Current location unavailable.", Toast.LENGTH_SHORT).show(); } }).addOnFailureListener(e -> { if (getContext() != null) Toast.makeText(getContext(), "Error getting location.", Toast.LENGTH_SHORT).show(); });
        } catch (SecurityException e) { Log.e("MapFragment", "Permission missing", e); }
    }

    private void fetchDirections(LatLng start, LatLng end) {
        if (getContext() == null) return;
        String apiKey = "";
        try {
            apiKey = getString(R.string.directions_api_key);
            if (apiKey == null || apiKey.isEmpty() || apiKey.startsWith("YOUR_")) {
                Log.e("MapFragment", "Directions API key missing or not set in strings.xml.");
                Toast.makeText(getContext(), "Directions API key error.", Toast.LENGTH_LONG).show();
                return;
            }
        } catch (Exception e) {
            Log.e("MapFragment", "Failed to load Directions API key from strings.xml", e);
            Toast.makeText(getContext(), "API key error.", Toast.LENGTH_SHORT).show();
            return;
        }

        String urlString = "https://maps.googleapis.com/maps/api/directions/json?origin=" + start.latitude + "," + start.longitude + "&destination=" + end.latitude + "," + end.longitude + "&key=" + apiKey;

        Executors.newSingleThreadExecutor().execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(urlString);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.connect();
                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    String response = sb.toString();
                    if (getActivity() != null) getActivity().runOnUiThread(() -> onDirectionsResponse(response, start, end));
                } else {
                    String errorMsg = "Directions API error: " + responseCode;
                    Log.e("MapFragment", errorMsg);
                    if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error fetching directions: " + responseCode, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                Log.e("MapFragment", "Network error", e);
                if (getActivity() != null) getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Network error.", Toast.LENGTH_SHORT).show());
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }

    private void onDirectionsResponse(String response, LatLng start, LatLng end) {
        if (getContext() == null) return; try { JSONObject json = new JSONObject(response); String status = json.getString("status"); if (!"OK".equals(status)) { Log.e("MapFragment", "Directions API status: " + status); Toast.makeText(getContext(), "Could not find directions: " + status, Toast.LENGTH_LONG).show(); return; } JSONArray routes = json.getJSONArray("routes"); if (routes.length() > 0) { JSONObject route = routes.getJSONObject(0); JSONObject overviewPolyline = route.getJSONObject("overview_polyline"); String encodedPath = overviewPolyline.getString("points"); List<LatLng> path = decodePolyline(encodedPath); if (!path.isEmpty()) drawRouteAndAnimate(path, start, end); else { Log.w("MapFragment", "Decoded path empty."); Toast.makeText(getContext(), "Route path invalid.", Toast.LENGTH_SHORT).show(); } } else { Toast.makeText(getContext(), "No routes found.", Toast.LENGTH_SHORT).show(); } } catch (Exception e) { Log.e("MapFragment", "Error parsing directions", e); Toast.makeText(getContext(), "Error parsing directions.", Toast.LENGTH_SHORT).show(); }
    }
    private void drawRouteAndAnimate(List<LatLng> path, LatLng start, LatLng end) {
        if (mMap == null || getContext() == null) return; if (currentRoute != null) currentRoute.remove(); if (carMarker != null) carMarker.remove(); currentRoute = mMap.addPolyline(new PolylineOptions().addAll(path).color(ContextCompat.getColor(requireContext(), R.color.primary_blue)).width(18).geodesic(true).clickable(false)); LatLngBounds.Builder builder = new LatLngBounds.Builder(); builder.include(start); for (LatLng p : path) builder.include(p); builder.include(end); try { mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150)); } catch (IllegalStateException e) { Log.e("MapFragment", "Map size error", e); mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(start, 16f)); } new Handler(Looper.getMainLooper()).postDelayed(() -> startRealTimeNavigation(start), 1200);
    }

    private void startRealTimeNavigation(LatLng startPos) {
        if (mMap == null || getContext() == null) return;
        try {
            mMap.setMyLocationEnabled(false);
        } catch (SecurityException e) { Log.e("MapFragment", "Could not disable MyLocation", e); }

        if (carMarker != null) carMarker.remove();

        try {
            Drawable carDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_car_marker);
            BitmapDescriptor carIcon = null;

            if (carDrawable != null) {
                Drawable wrappedDrawable = DrawableCompat.wrap(carDrawable).mutate();
                int yellowColor = ContextCompat.getColor(requireContext(), R.color.bright_yellow);
                DrawableCompat.setTint(wrappedDrawable, yellowColor);

                int width = 64;
                int height = 64;

                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                wrappedDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                wrappedDrawable.draw(canvas);
                carIcon = BitmapDescriptorFactory.fromBitmap(bitmap);
            } else {
                Log.w("MapFragment", "ic_car_marker drawable not found, using default.");
                carIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
            }

            carMarker = mMap.addMarker(new MarkerOptions()
                    .position(startPos)
                    .flat(true)
                    .anchor(0.5f, 0.5f)
                    .icon(carIcon)
                    .zIndex(1.0f));

            if (carMarker != null) {
                isNavigating = true;
                startLocationUpdates();
                Toast.makeText(getContext(), "Navigation started", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("MapFragment", "Failed to add car marker.");
                Toast.makeText(getContext(), "Error: Could not display vehicle.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("MapFragment", "Error setting up real-time navigation", e);
            Toast.makeText(getContext(), "Error starting navigation.", Toast.LENGTH_SHORT).show();
            try { if(mMap != null) mMap.setMyLocationEnabled(true); }
            catch (SecurityException se) { Log.e("MapFragment", "Could not re-enable MyLocation", se); }
        }
    }

    private void startLocationUpdates() {
        if (getContext() == null || fusedLocationClient == null || locationCallback == null) { Log.w("MapFragment", "Cannot start location updates: null object."); return; } if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) { try { fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper()); Log.i("MapFragment", "Location updates requested."); } catch (SecurityException e) { Log.e("MapFragment", "SecurityException on location updates.", e); } } else { Log.w("MapFragment", "No permission for location updates."); }
    }
    private void stopLocationUpdates() {
        Log.i("MapFragment", "Stopping location updates."); try { if (fusedLocationClient != null && locationCallback != null) fusedLocationClient.removeLocationUpdates(locationCallback); } catch (Exception e) { Log.e("MapFragment", "Error removing location updates", e); } isNavigating = false; if (mMap != null && getContext() != null && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) { try { mMap.setMyLocationEnabled(true); } catch (SecurityException e) { Log.e("MapFragment", "SecurityException re-enabling MyLocation.", e); } }
    }
    private List<LatLng> decodePolyline(String encoded) {
        List<LatLng> poly = new ArrayList<>(); int index = 0, len = encoded.length(); int lat = 0, lng = 0; while (index < len) { int b, shift = 0, result = 0; try { do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20); int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1)); lat += dlat; shift = 0; result = 0; do { b = encoded.charAt(index++) - 63; result |= (b & 0x1f) << shift; shift += 5; } while (b >= 0x20); int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1)); lng += dlng; double latitude = (double) lat / 1E5; double longitude = (double) lng / 1E5; if (latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) poly.add(new LatLng(latitude, longitude)); else Log.w("MapFragment", "Decoded invalid coords: " + latitude + "," + longitude); } catch (StringIndexOutOfBoundsException e) { Log.e("MapFragment", "Decode polyline error: Index out of bounds.", e); break; } catch (Exception e) { Log.e("MapFragment", "Decode polyline error", e); break; } } return poly;
    }
    @Override public void onPause() { super.onPause(); Log.d("MapFragment", "onPause."); if (isNavigating) stopLocationUpdates(); }
    @Override public void onDestroyView() {
        super.onDestroyView();
        Log.d("MapFragment", "onDestroyView.");
        stopLocationUpdates();
        mMap = null;
        fusedLocationClient = null;
        locationCallback = null;
        filterByManagerId = null;
        spotIdToFocus = null;
    }

} // End Fragment