// References:
// Google Maps Platform Documentation
// http://wptrafficanalyzer.in/blog/drawing-driving-route-directions-between-two-locations-using-google-directions-in-google-map-android-api-v2/
// https://github.com/googlesamples/android-BluetoothChat/blob/master/Application/src/main/java/com/example/android/bluetoothchat/BluetoothChatFragment.java
// https://gist.github.com/antoniomaria/598ea89d54accef1a1f5
// https://stackoverflow.com/questions/20339942/get-device-angle-by-using-getorientation-function
package com.example.naviglass2;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.pm.PackageManager;
import android.content.BroadcastReceiver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, SensorEventListener {

    private static final String TAG = "MapsActivity";
    private GoogleMap mMap;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 2;
    private static final int REQUEST_ENABLE_BT = 5;
    private boolean mLocationPermissionGranted;
    private boolean setOriginCalled = false;
    private boolean locationManagerCalled = false;
    private LocationManager locationManager;
    private LocationRequest mLocationRequest;
    private AutocompleteSupportFragment autocompleteFragment;
    private Marker origin = null;
    private Marker destination = null;
    private Polyline mPolyline = null;
    private Bluetooth mBluetoothConnection = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private ArrayList<BluetoothDevice> mDiscoveredDevices;
    private ArrayList<String> mDiscoveredDevicesString;
    private ListView listView;
    private SwipeRefreshLayout swipeRefresh;
    private Button button;
    private TextView textBox;
    private List<LatLng> directionPoints;
    private String directionsData;
    SendMessage sendMessage = new SendMessage();
    private SensorManager mSensorManager;
    private Sensor accelerometer, magnetometer;
    float[] mGravity;
    float[] mGeomagnetic;
    private ArrayList<bitmapPosition> bitmaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // if Bluetooth is not supported, quit app
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available in this " +
                    "device. Terminating application", Toast.LENGTH_LONG).show();
            this.finish();
        }

        // autocomplete fragment provides a list of suggested places as user types an address or a place name
        // hide autocomplete fragment in beginning, when Bluetooth device list is shown
        autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.getView().setVisibility(View.GONE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // executed when "Set Destination" button is clicked
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mPolyline != null) {
                    button.setVisibility(View.GONE); // hides button since no use for it
                    getDirections(); // sends directions to pi
                    directionPoints = new ArrayList<>();
                    directionPoints = mPolyline.getPoints(); // list of coordinates that make up the route
                    StringBuilder route = new StringBuilder("");
                    // if destination is too far, too much data might break app and pi
                    // should try to send data in patches
                    if (directionPoints.size() <= 2000) {
                        for (int i = 0; i < directionPoints.size(); i++) {
                            // convert coordinates into String so it can be converted to bytes
                            // to be sent to pi
                            String latitude = Double.toString(directionPoints.get(i).latitude);
                            String longitude = Double.toString(directionPoints.get(i).longitude);
                            String point = "(" + latitude + "," + longitude + "),";
                            route.append(point);
                        }

                        // send coordinates of route to pi, divided in chunks
                        try {
                            byte[] bytes = route.toString().getBytes();
                            byte[] range = null;
                            int blockSize = 900; // each chunk is 900 bytes
                            int blockCount = (bytes.length + blockSize - 1) / blockSize;
                            int i;
                            // for every chunk except the last chunk
                            for (i = 1; i < blockCount; i++) {
                                int idx = (i - 1) * blockSize;
                                range = Arrays.copyOfRange(bytes, idx, idx + blockSize);
                                ByteArrayOutputStream send = new ByteArrayOutputStream();
                                String start;
                                // label the start of chunk- needed by pi
                                if (i == 1)
                                    start = "[(route," + Integer.toString(i) + ",start)";
                                else
                                    start = "[(route," + Integer.toString(i) + ")";
                                // end label- needed by pi
                                String end = "(route," + Integer.toString(i) + ")],";
                                // append startlabel-data-endlabel to an empty chunk
                                send.write(start.getBytes());
                                send.write(range);
                                send.write(end.getBytes());
                                // send the chunk to pi via Bluetooth
                                sendMessage.run(send.toByteArray());
                            }
                            // Last chunk. similar procedure
                            int last = -1;
                            if (bytes.length % blockSize == 0)
                                last = bytes.length;
                            else
                                last = bytes.length % blockSize + blockSize * (blockCount - 1);
                            range = Arrays.copyOfRange(bytes, (blockCount - 1) * blockSize, last);
                            ByteArrayOutputStream send = new ByteArrayOutputStream();
                            String start;
                            if (i == 1)
                                start = "[(route," + Integer.toString(i) + ",start)";
                            else
                                start = "[(route," + Integer.toString(i) + ")";
                            String end = "(route," + Integer.toString(i) + ",end)]";
                            send.write(start.getBytes());
                            send.write(range);
                            send.write(end.getBytes());
                            sendMessage.run(send.toByteArray());
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        // called to send map image to pi
                        sendMapImage1();
                        //getIntersections();
                    }
                    // activate timer to send current location periodically
                    timerHandler.postDelayed(timerRunnable, 0);
                }
            }
        });

        // for retrieving azimuth
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // On startup, map is hidden while list of Bluetooth devices is displayed.
        // Map will be visible once user selects a Bluetooth device from the list
        mMap = googleMap;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Places.initialize(getApplicationContext(), getString(R.string.google_maps_key));

        getLocationPermission(); // check for and request location permission
        updateLocationUI(); // Turn on the My Location layer and the related control on the map
        if (!setOriginCalled && ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED)
            setOrigin(); // Get current location of device and set the position of the map

        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ID, Place.Field.NAME,Place.Field.LAT_LNG));
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            // when user selects a place from autocomplete fragment list, set selected place as destination
            public void onPlaceSelected(@NonNull Place place) {
                if (destination != null)
                    destination.remove(); // erase previous destination
                destination = mMap.addMarker(new MarkerOptions().position(place.getLatLng()).
                        title("Destination"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(place.getLatLng(), 16)); // move map to show destination
                executeDirections(); // shows route in red line
            }

            @Override
            public void onError(Status status) {
                Log.e(TAG, status.toString());
            }
        });

        // when user touches a place on the map, set the place as destination
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                if (destination != null)
                    destination.remove(); // erase previous destination
                destination = mMap.addMarker(new MarkerOptions().position(latLng).title("Destination"));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16)); // move map to show destination
                executeDirections(); // shows route in red line
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // onBtEnabled() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        // Otherwise, call onBtEnabled()
        else if (mBluetoothConnection == null)
            onBtEnabled();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mBluetoothConnection != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBluetoothConnection.getState() == Bluetooth.STATE_NONE) {
                // Start Bluetooth services
                mBluetoothConnection.start();
            }
        }

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);
        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        mBluetoothAdapter.startDiscovery();

        mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
        orientationHandler.postDelayed(orientationRunnable, 0);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // needed to retrieve azimut
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;
    }

    Handler orientationHandler = new Handler();
    // retrieves and sends current azimut to pi every 5 seconds using accelerometer and magnetometer
    Runnable orientationRunnable = new Runnable() {
        @Override
        public void run() {
            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    SensorManager.getOrientation(R, orientation);
                    float azimut = orientation[0] * 180 / 3.14159f;
                    // angle between the magnetic north direction
                    // 0=North, 90=East, 180=South, 270=West
                    String string = "azimut=[" + Float.toString(azimut) + "]";
                    sendMessage.run(string.getBytes());
                }
            }
            orientationHandler.postDelayed(this, 5000);
        }
    };

    static class bitmapPosition {
        Bitmap bitmap;
        int index;
        int x, y;
    }

    // getting images of map
    // IMAGE WILL BE CORRECTLY COMPLETED ONLY NEAR THE LATITUDE OF NEW JERSEY, USA
    // because distance corresponding to change of a degree of longitude varies throughout world
    private void sendMapImage1() {
        // Map images are retrieved one by one, each 640x640 pixels (max size
        // allowed by Google API), each containing a part of the route, then put in an arrayList, starting
        // from origin and following the route to destination. Will be tiled together to form one
        // complete image that contains entire route
        double latitude = origin.getPosition().latitude; // get latitude, longitude of origin
        double longitude = origin.getPosition().longitude;
        int index = 0; // order of this image
        bitmaps = new ArrayList<>(); // list of images
        bitmaps.add(null); // so this image can be set at this index in list of images later
        String origin = Double.toString(latitude) + "," +
                Double.toString(longitude); // center of image
        String firstImageURL = getImageURL(origin); // getting url to pass to Google Maps API
        DownloadMapImageTask firstTask = new DownloadMapImageTask(this);
        firstTask.execute(firstImageURL, index, 0, 0); // this downloads image from Google Maps API

        for (int i = 5; i < directionPoints.size(); i = i + 5) {
            if (directionPoints.get(i).latitude - latitude < -0.002614) {
                // will tile image down
                ++index; // order of this image in arrayList
                bitmaps.add(null);
                // in NJ image length (640 pixels) is approx. 0.005528 degrees latitude.
                // shift center down
                latitude = latitude - 0.005228;
                String center = Double.toString(latitude) + "," +
                        Double.toString(longitude); // center of image
                String imageURL = getImageURL(center); // URL to pass to Google API to retrieve this block of map image
                DownloadMapImageTask task = new DownloadMapImageTask(this);
                task.execute(imageURL, index, 0, 1); // 0, 1 -> will place this image below previous image
            }
            if (directionPoints.get(i).latitude - latitude > 0.002614) {
                // will tile image up
                ++index; // order of this image in arrayList
                bitmaps.add(null);
                // in NJ image length (640 pixels) is approx. 0.005528 degrees latitude
                // shifting center up
                latitude = latitude + 0.005228;
                String center = Double.toString(latitude) + "," +
                        Double.toString(longitude); // center of image
                String imageURL = getImageURL(center); // URL to pass to Google API to retrieve this block of map image
                DownloadMapImageTask task = new DownloadMapImageTask(this);
                task.execute(imageURL, index, 0, -1); // 0, -1 -> will place this image on top of previous image
            }
            if (directionPoints.get(i).longitude - longitude > 0.003432) {
                // will tile image right
                ++index; // order of this image in arrayList
                bitmaps.add(null);
                // in NJ image length (640 pixels) is approx. 0.006864 degrees longitude
                // shift center right
                longitude = longitude + 0.006864;
                String center = Double.toString(latitude) + "," +
                        Double.toString(longitude); // center of image
                String imageURL = getImageURL(center); // URL to pass to Google API to retrieve this block of map image
                DownloadMapImageTask task = new DownloadMapImageTask(this);
                task.execute(imageURL, index, 1, 0); // 1, 0 -> will place this image to the right of previous image
            }
            if (directionPoints.get(i).longitude - longitude < -0.003432) {
                // will tile image left
                ++index; // order of this image in arrayList
                bitmaps.add(null);
                longitude = longitude - 0.006864;
                String center = Double.toString(latitude) + "," +
                        Double.toString(longitude); // center of image
                String imageURL = getImageURL(center);
                DownloadMapImageTask task = new DownloadMapImageTask(this);
                task.execute(imageURL, index, -1, 0); // -1, 0 -> will place this image to the left of previous image
            }
        }

        // Wait until all tasks are finished (all images retrieved)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                sendMapImage2();
            }
        }, index * 500 + 2000); // each image seems to take approx. half second and first image takes long
    }

    // Returns URL to use to retrieve the particular image
    private String getImageURL(String center) {
        String url= "https://maps.googleapis.com/maps/api/staticmap?center=" + center +
                "&size=640x640&zoom=17"/*change zoom level here if you want*/ +
                "&format=jpg&key=" + getString(R.string.google_maps_key);
        return url;
    }

    // download map images from Google API
    private static class DownloadMapImageTask extends AsyncTask<Object, Void, bitmapPosition> {
        // this is required to make this class static
        private WeakReference<MapsActivity> activityReference;
        DownloadMapImageTask(MapsActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected bitmapPosition doInBackground(Object... params) {
            Bitmap bitmap = null;
            InputStream inputStream = null;
            HttpURLConnection urlConnection = null;
            // go to this url and download image
            try {
                URL url = new URL((String) params[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.connect();
                inputStream = (InputStream) url.getContent();
                bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                urlConnection.disconnect();
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            bitmapPosition bp = new bitmapPosition();
            bp.bitmap = bitmap;
            bp.index = (int) params[1];
            bp.x = (int) params[2];
            bp.y = (int) params[3];
            return bp;
        }

        @Override
        protected void onPostExecute(bitmapPosition bp) {
            MapsActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing())
                return;

            // set the image in its order (passed index) in the array
            if (activity.bitmaps.size()- 1 >= bp.index) {
                activity.bitmaps.set(bp.index, bp);
            }
            else
                Log.d("tag", "Something went wrong when tiling map images");
        }
    }

    // tiling the downloaded map images
    private void sendMapImage2() {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); // will be used to convert image to bytes
            int x = 0, y = 0, xmax = 0, xmin = 0, ymax = 0, ymin = 0;
            for (int i = 0; i < bitmaps.size(); i++) {
                if (bitmaps.get(i) == null) {
                    // some tasks have not finished downloading image.
                    // give the task time to finish and try again
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendMapImage2();
                        }
                    }, 2000);
                }
                // get width, height of completed image, and position of first image on completed image
                x = x + bitmaps.get(i).x;
                if (x > xmax) xmax = x;
                if (x < xmin) xmin = x;
                y = y + bitmaps.get(i).y;
                if (y > ymax) ymax = y;
                if (y < ymin) ymin = y;
            }
            int width = (xmax-xmin+1)* 640; // width and height of each images are 640 pixels
            int height = (ymax-ymin+1) * 640;

            // if image is bigger than maximum image dimension an Android can create, do not make image
            // need a workaround to this
            if (width > 4000 || height > 3000) {
                Toast.makeText(this, "Map image is too big for the phone. Please " +
                        "select a closer destination", Toast.LENGTH_LONG).show();
            }
            else {
                // get position of top left of completed image; needed by pi
                double topLatitude = origin.getPosition().latitude;
                double topLongitude = origin.getPosition().longitude;
                topLatitude = topLatitude - ymin*0.005228 + 0.002614;
                topLongitude = topLongitude + xmin*0.006864 - 0.003432;

                Bitmap combinedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                // tile map images
                Canvas combinedImage = new Canvas(combinedBitmap);
                x = -xmin * 640; // position of first image
                y = -ymin * 640;
                for (int i = 0; i < bitmaps.size(); i++) {
                    x = x + bitmaps.get(i).x * 640; // value of bitmap.x indicates left or right
                    y = y + bitmaps.get(i).y * 640; // value of bitmap.y indicates up or down
                    combinedImage.drawBitmap(bitmaps.get(i).bitmap, x, y, null);
                }

                combinedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, byteArrayOutputStream);
                // imageByByte = completed image in bytes, that contains entire route
                byte[] imageByByte = byteArrayOutputStream.toByteArray();

                // send completed image to pi in bytes, divided to chunks
                byte[] range = null;
                int blockSize = 900; // each chunk is 900 bytes
                int blockCount = (imageByByte.length + blockSize - 1) / blockSize;
                int i;
                for (i = 1; i < blockCount; i++) { // for every chunk except the last chunk
                    int idx = (i - 1) * blockSize;
                    range = Arrays.copyOfRange(imageByByte, idx, idx + blockSize);
                    ByteArrayOutputStream send = new ByteArrayOutputStream();
                    String start;
                    // label start of chunk; label is needed by pi
                    if (i == 1)
                        start = "[(image," + Integer.toString(i) + ",start," + Integer.toString(width) + "x" +
                                Integer.toString(height) + ",(" + Double.toString(topLatitude) + ","
                                + Double.toString(topLongitude) + "))";
                    else
                        start = "[(image," + Integer.toString(i) + ")";
                    String end = "(image," + Integer.toString(i) + ")],"; // label end of chunk; label is needed by pi
                    // append startlabel-data-endlabel to an empty bytearrayoutputstream
                    send.write(start.getBytes());
                    send.write(range);
                    send.write(end.getBytes());
                    sendMessage.run(send.toByteArray()); // send this chunk to pi via Bluetooth
                }
                // Last chunk, similar procedure
                int last = -1;
                if (imageByByte.length % blockSize == 0)
                    last = imageByByte.length;
                else
                    last = imageByByte.length % blockSize + blockSize * (blockCount - 1);
                range = Arrays.copyOfRange(imageByByte, (blockCount - 1) * blockSize, last);
                ByteArrayOutputStream send = new ByteArrayOutputStream();
                String start;
                if (i == 1)
                    start = "[(image," + Integer.toString(i) + ",start," + Integer.toString(width) +
                            "x" + Integer.toString(height) + ",(" + Double.toString(topLatitude) +
                            "," + Double.toString(topLongitude) + "))";
                else
                    start = "[(image," + Integer.toString(i) + ")";
                String end = "(image," + Integer.toString(i) + ",end)]";
                send.write(start.getBytes());
                send.write(range);
                send.write(end.getBytes());
                sendMessage.run(send.toByteArray());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    Handler timerHandler = new Handler();
    // send pi current location in bytes (latitude, longitude) every 30 seconds
    Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            // check if location permission granted every 30 seconds
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    // sends location to pi
                    String locationString = "location=[(" + Double.toString(location.getLatitude())
                            + "," + Double.toString(location.getLongitude()) + ")]";
                    sendMessage.run(locationString.getBytes());
                }
            }
            // if location permission denied, request it
            else if (mLocationPermissionGranted) {
                getLocationPermission();
            }
            timerHandler.postDelayed(this, 30000);
        }
    };

    // retrieve and send list of directions (e.g. turn right at College Ave) to pi in bytes
    private void getDirections() {
        if (directionsData != null) { //directionsData is directions retrieved earlier using Google Maps Directions API
            try {
                // putting together select relevent information: start location, end location, maneuver, driving instructions
                JSONObject jsonObject = new JSONObject(directionsData);
                JSONArray routes = jsonObject.getJSONArray("routes");
                for (int i = 0; i < routes.length(); i++) {
                    JSONObject onlyRoute = routes.getJSONObject(i);
                    StringBuilder directions = new StringBuilder("directions=[");
                    JSONArray legs = onlyRoute.getJSONArray("legs");
                    for (int j = 0; i < legs.length(); i++) {
                        JSONObject onlyleg = legs.getJSONObject(j);
                        JSONArray steps = onlyleg.getJSONArray("steps");

                        for (int k = 0; k < steps.length(); k++) {
                            JSONObject eachStep = steps.getJSONObject(k);
                            JSONObject start_location = eachStep.getJSONObject("start_location");
                            JSONObject end_location = eachStep.getJSONObject("end_location");
                            String string = "(("
                                    + Double.toString(start_location.optDouble("lat")) + ","
                                    + Double.toString(start_location.optDouble("lng")) + "),("
                                    + Double.toString(end_location.optDouble("lat")) + ","
                                    + Double.toString(end_location.optDouble("lng")) + "),"
                                    + eachStep.optString("maneuver") + ","
                                    + eachStep.optString("html_instructions").replaceAll

                                    ("<b>", "").replaceAll("</b>", "").
                                    replaceAll("<div style=\"font-size:0.9em\">", ". ").
                                    replaceAll("</div>", ". ").
                                    replaceAll("&nbsp;", "") + "),";
                            directions.append(string);
                        }
                    }
                    directions.append("]");

                    // send directions to pi in bytes, divided in chunks
                    try {
                        byte[] bytes = directions.toString().getBytes();
                        byte[] range = null;
                        int blockSize = 900; // each chunk is 900 bytes
                        int blockCount = (bytes.length + blockSize - 1) / blockSize;
                        int m;
                        for (m = 1; m < blockCount; m++) { // for every chunk except the last chunk
                            int idx = (m-1) * blockSize;
                            range = Arrays.copyOfRange(bytes, idx, idx + blockSize);
                            ByteArrayOutputStream send = new ByteArrayOutputStream();
                            String start;
                            // label start of chunk; label is needed by pi
                            if (m == 1)
                                start = "[(directions," + Integer.toString(m) + ",start)";
                            else
                                start = "[(directions," + Integer.toString(m) + ")";
                            // label end of chunk; label is needed by pi
                            String end = "(directions," + Integer.toString(m) + ")],";
                            // append startlabel-data-endlabel to an empty bytearrayoutputstream
                            send.write(start.getBytes());
                            send.write(range);
                            send.write(end.getBytes());
                            // send this chunk to pi via Bluetooth
                            sendMessage.run(send.toByteArray());
                        }
                        // Last chunk, similar procedure
                        int last = -1;
                        if (bytes.length % blockSize == 0)
                            last = bytes.length;
                        else
                            last = bytes.length % blockSize + blockSize * (blockCount - 1);
                        range = Arrays.copyOfRange(bytes, (blockCount - 1) * blockSize, last);
                        ByteArrayOutputStream send = new ByteArrayOutputStream();
                        String start;
                        if (m == 1)
                            start = "[(directions," + Integer.toString(m) + ",start)";
                        else
                            start = "[(directions," + Integer.toString(m) + ")";
                        String end = "(directions," + Integer.toString(m) + ",end)]";
                        send.write(start.getBytes());
                        send.write(range);
                        send.write(end.getBytes());
                        sendMessage.run(send.toByteArray());
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // retrieves list of intersections
    /*private void getIntersections() {
        ArrayList<String> urls = new ArrayList<>();
        String url1 = "http://api.geonames.org/findNearestIntersectionJSON?";
        for (int i = 0; i < directionPoints.size(); i++) {
            double lat = directionPoints.get(i).latitude;
            double lng = directionPoints.get(i).longitude;
            String url = url1 + "lat=" + Double.toString(lat) + "&lng=" + Double.toString(lng)
                    + "&username=yk9326";
            urls.add(url);
        }
        IntersectionsJSON intersectionsJSON = new IntersectionsJSON(this);
        intersectionsJSON.execute(urls);
    }

    private static class IntersectionsJSON extends AsyncTask<ArrayList<String>, Void, Void> {
        private WeakReference<MapsActivity> activityReference;
        IntersectionsJSON(MapsActivity context) {
            activityReference = new WeakReference<>(context);
        }
        StringBuilder intersections = new StringBuilder("intersections=[");
        HashSet<String> set = new HashSet<String>();

        @Override
        protected Void doInBackground(ArrayList<String>... arrayList) {
            ArrayList<String> urls = arrayList[0];
            for (int i = 0; i < urls.size(); i++) {
                String data = "";
                try {
                    data = downloadUrl(urls.get(i));
                    JSONObject jsonObject = new JSONObject(data);
                    JSONObject jsonObject2 = jsonObject.optJSONObject("intersection");
                    if (jsonObject2 != null) {
                        String latlng = jsonObject2.optString("lat") + "," +
                                jsonObject2.optString("lng");
                        if (!set.contains(latlng)) {
                            set.add(latlng);
                            String intersection = "(" + jsonObject2.optString("street1") + ","
                                    + jsonObject2.optString("street2") + ",(" +
                                    jsonObject2.optString("lat") + "," + jsonObject2.optString("lng")
                                    + ")),";
                            intersections.append(intersection);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.toString());
                }
            }
            intersections.append("]");
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            MapsActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing())
                return;
            activity.sendMessage.run(intersections.toString().getBytes()); //!--
        }
    }*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == REQUEST_ENABLE_BT) {
                // If Bluetooth was turned on, call onBtEnabled()
                if (resultCode == RESULT_OK) {
                    if (mBluetoothConnection == null)
                        onBtEnabled();
                }
                else {
                    // User denied Bluetooth. Shut down app
                    Toast.makeText(getApplicationContext(), "Bluetooth is disabled. Terminating " +
                            "application. Please turn on Bluetooth and try again", Toast.LENGTH_LONG).show();
                    this.finish();
                }

        }
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        // if device moves, move map to show current location and send current location to pi
        public void onLocationChanged(Location location) {
            try {
                if (ContextCompat.checkSelfPermission(getApplicationContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    // if origin was not set on app startup, set current location as origin
                    if (origin == null) {
                        origin = mMap.addMarker(new MarkerOptions().position(new LatLng
                                (location.getLatitude(), location.getLongitude())).title("Origin"));
                        autocompleteFragment.setLocationBias(RectangularBounds.newInstance(origin.getPosition(),
                                origin.getPosition()));
                    }
                    // move map to show current location
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), 16));
                    // send current location to pi
                    String locationString = "location=[(" + Double.toString(location.getLatitude())
                            + "," + Double.toString(location.getLongitude()) + ")]";
                    sendMessage.run(locationString.getBytes());
                }
                else {
                    Toast.makeText(getApplicationContext(), "Current location not found",
                            Toast.LENGTH_LONG).show();
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);
                }
            }
            catch(SecurityException e) {
                Log.e(TAG, e.getMessage());
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {}

        @Override
        public void onProviderEnabled(String provider) {
            if (!setOriginCalled)
                setOrigin();
        }

        @Override
        // If user disables GPS, show this message
        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "Location is disabled. App requires " +
                    "location access", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                mLocationPermissionGranted = false;
                // If request is cancelled, the result arrays are empty.
                // If location permission granted after prompt
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                    if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        // If not done already in app startup, register location listener function
                        // which takes action when device is moved
                        if (!locationManagerCalled) {
                            locationManagerCalled = true;
                            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1,
                                    0.5f, locationListener);
                        }
                    }
                }
                updateLocationUI();            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unregister the ACTION_FOUND receiver.
        try {
            unregisterReceiver(receiver);
            unregisterReceiver(btReceiverStateChanged);
            mSensorManager.unregisterListener(this);

        }
        catch (IllegalArgumentException e) {}
        if (mBluetoothConnection != null)
            mBluetoothConnection.stop();
    }

    // called on app startup if Bluetooth is enabled. prepares to show list of nearby Bluetooth devices
    private void onBtEnabled() {
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(btReceiverStateChanged, filter);
        mBluetoothConnection = new Bluetooth();
        swipeRefresh = findViewById(R.id.swipe_refresh);
        swipeRefresh.setBackgroundColor(Color.WHITE);
        swipeRefresh.setVisibility(View.VISIBLE);
        listView = findViewById(R.id.list_view);
        textBox = findViewById(R.id.text_box);
        mDiscoveredDevices = new ArrayList<>();
        mDiscoveredDevicesString = new ArrayList<>();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // display device if device is not Bluetooth LE, because our pi is not Bluetooth LE
                if (device.getType() != BluetoothDevice.DEVICE_TYPE_LE) {
                    ArrayAdapter<String> discoveredDevicesArrayAdapter = new ArrayAdapter<String>(context,
                            android.R.layout.simple_list_item_1, mDiscoveredDevicesString);
                    // display device only if device is not already listed
                    if (discoveredDevicesArrayAdapter.getPosition((device.getName() + "\n" + device.getAddress())) == -1) {
                        mDiscoveredDevices.add(device);
                        mDiscoveredDevicesString.add(device.getName() + "\n" + device.getAddress());
                    }
                    listView.setAdapter(discoveredDevicesArrayAdapter);
                }
            }
            // If a device is selected, show map
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View view, int pos, long id){
                    swipeRefresh.setVisibility(View.GONE);
                    textBox.setVisibility(View.GONE);
                    autocompleteFragment.getView().setVisibility(View.VISIBLE);
                    BluetoothDevice device = mDiscoveredDevices.get(pos);
                    mBluetoothConnection.connect(device);
                }
            });
            // When list is swipe down, refresh list of devices
            swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    mDiscoveredDevices.clear();
                    mDiscoveredDevicesString.clear();
                    if (mBluetoothAdapter.isDiscovering())
                        mBluetoothAdapter.cancelDiscovery();
                    mBluetoothAdapter.startDiscovery();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            swipeRefresh.setRefreshing(false);
                        }
                    }, 5000);
                }
            });
        }
    };

    // If Bluetooth is turned off, request for Bluetooth to be turned on
    private final BroadcastReceiver btReceiverStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    /*swipeRefresh.setVisibility(View.VISIBLE);
                    mDiscoveredDevices.clear();
                    mDiscoveredDevicesString.clear();
                    if (mBluetoothAdapter.isDiscovering())
                        mBluetoothAdapter.cancelDiscovery();
                    mBluetoothAdapter.startDiscovery();*/
                }
            }
        }
    };

    // sends bytes to pi
    class SendMessage {
        public synchronized void run(byte[] bytes) {
            // Check that we're actually connected before trying anything
            if (mBluetoothConnection.getState() != Bluetooth.STATE_CONNECTED)
                return;
            if (bytes.length > 0) { // Check that there's actually something to send
                    mBluetoothConnection.write(bytes);
            }
        }
    }

    // checks and asks for location permission
    private void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            if (!locationManagerCalled) {
            locationManagerCalled = true;
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1,
                    0.5f, locationListener);
            }
        }
        else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    // If user has granted location permission, turn on the My Location layer and the related
    // control on the map. Otherwise, disable the layer and the control
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            }
            else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                getLocationPermission();
            }
        }
        catch (SecurityException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    // On startup, retrieve current location of device and set the position of the map
    private void setOrigin() {
        setOriginCalled = true;
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                origin = mMap.addMarker(new MarkerOptions().position(new LatLng(
                        location.getLatitude(), location.getLongitude())).title("Origin"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()), 16));
                autocompleteFragment.setLocationBias(RectangularBounds.newInstance(origin.getPosition(),
                        origin.getPosition()));
            }
            else
                Toast.makeText(this, "Unable to retrieve last known location. Please " +
                        "move device", Toast.LENGTH_LONG).show();
        }
    }

    // Initiate download of directions json data using Google Maps Directions API
    private void executeDirections() {
        if (origin != null && destination != null) {
            if (mPolyline != null)
                mPolyline.remove();
            String url = getDirectionsUrl(origin.getPosition(), destination.getPosition());
            DownloadTask downloadTask = new DownloadTask(this);
            downloadTask.execute(url);
        }
    }

    // Returns URL to the Google Directions API
    private String getDirectionsUrl(LatLng origin, LatLng dest) {
        String output = "json";
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        //String sensor = "sensor=false";
        String mode = "mode=driving";
        String key = "key=" + getString(R.string.google_maps_key);
        String parameters = str_origin + "&" + str_dest + "&" + mode + "&" + key;
        return "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
    }

    // Fetches direction data from url passed
    private static class DownloadTask extends AsyncTask<String, Void, String> {
        // Using weak reference to make this class static and refer to main thread
        private WeakReference<MapsActivity> activityReference;
        DownloadTask(MapsActivity context) {
            activityReference = new WeakReference<>(context);
        }

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            return data; // data = data downloaded from web service
        }

        // Executes in UI thread, after execution of doInBackground()
        @Override
        protected void onPostExecute(String data) {
            MapsActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing())
                return;
            activity.directionsData = data;
            ParserTask parserTask = new ParserTask(activity);
            // Invokes the thread for parsing the JSON data
            parserTask.execute(data);
        }
    }

    // A method to download json data from url
    private static String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            // Create an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url
            urlConnection.connect();
            // Reading data from url
            iStream = urlConnection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            StringBuilder sb = new StringBuilder();
            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }
            data = sb.toString();
            br.close();
        }
        catch(Exception e) {
            Log.e(TAG, e.toString());
        }
        finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // A class to parse the Google Places in JSON format
    private static class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>>> {
        private WeakReference<MapsActivity> activityReference;
        ParserTask(MapsActivity context) {
            activityReference = new WeakReference<>(context);
        }
        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {
            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();
                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        // Executes in UI thread, after the parsing process
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            MapsActivity activity = activityReference.get();
            if (activity == null || activity.isFinishing())
                return;

            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            if (result != null) {
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList<LatLng>();
                    lineOptions = new PolylineOptions();

                    // Fetching i-th route
                    List<HashMap<String, String>> path = result.get(i);

                    // Fetching all the points in i-th route
                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);
                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);
                        points.add(position);
                    }
                    // Adding all the points in the route to LineOptions
                    lineOptions.addAll(points);
                    lineOptions.width(5);
                    lineOptions.color(Color.RED);
                }
                // Drawing polyline in the map for the i-th route
                activity.mPolyline = activity.mMap.addPolyline(lineOptions);
                activity.button.setVisibility(View.VISIBLE);
            }
            else {
                activity.mPolyline = null;
                Toast.makeText(activity, "Unable to retrieve direction", Toast.LENGTH_LONG).show();
            }
        }
    }

}