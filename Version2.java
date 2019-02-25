package com.example.shiva;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.location.GnssClock;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.Math;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {

    private LocationManager mLocationManager;
    private GnssMeasurementsEvent.Callback gnssMeasurementsEventListener;
    final static long GPS_WEEK_SEC = 604800;
    final static double NANOS_to_SECONDS = Math.pow(10, -9);
    final static long SPEED_OF_LIGHT = 299792458;

    TextView prTextView, weekNumTextView, tRxSecondsView, tTxSecondsView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toast.makeText(this, "Please confirm GPS week number before considering measurements", Toast.LENGTH_SHORT).show();
        mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        MainActivityPermissionsDispatcher.getPseudoRangeWithPermissionCheck(this);
    }

    @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    protected void getPseudoRange() {
        prTextView = (TextView) findViewById(R.id.prTextView);
        weekNumTextView = (TextView) findViewById(R.id.weekNumTextView);
        tRxSecondsView = (TextView) findViewById(R.id.prTimeTextView);
        tTxSecondsView = (TextView) findViewById(R.id.satIDTextView);

        //The following control flow is to check if the ACCESS_FINE_LOCATION has been successfully granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mLocationManager.registerGnssMeasurementsCallback(gnssMeasurementsEventListener = new GnssMeasurementsEvent.Callback() {
            @Override
            public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {

                GnssClock clock = event.getClock();

                long weekNumber = (long) Math.floor(-(double) clock.getFullBiasNanos() * NANOS_to_SECONDS / GPS_WEEK_SEC);  //I'm not sure why, but sometimes the week number can differ by 1. So do confirm from the internet
                long weekNumberNanos = (long) (weekNumber * GPS_WEEK_SEC * Math.pow(10, 9));
                long tRxNanos = clock.getTimeNanos() - clock.getFullBiasNanos() - weekNumberNanos;
                weekNumTextView.setText("Week number: " + Long.toString(weekNumber));

                if (clock.hasBiasNanos()) {
                    for (GnssMeasurement measurement : event.getMeasurements()) {
                        double tRxSeconds = ((double) tRxNanos - measurement.getTimeOffsetNanos() - clock.getBiasNanos()) * NANOS_to_SECONDS;   //tRxSeconds is the time at which GPS signal is recieved
                        double tTxSeconds = (double) measurement.getReceivedSvTimeNanos() * NANOS_to_SECONDS;       //tTxSeconds is the time at which GPS signal is transmitted from the satellite


                        double prSeconds = tRxSeconds - tTxSeconds;
                        double prMetres = prSeconds * SPEED_OF_LIGHT;
                        tRxSecondsView.setText("Pseudo range in seconds: " + Double.toString(tRxSeconds - tTxSeconds));
                        tTxSecondsView.setText("Satellite ID: " + Integer.toString(measurement.getSvid()));
                        prTextView.setText(Double.toString(prMetres));
                    }
                }
            }
        });
    }


   /* @NeedsPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    protected void getPseudoRange(){
        final TextView prTextView = (TextView) findViewById(R.id.prTextView);
        gnssMeasurementsEventListener=
                new GnssMeasurementsEvent.Callback() {
                    @Override
                    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
                        GnssClock clock = event.getClock();
                        long weekNumber = (long)Math.floor(((double)clock.getFullBiasNanos() * NANOS_to_SECONDS/GPS_WEEK_SEC));
                        long weekNumberNanos = (long)(weekNumber*GPS_WEEK_SEC*Math.pow(10,9));
                        long tRxNanos = clock.getTimeNanos() - clock.getFullBiasNanos() - weekNumberNanos;
                        if (clock.hasBiasNanos()){
                            for (GnssMeasurement measurement : event.getMeasurements()){
                                double tRxSeconds = ((double)tRxNanos - measurement.getTimeOffsetNanos() - clock.getBiasNanos())* NANOS_to_SECONDS;
                                double tTxSeconds = (double)measurement.getReceivedSvTimeNanos()*NANOS_to_SECONDS;
                                double prSeconds = tRxSeconds - tTxSeconds;
                                double prMetres = prSeconds*SPEED_OF_LIGHT;
                                prTextView.setText(Double.toString(prMetres));
                            }
                        }
                    }
                };
    }*/

    @Override
    public void onRequestPermissionsResult(int requestcode, String[] permissions, int[] grantResults){
        super.onRequestPermissionsResult(requestcode, permissions, grantResults);
        //Delegating the permission handling to generated method
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestcode, grantResults);
    }
}
