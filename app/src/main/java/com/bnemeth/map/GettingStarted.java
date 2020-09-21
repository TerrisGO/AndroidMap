package com.bnemeth.map;

/*
 * Copyright 2018-2019 devemux86
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileUtils;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.rendertheme.AssetsRenderTheme;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.android.view.MapView;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.layer.Layers;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.XmlRenderTheme;
import org.mapsforge.map.rendertheme.ExternalRenderTheme;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * A very basic Android app example.
 * <p>
 * You'll need a map with filename berlin.map from download.mapsforge.org in device storage:
 * /sdcard/Android/data/org.mapsforge.samples.android/files/
 */
public class GettingStarted extends Activity {

    // Name of the map file in device storage
    private static final String MAP_FILE = "hungary.map";

    private MapView mapView;

    final MyLocationOverlay overlay = new MyLocationOverlay();;

    private Boolean IsRequestLocationUpdatesRunning = false;
    private Location LastLocation = null;
    LocationListener ContinuousLocationListener = null;

    DownloadManager DownloadManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
         * Before you make any calls on the mapsforge library, you need to initialize the
         * AndroidGraphicFactory. Behind the scenes, this initialization process gathers a bit of
         * information on your device, such as the screen resolution, that allows mapsforge to
         * automatically adapt the rendering for the device.
         * If you forget this step, your app will crash. You can place this code, like in the
         * Samples app, in the Android Application class. This ensures it is created before any
         * specific activity. But it can also be created in the onCreate() method in your activity.
         */
        AndroidGraphicFactory.createInstance(getApplication());

        /*
         * A MapView is an Android View (or ViewGroup) that displays a mapsforge map. You can have
         * multiple MapViews in your app or even a single Activity. Have a look at the mapviewer.xml
         * on how to create a MapView using the Android XML Layout definitions. Here we create a
         * MapView on the fly and make the content view of the activity the MapView. This means
         * that no other elements make up the content of this activity.
         */
        mapView = new MapView(this);

        //setContentView(mapView);

        setContentView(R.layout.activity_getting_started);

        LinearLayout layout = findViewById(R.id.layoutMap);
        layout.addView(mapView);

        InitMap();

