package ir.map.hoospital;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mapbox.core.constants.Constants;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.nostra13.universalimageloader.utils.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ir.map.sdk_common.MapirLatLng;
import ir.map.sdk_map.annotations.IconFactory;
import ir.map.sdk_map.annotations.Marker;
import ir.map.sdk_map.annotations.MarkerOptions;
import ir.map.sdk_map.annotations.Polyline;
import ir.map.sdk_map.annotations.PolylineOptions;
import ir.map.sdk_map.geometry.LatLng;
import ir.map.sdk_map.maps.MapirMap;
import ir.map.sdk_map.maps.SupportMapFragment;
import ir.map.sdk_services.RouteMode;
import ir.map.sdk_services.ServiceHelper;
import ir.map.sdk_services.models.MapirError;
import ir.map.sdk_services.models.MapirRouteResponse;
import ir.map.sdk_services.models.MapirSearchItem;
import ir.map.sdk_services.models.MapirSearchResponse;
import ir.map.sdk_services.models.base.ResponseListener;

public class MainActivity extends AppCompatActivity implements MapirMap.OnMapClickListener {

    private MapirMap mapirMap;
    private Marker mapMarker;
    private List<Marker> hospitalMarkers = new ArrayList<>();
    private Polyline routeLine;
    private Button routeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        routeButton = findViewById(R.id.button);
        routeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sortNearestHospital();
                routeToNearest();
            }
        });

        SupportMapFragment supportMapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.myMapView);

        supportMapFragment.getMapAsync(mapirMap -> {
            this.mapirMap = mapirMap;
            this.mapirMap.addOnMapClickListener(this);
        });
    }

    @Override
    public void onMapClick(@NonNull LatLng point) {
        if (this.mapirMap != null) // Checks if we were successful in obtaining the map
            //mTehran object holds marker instance for future use like remove marker from Map
            if (mapMarker == null) {
                mapMarker = mapirMap.addMarker(new MarkerOptions()
                        .position(point));
            } else {
                mapMarker.setPosition(point);
            }
        if (routeLine != null)
            mapirMap.removePolyline(routeLine);

        new ServiceHelper().search("بیمارستان", point.getLatitude(), point.getLongitude(), null, new ResponseListener<MapirSearchResponse>() {
            @Override
            public void onSuccess(MapirSearchResponse mapirSearchResponse) {
                if (!hospitalMarkers.isEmpty()) {
                    mapirMap.removeAnnotations(hospitalMarkers);
                }
                for (MapirSearchItem item : mapirSearchResponse.values) {
                    hospitalMarkers.add(mapirMap.addMarker(new MarkerOptions()
                            .icon(IconFactory.getInstance(MainActivity.this)
                                    .fromResource(R.drawable.hospital))
                            .setPosition(new LatLng(item.coordinate.latitude, item.coordinate.longitude))
                            .title(item.title)));
                }
                routeButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(MapirError mapirError) {
                Toast.makeText(getBaseContext(), mapirError.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void routeToNearest() {
        if (routeLine != null)
            mapirMap.removePolyline(routeLine);
        new ServiceHelper().getRouteInfo(new MapirLatLng(mapMarker.getPosition().getLatitude(),
                        mapMarker.getPosition().getLongitude()),
                new MapirLatLng(hospitalMarkers.get(0).getPosition().getLatitude(),
                        hospitalMarkers.get(0).getPosition().getLongitude()),
                RouteMode.BASIC, new ResponseListener<MapirRouteResponse>() {
                    @Override
                    public void onSuccess(MapirRouteResponse mapirRouteResponse) {
                        drawRoute(mapirRouteResponse);
                        routeButton.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError(MapirError mapirError) {
                        Toast.makeText(getBaseContext(), mapirError.message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void drawRoute(MapirRouteResponse routingInfo) {
        List<LatLng> points = new ArrayList<>();
        List<Point> coordinates = LineString.fromPolyline(routingInfo.routes.get(0).geometry,
                Constants.PRECISION_5).coordinates();

        for (Point point : coordinates)
            points.add(new LatLng(point.latitude(), point.longitude()));

        routeLine = mapirMap.addPolyline(new PolylineOptions()
                .addAll(points)
                .color(Color.BLUE)
                .width(5));
    }

    private void sortNearestHospital() {
        Collections.sort(hospitalMarkers, new Comparator<Marker>() {
            @Override
            public int compare(Marker o1, Marker o2) {
                return Double.compare(o1.getPosition().distanceTo(mapMarker.getPosition()), o2.getPosition().distanceTo(mapMarker.getPosition()));
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (!hospitalMarkers.isEmpty()) {
            mapirMap.removeAnnotations(hospitalMarkers);
            if (routeLine != null) {
                mapirMap.removePolyline(routeLine);
                routeLine = null;
            }
            if (mapMarker != null) {
                mapirMap.removeMarker(mapMarker);
                mapMarker = null;
            }
            hospitalMarkers.clear();
        } else {
            super.onBackPressed();
        }
    }
}
