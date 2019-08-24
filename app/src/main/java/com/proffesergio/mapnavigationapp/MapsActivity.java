package com.proffesergio.mapnavigationapp;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;
import com.proffesergio.mapnavigationapp.models.Result;
import com.proffesergio.mapnavigationapp.models.Route;
import com.proffesergio.mapnavigationapp.models.events.BeginJourneyEvent;
import com.proffesergio.mapnavigationapp.models.events.CurrentJourneyEvent;
import com.proffesergio.mapnavigationapp.models.events.EndJourneyEvent;
import com.proffesergio.mapnavigationapp.utilities.JourneyEventBus;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
//import com.proffesergio.mapnavigationapp.utilities.LocationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.google.android.gms.maps.model.JointType.ROUND;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = MapsActivity.class.getSimpleName();
    private SupportMapFragment mapFragment;
    private GoogleMap mMap;
    private List<LatLng> polyLineList;
    private Marker marker;
    private float v;
    private double lat, lng;
    private Handler handler;
    private LatLng startPosition, endPosition;
    private int index, next;
    private LatLng home;
    private Button button;
    private EditText destinationEditText;
    private String destination;
    private LinearLayout linearLayout;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private Polyline blackPolyline, greyPolyLine;
    private ApiInterface apiInterface;
    private Disposable disposable;

    /**Location Util (Commented) **/
//    private LocationUtil locationUtilObj;
    private final int REQUEST_CODE_PERMISSION_MULTIPLE = 123;
    private boolean isDeninedRTPs = true;       // initially true to prevent anim(2)
    private boolean showRationaleRTPs = false;

    FusedLocationProviderClient mFusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

//        checkAndRequestRunTimePermissions();
//        getCurrentLocation();


            /**Location Util (Commented) **/