        findViewById(R.id.buttonGpsCenter).setBackgroundColor(Color.TRANSPARENT);
        findViewById(R.id.buttonGpsCenterContinuous).setBackgroundColor(Color.TRANSPARENT);
        findViewById(R.id.buttonDownloadMap).setBackgroundColor(Color.TRANSPARENT);
    }

    @Override
    protected void onDestroy() {
        /*
         * Whenever your activity exits, some cleanup operations have to be performed lest your app
         * runs out of memory.
         */
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();

        unregisterReceiver(onComplete);
    }

    private void InitMap(){
        try {
            /*
             * We then make some simple adjustments, such as showing a scale bar and zoom controls.
             */
            mapView.getMapScaleBar().setVisible(true);
            mapView.setBuiltInZoomControls(false);

            /*
             * To avoid redrawing all the tiles all the time, we need to set up a tile cache with an
             * utility method.
             */
            TileCache tileCache = AndroidUtil.createTileCache(this, "mapcache",
                    mapView.getModel().displayModel.getTileSize(), 1f,
                    mapView.getModel().frameBufferModel.getOverdrawFactor());

            /*
             * Now we need to set up the process of displaying a map. A map can have several layers,
             * stacked on top of each other. A layer can be a map or some visual elements, such as
             * markers. Here we only show a map based on a mapsforge map file. For this we need a
             * TileRendererLayer. A TileRendererLayer needs a TileCache to hold the generated map
             * tiles, a map file from which the tiles are generated and Rendertheme that defines the
             * appearance of the map.
             */
            File mapFile = new File(getExternalFilesDir(null), MAP_FILE);
            MapDataStore mapDataStore = new MapFile(mapFile);
            TileRendererLayer tileRendererLayer = new TileRendererLayer(tileCache, mapDataStore,
                    mapView.getModel().mapViewPosition, AndroidGraphicFactory.INSTANCE);

            //tileRendererLayer.setXmlRenderTheme(InternalRenderTheme.DEFAULT);

            File themeFile = new File(getExternalFilesDir(null), "Vectorial_V7/Vectorial_V7.xml");
            XmlRenderTheme theme = new ExternalRenderTheme(themeFile);

            //XmlRenderTheme theme = new AssetsRenderTheme(this, "OpenmapsTheme/","theme.xml");

            ///XmlRenderTheme theme = new AssetsRenderTheme(this, "", themeFile.getAbsolutePath());
            //XmlRenderTheme theme = new AssetsRenderTheme(getApplicationContext(), "Openmaps/", "Theme.xml");

            tileRendererLayer.setXmlRenderTheme(theme);

            /*
             * On its own a tileRendererLayer does not know where to display the map, so we need to
             * associate it with our mapView.
             */
            mapView.getLayerManager().getLayers().add(tileRendererLayer);

            /*
             * The map also needs to know which area to display and at what zoom level.
             * Note: this map position is specific to Berlin area.
             */
            mapView.setCenter(new LatLong(47.498333,19.0408337)); // Budapest
            mapView.setZoomLevel((byte) 12);

            /*
            Layers layers = mapView.getLayerManager().getLayers();
            LatLong latLong = new LatLong(47.545809, 19.034227);

            Paint paint = AndroidGraphicFactory.INSTANCE.createPaint();
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(10);
            paint.setStyle(Style.FILL);


            Circle circle = new Circle(latLong, 20, paint, paint);
            layers.add(circle);*/

            //overlay = new MyLocationOverlay();

            mapView.getLayerManager().getLayers().add(overlay);

        }
        catch (Exception e) {
            /*
             * In case of map file errors avoid crash, but developers should handle these cases!
             */
            e.printStackTrace();
        }
    }

    public void buttonGpsCenterClick(View view){
        if (IsRequestLocationUpdatesRunning){
            if (LastLocation != null){
                //overlay.setPosition(LastLocation.getLatitude(), LastLocation.getLongitude(), LastLocation.getAccuracy());
                mapView.setCenter(new LatLong(LastLocation.getLatitude(), LastLocation.getLongitude()));
                mapView.setZoomLevel((byte) 15);
            }

            return;
        }

        try{
            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);

            LocationListener listener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    overlay.setPosition(location.getLatitude(), location.getLongitude(), location.getAccuracy());
                    mapView.setCenter(new LatLong(location.getLatitude(), location.getLongitude()));
                    mapView.setZoomLevel((byte) 15);
                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };

            lm.requestSingleUpdate(LocationManager.GPS_PROVIDER, listener , getMainLooper());
        }
        catch (SecurityException e) {
            /*
             * In case of map file errors avoid crash, but developers should handle these cases!
             */
            e.printStackTrace();
            throw e;
        }
    }

    public void buttonGpsCenterContinuousClick(View view){
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        Button button = (Button)view;

        if (IsRequestLocationUpdatesRunning){
            IsRequestLocationUpdatesRunning = false;

            if (ContinuousLocationListener != null){
                lm.removeUpdates(ContinuousLocationListener);
            }
            LastLocation = null;
            button.setBackgroundColor(Color.TRANSPARENT);
            return;
        }

        button.setBackgroundColor(Color.GRAY);

        ContinuousLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                overlay.setPosition(location.getLatitude(), location.getLongitude(), location.getAccuracy());

                if(LastLocation == null){
                    mapView.setCenter(new LatLong(location.getLatitude(), location.getLongitude()));
                }
                LastLocation = location;
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        try{
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 15000, 0, ContinuousLocationListener , getMainLooper());
            IsRequestLocationUpdatesRunning = true;
        }
        catch (SecurityException e) {
            /*
             * In case of map file errors avoid crash, but developers should handle these cases!
             */
            e.printStackTrace();
            IsRequestLocationUpdatesRunning = false;
            lm.removeUpdates(ContinuousLocationListener);
        }
    }

    public void buttonDownloadMapClick(View view){
        try{
            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            File existingMapFile = new File(getExternalFilesDir(null), "hungary.zip");
            if (existingMapFile.exists()){
                existingMapFile.delete();
            }

            //File mapFile = new File(getExternalFilesDir(null), MAP_FILE);
            DownloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
            Uri uri = Uri.parse("http://openmaps.eu/dltosm.php?dl=hungary_openmaps_eu_europe.map.zip");
            DownloadManager.Request request =  new DownloadManager.Request(uri);
            request.setDestinationInExternalFilesDir(this, null, "hungary.zip");
            DownloadManager.enqueue(request);

            File existingThemeFile = new File(getExternalFilesDir(null), "Vectorial_V7/Vectorial_V7.xml");
            if (!existingThemeFile.exists()){
                File existingThemeFileZip = new File(getExternalFilesDir(null), "Vectorial_V7.zip");
                if (existingThemeFileZip.exists()){
                    existingThemeFileZip.delete();
                }

                Uri themeUri = Uri.parse("https://openmaps.eu/renderthemes/Vectorial_V7.zip");
                DownloadManager.Request themeRequest =  new DownloadManager.Request(themeUri);
                themeRequest.setDestinationInExternalFilesDir(this, null, "Vectorial_V7.zip");
                DownloadManager.enqueue(themeRequest);
            }
        }
        catch (Exception e){
            e.printStackTrace();
            throw e;
        }
    }

    BroadcastReceiver onComplete= new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
           extractMapFile();
           extractThemeFile();
        }

        private void extractMapFile(){
            try{
                File mapFile = new File(getExternalFilesDir(null), "hungary.zip");
                if (!mapFile.exists()){
                    return;
                }

                ZipFile zipFile = new ZipFile(mapFile);
                //InputStream stream = zipFile.getInputStream();
                for(Enumeration e = zipFile.entries(); e.hasMoreElements();){
                    ZipEntry entry = (ZipEntry)e.nextElement();
                    if(entry.isDirectory()){
                        continue;
                    }

                    File file = new File(getExternalFilesDir(null), MAP_FILE);
                    BufferedInputStream stream = new BufferedInputStream(zipFile.getInputStream(entry));
                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));

                    try {
                        //IOUtils.copy(stream, outputStream);
                        copyStream(stream, outputStream);
                    }
                    finally {
                        stream.close();
                        outputStream.close();
                    }
                }

                mapFile.delete();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        private  void extractThemeFile(){
            try{
                File mapFile = new File(getExternalFilesDir(null), "Vectorial_V7.zip");
                if (!mapFile.exists()){
                    return;
                }

                ZipFile zipFile = new ZipFile(mapFile);
                //InputStream stream = zipFile.getInputStream();
                for(Enumeration e = zipFile.entries(); e.hasMoreElements();){
                    ZipEntry entry = (ZipEntry)e.nextElement();
                    if(entry.isDirectory()){
                        File dir = new File(getExternalFilesDir(null), entry.getName());
                        dir.mkdir();
                        continue;
                    }

                    File file = new File(getExternalFilesDir(null), entry.getName());
                    BufferedInputStream stream = new BufferedInputStream(zipFile.getInputStream(entry));
                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));

                    try {
                        //IOUtils.copy(stream, outputStream);
                        copyStream(stream, outputStream);
                    }
                    finally {
                        stream.close();
                        outputStream.close();
                    }
                }

                mapFile.delete();
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }


}