package com.example.astro_mobile.manager.events.evidence;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.astro_mobile.R;
import java.util.ArrayList;

/** Abre as imagens reais enviadas como evidência. */
public final class EventEvidenceFragment extends Fragment {
    private ArrayList<String> images;
    public EventEvidenceFragment() { super(R.layout.fragment_event_evidence); }
    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.button_event_evidence_back).setOnClickListener(clicked -> NavHostFragment.findNavController(this).popBackStack());
        images = requireArguments().getStringArrayList("evidence_images");
        if (images == null || images.isEmpty()) {
            view.findViewById(R.id.progress_event_evidence).setVisibility(View.GONE);
            view.findViewById(R.id.text_event_evidence_error).setVisibility(View.VISIBLE);
            return;
        }
        LinearLayout pages = view.findViewById(R.id.container_event_evidence_pages);
        pages.setVisibility(images.size() > 1 ? View.VISIBLE : View.GONE);
        for (int i = 0; i < images.size(); i++) {
            final int page = i;
            TextView button = new TextView(requireContext(), null, 0, R.style.EventEvidencePage);
            int size = Math.round(65 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            if (i > 0) params.setMarginStart(Math.round(10 * getResources().getDisplayMetrics().density));
            button.setLayoutParams(params); button.setText(String.valueOf(i + 1));
            button.setOnClickListener(clicked -> showImage(view, page)); pages.addView(button);
        }
        int selected = requireArguments().getInt("evidence_selected", 0);
        showImage(view, selected >= 0 && selected < images.size() ? selected : 0);
    }
    private void showImage(View view, int page) {
        LinearLayout pages = view.findViewById(R.id.container_event_evidence_pages);
        for (int i = 0; i < pages.getChildCount(); i++) pages.getChildAt(i).setSelected(i == page);
        view.findViewById(R.id.progress_event_evidence).setVisibility(View.VISIBLE);
        view.findViewById(R.id.text_event_evidence_error).setVisibility(View.GONE);
        Glide.with(this).load(images.get(page)).listener(new RequestListener<Drawable>() {
            @Override public boolean onLoadFailed(@Nullable GlideException error, Object model, Target<Drawable> target, boolean first) {
                if (getView() == view) { view.findViewById(R.id.progress_event_evidence).setVisibility(View.GONE); view.findViewById(R.id.text_event_evidence_error).setVisibility(View.VISIBLE); }
                return false;
            }
            @Override public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource source, boolean first) {
                if (getView() == view) view.findViewById(R.id.progress_event_evidence).setVisibility(View.GONE);
                return false;
            }
        }).into((ImageView) view.findViewById(R.id.image_event_evidence));
    }
}
