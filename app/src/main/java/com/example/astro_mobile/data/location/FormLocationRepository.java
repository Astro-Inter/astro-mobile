package com.example.astro_mobile.data.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;

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
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeout;

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
        String provider = availableProvider(LocationManager.FUSED_PROVIDER);
        if (provider == null) provider = availableProvider(LocationManager.NETWORK_PROVIDER);
        if (provider == null) provider = availableProvider(LocationManager.GPS_PROVIDER);
        if (provider == null) {
            callback.onFailure(Failure.LOCATION_DISABLED);
            return;
        }
        CancellationSignal signal = new CancellationSignal();
        pending = signal;
        // Uma leitura atual tem prazo próprio e pode ser cancelada ao sair da tela.
        timeout = () -> {
            cancel();
            callback.onFailure(Failure.UNAVAILABLE);
        };
        handler.postDelayed(timeout, 15_000L);
        try {
            manager.getCurrentLocation(provider, signal, context.getMainExecutor(), location -> {
                if (signal.isCanceled()) return;
                cancel();
                if (location == null) callback.onFailure(Failure.UNAVAILABLE);
                else callback.onLocation(new Location(location));
            });
        } catch (SecurityException error) {
            cancel();
            callback.onFailure(Failure.PERMISSION_REQUIRED);
        } catch (IllegalArgumentException error) {
            cancel();
            callback.onFailure(Failure.UNAVAILABLE);
        }
    }

    private String availableProvider(String provider) {
        return manager.getAllProviders().contains(provider) && manager.isProviderEnabled(provider)
                ? provider : null;
    }

    public void cancel() {
        if (pending != null) pending.cancel();
        pending = null;
        if (timeout != null) handler.removeCallbacks(timeout);
        timeout = null;
    }
}
