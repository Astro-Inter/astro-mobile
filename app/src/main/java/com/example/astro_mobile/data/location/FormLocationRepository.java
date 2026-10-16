package com.example.astro_mobile.data.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationRequest;
import android.os.CancellationSignal;

import androidx.core.content.ContextCompat;

public final class FormLocationRepository {
    public enum Failure { PERMISSION_REQUIRED, LOCATION_DISABLED, UNAVAILABLE }

    public interface Callback {
        void onLocation(Location location);
        void onFailure(Failure failure);
    }

    private final Context context;
    private final LocationManager manager;
    private CancellationSignal pending;

    public FormLocationRepository(Context context) {
        this.context = context.getApplicationContext();
        manager = this.context.getSystemService(LocationManager.class);
    }

    @SuppressLint("MissingPermission")
    public void requestCurrent(Callback callback) {
        cancel();
        // A permissão precisa é conferida antes de cada leitura, inclusive após voltar ao app.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            callback.onFailure(Failure.PERMISSION_REQUIRED);
            return;
        }
        if (manager == null || !manager.isLocationEnabled()) {
            callback.onFailure(Failure.LOCATION_DISABLED);
            return;
        }
        String provider = manager.hasProvider(LocationManager.FUSED_PROVIDER)
                && manager.isProviderEnabled(LocationManager.FUSED_PROVIDER)
                ? LocationManager.FUSED_PROVIDER : LocationManager.GPS_PROVIDER;
        if (!manager.hasProvider(provider) || !manager.isProviderEnabled(provider)) {
            callback.onFailure(Failure.LOCATION_DISABLED);
            return;
        }
        CancellationSignal signal = new CancellationSignal();
        pending = signal;
        // Uma leitura atual tem prazo próprio e pode ser cancelada ao sair da tela.
        LocationRequest request = new LocationRequest.Builder(0L)
                .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
                .setDurationMillis(15_000L).setMaxUpdates(1).build();
        try {
            manager.getCurrentLocation(provider, request, signal, context.getMainExecutor(), location -> {
                if (signal.isCanceled()) return;
                pending = null;
                if (location == null) callback.onFailure(Failure.UNAVAILABLE);
                else callback.onLocation(new Location(location));
            });
        } catch (SecurityException error) {
            pending = null;
            callback.onFailure(Failure.PERMISSION_REQUIRED);
        } catch (IllegalArgumentException error) {
            pending = null;
            callback.onFailure(Failure.UNAVAILABLE);
        }
    }

    public void cancel() {
        if (pending != null) pending.cancel();
        pending = null;
    }
}