//        locationUtilObj = new LocationUtil(this, (LocationUtil.GetLocationListener) this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        linearLayout = findViewById(R.id.linearLayout);
        polyLineList = new ArrayList<>();

        button = findViewById(R.id.destination_button);
        destinationEditText = findViewById(R.id.edittext_place);

        /** Retrofit Client for Google Map API **/
        Retrofit retrofit = new Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl("https://maps.googleapis.com/")
                .build();
        apiInterface = retrofit.create(ApiInterface.class);


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                destination = destinationEditText.getText().toString();
                destination = destination.replace(" ", "+");
                Log.d(TAG, destination);
                mapFragment.getMapAsync(MapsActivity.this);
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Get the current location of the device and set the position of the map.
//        getDeviceLocation();


        final double latitude = 23.7908;
        double longitude = 90.4109;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Add a marker in Home and move the camera
        home = new LatLng(23.8022834, 90.425192);

        /** Current Location **/
        if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        mMap.setMyLocationEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);




        mMap.addMarker(new MarkerOptions().position(home).title("Marker in Home"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(home));
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder()
                .target(googleMap.getCameraPosition().target)
                .zoom(17)
                .bearing(30)
                .tilt(45)
                .build()));
        apiInterface.getDirections("driving", "less_driving",
                latitude + "," + longitude, destination,
                getResources().getString(R.string.google_directions_key))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new SingleObserver<Result>() {

                            @Override
                            public void onSubscribe(Disposable d) {

                            }

                            @Override
                            public void onSuccess(Result result) {
                                List<Route> routeList = result.getRoutes();
                                for (Route route : routeList) {
                                    String polyLine = route.getOverviewPolyline().getPoints();
                                    polyLineList = decodePoly(polyLine);
                                    drawPolyLineAndAnimateCar();
                                }
                            }

                            @Override
                            public void onError(Throwable e) {
                                e.printStackTrace();
                            }
                        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //This is an event bus for receiving journey events this can be shifted anywhere
        //in code.
        //Do remember to dispose when not in use. For eg. its necessary to dispose it in
        //onStop as activity is not visible.
        disposable = JourneyEventBus.getInstance().getOnJourneyEvent()
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                        if (o instanceof BeginJourneyEvent) {
                            Snackbar.make(linearLayout, "Journey has started",
                                    Snackbar.LENGTH_SHORT).show();
                        } else if (o instanceof EndJourneyEvent) {
                            Snackbar.make(linearLayout, "Journey has ended",
                                    Snackbar.LENGTH_SHORT).show();
                        } else if (o instanceof CurrentJourneyEvent) {
                            /*
                             * This can be used to receive the current location update of the car
                             */
                            //Log.d(TAG,"Current "+((CurrentJourneyEvent) o).getCurrentLatLng());
                        }
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private void drawPolyLineAndAnimateCar() {
        //Adjusting bounds
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : polyLineList) {
            builder.include(latLng);
        }
        LatLngBounds bounds = builder.build();
        CameraUpdate mCameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 2);
        mMap.animateCamera(mCameraUpdate);

        polylineOptions = new PolylineOptions();
        polylineOptions.color(Color.GRAY);
        polylineOptions.width(5);
        polylineOptions.startCap(new SquareCap());
        polylineOptions.endCap(new SquareCap());
        polylineOptions.jointType(ROUND);
        polylineOptions.addAll(polyLineList);
        greyPolyLine = mMap.addPolyline(polylineOptions);

        blackPolylineOptions = new PolylineOptions();
        blackPolylineOptions.width(5);
        blackPolylineOptions.color(Color.BLACK);
        blackPolylineOptions.startCap(new SquareCap());
        blackPolylineOptions.endCap(new SquareCap());
        blackPolylineOptions.jointType(ROUND);
        blackPolyline = mMap.addPolyline(blackPolylineOptions);

        mMap.addMarker(new MarkerOptions()
                .position(polyLineList.get(polyLineList.size() - 1)));

        ValueAnimator polylineAnimator = ValueAnimator.ofInt(0, 100);
        polylineAnimator.setDuration(2000);
        polylineAnimator.setInterpolator(new LinearInterpolator());
        polylineAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                List<LatLng> points = greyPolyLine.getPoints();
                int percentValue = (int) valueAnimator.getAnimatedValue();
                int size = points.size();
                int newPoints = (int) (size * (percentValue / 100.0f));
                List<LatLng> p = points.subList(0, newPoints);
                blackPolyline.setPoints(p);
            }
        });
        polylineAnimator.start();
        marker = mMap.addMarker(new MarkerOptions().position(home)
                .flat(true)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_car)));
        handler = new Handler();
        index = -1;
        next = 1;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index < polyLineList.size() - 1) {
                    index++;
                    next = index + 1;
                }
                if (index < polyLineList.size() - 1) {
                    startPosition = polyLineList.get(index);
                    endPosition = polyLineList.get(next);
                }
                if (index == 0) {
                    BeginJourneyEvent beginJourneyEvent = new BeginJourneyEvent();
                    beginJourneyEvent.setBeginLatLng(startPosition);
                    JourneyEventBus.getInstance().setOnJourneyBegin(beginJourneyEvent);
                }
                if (index == polyLineList.size() - 1) {
                    EndJourneyEvent endJourneyEvent = new EndJourneyEvent();
                    endJourneyEvent.setEndJourneyLatLng(new LatLng(polyLineList.get(index).latitude,
                            polyLineList.get(index).longitude));
                    JourneyEventBus.getInstance().setOnJourneyEnd(endJourneyEvent);
                }
                ValueAnimator valueAnimator = ValueAnimator.ofFloat(0, 1);
                valueAnimator.setDuration(3000);
                valueAnimator.setInterpolator(new LinearInterpolator());
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator valueAnimator) {
                        v = valueAnimator.getAnimatedFraction();
                        lng = v * endPosition.longitude + (1 - v)
                                * startPosition.longitude;
                        lat = v * endPosition.latitude + (1 - v)
                                * startPosition.latitude;
                        LatLng newPos = new LatLng(lat, lng);
                        CurrentJourneyEvent currentJourneyEvent = new CurrentJourneyEvent();
                        currentJourneyEvent.setCurrentLatLng(newPos);
                        JourneyEventBus.getInstance().setOnJourneyUpdate(currentJourneyEvent);
                        marker.setPosition(newPos);
                        marker.setAnchor(0.5f, 0.5f);
                        marker.setRotation(getBearing(startPosition, newPos));
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition
                                (new CameraPosition.Builder().target(newPos)
                                        .zoom(15.5f).build()));
                    }
                });
                valueAnimator.start();
                if (index != polyLineList.size() - 1) {
                    handler.postDelayed(this, 3000);
                }
            }
        }, 3000);
    }


    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }

    private float getBearing(LatLng begin, LatLng end) {
        double lat = Math.abs(begin.latitude - end.latitude);
        double lng = Math.abs(begin.longitude - end.longitude);

        if (begin.latitude < end.latitude && begin.longitude < end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)));
        else if (begin.latitude >= end.latitude && begin.longitude < end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 90);
        else if (begin.latitude >= end.latitude && begin.longitude >= end.longitude)
            return (float) (Math.toDegrees(Math.atan(lng / lat)) + 180);
        else if (begin.latitude < end.latitude && begin.longitude >= end.longitude)
            return (float) ((90 - Math.toDegrees(Math.atan(lng / lat))) + 270);
        return -1;
    }

    /** Getting Current Location **/
