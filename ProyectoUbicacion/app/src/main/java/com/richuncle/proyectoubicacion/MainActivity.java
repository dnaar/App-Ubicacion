package com.richuncle.proyectoubicacion;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    ExecutorService executorService = Executors.newFixedThreadPool(1);

    double latitud, longitud, lumx_val, accel_val;

    private final int REQUEST_CHECK_CODE = 8989;
    private LocationSettingsRequest.Builder builder;

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    public TextView txtLat;
    public TextView txtLong;
    public TextView txtlumx, txtaccel;
    private SensorManager sensorManager;
    Sensor lumx, accel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtLat = findViewById(R.id.latitud_text);
        txtLong = findViewById(R.id.longitud_text);
        txtlumx = findViewById(R.id.txt_lumx);
        txtaccel = findViewById(R.id.txt_accel);

        //Confirmación de localización activada-----------------------------------
        LocationRequest request = new LocationRequest().setFastestInterval(1500).setInterval(3000).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        builder = new LocationSettingsRequest.Builder().addLocationRequest(request);
        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(this).checkLocationSettings(builder.build());
        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    task.getResult(ApiException.class);
                } catch (ApiException e) {
                    switch(e.getStatusCode()){
                        case LocationSettingsStatusCodes
                                .RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(MainActivity.this, REQUEST_CHECK_CODE);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            }catch (ClassCastException ex){

                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        {
                            break;
                        }
                    }
                }
            }
        });
        //---------------------------------------------------------------------

        //Corroborar permisos de Ubicación.     //Pedirlos de ser necesario
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE_LOCATION_PERMISSION
            );
        }else{
            setListener();
        }
        // Manejo de sensores
        sensorManager  =(SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lumx = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if(lumx!=null){
            sensorManager.registerListener(MainActivity.this, lumx, SensorManager.SENSOR_DELAY_NORMAL);
        }else{
            txtlumx.setText("Sensor lumínico no disponible");
        }
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(accel != null){
            sensorManager.registerListener(MainActivity.this, accel, SensorManager.SENSOR_DELAY_NORMAL);
        }else{
            txtaccel.setText("Sensor acelerómetro no disponible");
        }
    }

    public void setListener(){
        final Button startbutton = findViewById(R.id.btn_start);
        startbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                _actualizarLocalizacion();
                startbutton.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void _actualizarLocalizacion() {
        LocationManager ubicacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ubicacion.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, new locationListener());
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setListener();
            } else {
                Toast.makeText(this, "Permisos de ubicación negados", Toast.LENGTH_SHORT).show();
            }
        }
    }

        private class locationListener implements LocationListener {
        @Override
        public void onLocationChanged(@NonNull Location location) {
            latitud = location.getLatitude();
            longitud = location.getLongitude();
            String time_Stamp = (System.currentTimeMillis()-System.currentTimeMillis()%60000) + "";
            String Message = String.format("%.5f,%.5f,%s,%.2f,%.2f", location.getLatitude(), location.getLongitude(), time_Stamp,lumx_val,accel_val);
            //Send UDP Messages
            UDPSender client = new UDPSender(Message, 10840);
            executorService.submit(client);
            //Update on screen data
            txtlumx.setText(String.format("%.2f", lumx_val));
            txtaccel.setText(String.format("%.2f", accel_val));
            txtLat.setText(String.format("%.5f",latitud));
            txtLong.setText(String.format("%.5f",longitud));
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        if(sensor.getType() == Sensor.TYPE_LIGHT){
            lumx_val = sensorEvent.values[0];
        }else if (sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            accel_val = sensorEvent.values[1];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}