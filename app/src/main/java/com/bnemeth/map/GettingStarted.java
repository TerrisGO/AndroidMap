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
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import org.mapsforge.core.model.LatLong;
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
import java.io.File;

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

            //File themeFile = new File(getExternalFilesDir(null), "Openmaps/Theme.xml");
            //XmlRenderTheme theme = new ExternalRenderTheme(themeFile);

            XmlRenderTheme theme = new AssetsRenderTheme(this, "OpenmapsTheme/","theme.xml");

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

    @Override
    protected void onDestroy() {
        /*
         * Whenever your activity exits, some cleanup operations have to be performed lest your app
         * runs out of memory.
         */
        mapView.destroyAll();
        AndroidGraphicFactory.clearResourceMemoryCache();
        super.onDestroy();
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

        if (IsRequestLocationUpdatesRunning){
            IsRequestLocationUpdatesRunning = false;

            if (ContinuousLocationListener != null){
                lm.removeUpdates(ContinuousLocationListener);
            }
            LastLocation = null;
        }

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
        //File mapFile = new File(getExternalFilesDir(null), MAP_FILE);
       //DownloadManager = (DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE);
       //Uri uri = Uri.parse("");
       //DownloadManager.enqueue(new DownloadManager.Request())
    }

}