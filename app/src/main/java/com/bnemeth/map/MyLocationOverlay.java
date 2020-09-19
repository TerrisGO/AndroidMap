package com.bnemeth.map;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Style;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Point;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.overlay.Circle;

public class MyLocationOverlay extends Layer {

    Circle circle;
    Paint paint;
    Boolean positionSet = false;

    public MyLocationOverlay(){

        paint = AndroidGraphicFactory.INSTANCE.createPaint();
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);
        paint.setStyle(Style.STROKE);

        LatLong latLong = new LatLong(47.545809, 19.034227);

        circle = new Circle(latLong, 100, null, paint);
        //circle.setDisplayModel(this.displayModel);
    }

    @Override
    public synchronized  void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint){
        if (positionSet) {
            this.circle.draw(boundingBox, zoomLevel, canvas, topLeftPoint);
        }
    }
    @Override
    protected void onAdd() {
        this.circle.setDisplayModel(this.displayModel);
    }

    @Override
    public void onDestroy() {
        this.circle.onDestroy();
    }

    public void setPosition(double latitude, double longitude, float accuracy) {
        synchronized (this) {
            positionSet = true;
            LatLong latLong = new LatLong(latitude, longitude);
            //this.marker.setLatLong(latLong);
            //if (this.circle != null) {
                //circle = new Circle(latLong, 100, null, paint);
                this.circle.setLatLong(latLong);
                this.circle.setRadius(accuracy);
            //}
            //circle = new Circle(latLong, 100, null, paint);
            requestRedraw();
        }
    }
}

