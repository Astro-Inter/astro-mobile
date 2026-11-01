package com.example.astro_mobile.shared.profile;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.DrawableRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.astro_mobile.R;

public final class PhotoViewerDialog extends DialogFragment {
    public static void show(Fragment host, @Nullable String url, @DrawableRes int fallback) {
        if (host.getParentFragmentManager().isStateSaved()
                || host.getParentFragmentManager().findFragmentByTag("photo_viewer") != null) return;
        PhotoViewerDialog viewer = new PhotoViewerDialog();
        Bundle args = new Bundle();
        args.putString("url", url);
        args.putInt("fallback", fallback);
        viewer.setArguments(args);
        viewer.show(host.getParentFragmentManager(), "photo_viewer");
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_photo_viewer);
        View content = dialog.findViewById(R.id.container_photo_viewer);
        content.findViewById(R.id.button_photo_viewer_close).setOnClickListener(clicked -> dismiss());
        String url = requireArguments().getString("url");
        int fallback = requireArguments().getInt("fallback");
        // Mostra a imagem inteira; o avatar circular continua apenas na tela de origem.
        Object image = url == null || url.trim().isEmpty() ? fallback : url;
        Glide.with(this).load(image).fitCenter().listener(new RequestListener<Drawable>() {
            @Override public boolean onLoadFailed(@Nullable GlideException error, Object model,
                    Target<Drawable> target, boolean first) {
                content.findViewById(R.id.progress_photo_viewer).setVisibility(View.GONE);
                content.findViewById(R.id.text_photo_viewer_error).setVisibility(View.VISIBLE);
                return false;
            }
            @Override public boolean onResourceReady(Drawable resource, Object model,
                    Target<Drawable> target, DataSource source, boolean first) {
                content.findViewById(R.id.progress_photo_viewer).setVisibility(View.GONE);
                return false;
            }
        }).into((ImageView) content.findViewById(R.id.image_photo_viewer));
        return dialog;
    }

    @Override public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.argb(230, 0, 0, 0)));
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }
}