//
//    private void getCurrentLocation() {
//        if (locationUtilObj == null) {
//            locationUtilObj = new LocationUtil(this, (LocationUtil.GetLocationListener) this);
//        } else {
//            locationUtilObj.checkLocationSettings();
//        }
//    }
//
//    @Override
//    public void location_Error(String error) {
//        Log.e("LOCATION_ERROR",error);
//    }

    private double bearingBetweenLocations(LatLng latLng1, LatLng latLng2) {

        double PI = 3.14159;
        double lat1 = latLng1.latitude * PI / 180;
        double long1 = latLng1.longitude * PI / 180;
        double lat2 = latLng2.latitude * PI / 180;
        double long2 = latLng2.longitude * PI / 180;

        double dLon = (long2 - long1);

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon);

        double brng = Math.atan2(y, x);

        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;

        return brng;
    }

    /** Runtime Permissions **/

//    /**
//     * custom method to check and request for run time permissions
//     * if not granted already
//     */
//    private void checkAndRequestRunTimePermissions() {
//        if (Build.VERSION.SDK_INT >= 23) {
//            // Marshmallow+
//
//            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
//                        REQUEST_CODE_PERMISSION_MULTIPLE);
//
//            }
//        }
//
//
//        onRunTimePermissionGranted();
//    }
//    /******************************************************/
//    /**
//     * custom method to execute after run time permissions
//     * granted or if it run time permission no required at all
//     */
//    private void onRunTimePermissionGranted() {
//
//        isDeninedRTPs = false;
//        getCurrentLocation();
//    }
//    /******************************************************/
//
//    /**
//     * predefined method to check run time permissions list call back
//     *
//     * @param requestCode   : to handle the corresponding request
//     * @param permissions:  contains the list of requested permissions
//     * @param grantResults: contains granted and un granted permissions result list
//     */
//
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        switch (requestCode) {
//            case 1000: {
//                // If request is cancelled, the result arrays are empty.
//                if (grantResults.length > 0
//                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    mFusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
//                        @Override
//                        public void onSuccess(Location location) {
//                            if (location != null) {
//                                Toast.makeText(MapsActivity.this, "Permission Granted", Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    });
//                } else {
//                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
//                }
//                break;
//            }
//        }
//    }
//
//
//
//
//
////
////    @Override
////    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
////        if (requestCode == REQUEST_CODE_PERMISSION_MULTIPLE) {
////            if (grantResults.length > 0) {
////                for (int i = 0; i < permissions.length; i++) {
////                    String permission = permissions[i];
////                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
////                        isDeninedRTPs = true;
////                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
////                            showRationaleRTPs = shouldShowRequestPermissionRationale(permission);
////                        }
////
////                        break;
////                    }
////
////                }
////                onRunTimePermissionDenied();
////            }
////        } else {
////            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
////        }
////    }
//
//    /******************************************************/
//
//    private void onRunTimePermissionDenied() {
//        if (isDeninedRTPs) {
//            if (!showRationaleRTPs) {
//                //goToSettings();
//            } else {
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
//                            REQUEST_CODE_PERMISSION_MULTIPLE);
//                }
//            }
//        } else {
//            onRunTimePermissionGranted();
//        }
//    }
}
