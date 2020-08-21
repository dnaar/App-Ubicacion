package com.richuncle.tioricov4;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.EditText;
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

public class MainActivity extends AppCompatActivity {

    double latitud, longitud;

    private final int REQUEST_CHECK_CODE = 8989;
    private LocationSettingsRequest.Builder builder;

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    private TextView txtLat;
    private TextView txtLong;
    EditText txt_Phone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLat = findViewById(R.id.latitud_text);
        txtLong = findViewById(R.id.longitud_text);
        txt_Phone = findViewById(R.id.phone_text);

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
            _actualizarLocalizacion();
        }
        findViewById(R.id.change_location_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendMessage();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                _actualizarLocalizacion();
            } else {
                Toast.makeText(this, "Permisos de ubicación negados", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == 0 && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                SendMessage();
            } else {
                Toast.makeText(this, "Permisos de mensajería negados", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void _actualizarLocalizacion() {
        LocationManager ubicacion = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ubicacion.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new locationListener());
    }

    private class locationListener implements LocationListener {

        @Override
        public void onLocationChanged(@NonNull Location location) {
            latitud = location.getLatitude();
            longitud = location.getLongitude();
            txtLat.setText(String.format("%.7f",latitud));
            txtLong.setText(String.format("%.7f",longitud));
        }

    }

    private void SendMessage(){
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS);
        if (permissionCheck==PackageManager.PERMISSION_GRANTED) {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("hh:mm:ss a dd-MMM-yyyy");
            String time_Stamp = simpleDateFormat.format(calendar.getTime());
            String phoneNumber = txt_Phone.getText().toString().trim();
            String Message = String.format("Latitud: %s\nLongitud: %s\nFecha: %s", latitud, longitud, time_Stamp);

            if(!txt_Phone.getText().toString().equals("") && txt_Phone.getText().toString().length() > 9){
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, Message, null, null);
                Toast.makeText(this, "Mensaje Enviado", Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(this,"Ingresar un número de telefono válido", Toast.LENGTH_SHORT).show();
            }
        }else{
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 0);
        }
    }

}