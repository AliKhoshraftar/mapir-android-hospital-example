package ir.map.hoospital;

import android.app.Application;

import ir.map.sdk_map.Mapir;
import ir.map.sdk_services.ServiceSDK;

public class AppController extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Mapir.getInstance(this, "YOUR_MAPIR_TOKEN");
        ServiceSDK.init(this, "YOUR_MAPIR_TOKEN");
    }
}
