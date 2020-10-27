package com.richuncle.proyectoubicacion;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    ExecutorService executorService = Executors.newFixedThreadPool(1);

    double latitud, longitud;
    String data;

    private final int REQUEST_CHECK_CODE = 8989;
    private LocationSettingsRequest.Builder builder;

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    public TextView txtLat;
    public TextView txtLong;
    private TextView txtdata;
    private Button btnconectar;

    //BLE implementation
    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bluetoothLeScanner;
    BluetoothManager bluetoothManager;
    BluetoothScanCallback bluetoothScanCallback;
    BluetoothGatt gattClient;
    private GattClientCallback gattClientCallback;
    BluetoothGattCharacteristic characteristicID;
    private static final UUID BLE_UART_UUID = UUID.fromString("00008655-0000-1000-8000-00805F9B34FB");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("00002626-0000-1000-8000-00805F9B34FB");
    private static final UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txtLat = findViewById(R.id.latitud_text);
        txtLong = findViewById(R.id.longitud_text);
        txtdata = findViewById(R.id.txt_data);
        btnconectar = findViewById(R.id.btn_connect);

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
                    switch (e.getStatusCode()) {
                        case LocationSettingsStatusCodes
                                .RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                                resolvableApiException.startResolutionForResult(MainActivity.this, REQUEST_CHECK_CODE);
                            } catch (IntentSender.SendIntentException ex) {
                                ex.printStackTrace();
                            } catch (ClassCastException ex) {

                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE: {
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
        } else {
            setListener();
        }
        bluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        btnconectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScan();
            }
        });
    }

    public void setListener() {
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
            String time_Stamp = (System.currentTimeMillis() - System.currentTimeMillis() % 60000) + "";
            String Message = String.format("%.5f,%.5f,%s,%s", location.getLatitude(), location.getLongitude(), time_Stamp, data);
            //Send UDP Messages
            UDPSender client = new UDPSender(Message, 10840);
            executorService.submit(client);
            //Update on screen data
            txtdata.setText(String.format("%s", data));
            txtLat.setText(String.format("%.5f", latitud));
            txtLong.setText(String.format("%.5f", longitud));
        }
    }

    private void startScan() {
        bluetoothScanCallback = new BluetoothScanCallback();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        bluetoothLeScanner.startScan(bluetoothScanCallback);
    }

    private void connectDevice(BluetoothDevice device) {
        if (device == null) ;
        gattClientCallback = new GattClientCallback();
        gattClient = device.connectGatt(this, false, gattClientCallback);
    }

    private class BluetoothScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice().getName() != null) {
                if (result.getDevice().getName().equals("D_E_Connect")) {
                    // When find your device, connect.
                    connectDevice(result.getDevice());
                    bluetoothLeScanner.stopScan(bluetoothScanCallback); // stop scan
                    btnconectar.setVisibility(View.INVISIBLE);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
        }

        @Override
        public void onScanFailed(int errorCode) {
        }
    }

    private class GattClientCallback extends BluetoothGattCallback {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (status == BluetoothGatt.GATT_FAILURE) {
                return;
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                return;
            }

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status != BluetoothGatt.GATT_SUCCESS) return;
            // Reference your UUIDs
            characteristicID = gatt.getService(BLE_UART_UUID).getCharacteristic(CHARACTERISTIC_UUID);
            gatt.setCharacteristicNotification(characteristicID, true);
            BluetoothGattDescriptor descriptor = characteristicID.getDescriptor(DESCRIPTOR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            data = new String(characteristic.getValue());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            data = new String(characteristic.getValue());
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
        }
    }
}