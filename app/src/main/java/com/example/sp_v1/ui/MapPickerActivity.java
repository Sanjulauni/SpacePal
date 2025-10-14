package com.example.sp_v1.ui;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sp_v1.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView addressTextView;
    private LatLng selectedLatLng;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_picker);

        addressTextView = findViewById(R.id.address_text_view);
        MaterialButton selectLocationButton = findViewById(R.id.select_location_button);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        selectLocationButton.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("picked_latitude", selectedLatLng.latitude);
                resultIntent.putExtra("picked_longitude", selectedLatLng.longitude);
                setResult(RESULT_OK, resultIntent);
                finish();
            } else {
                Toast.makeText(this, "Location not selected.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        LatLng colombo = new LatLng(6.9271, 79.8612);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(colombo, 15f));
        selectedLatLng = colombo;
        getAddressFromLatLng(colombo);

        mMap.setOnCameraIdleListener(() -> {
            selectedLatLng = mMap.getCameraPosition().target;
            getAddressFromLatLng(selectedLatLng);
        });
    }

    private void getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String address = addresses.get(0).getAddressLine(0);
                addressTextView.setText(address);
            } else {
                addressTextView.setText("Address not found");
            }
        } catch (IOException e) {
            addressTextView.setText("Could not fetch address");
        }
    }
}