package com.example.astro_mobile.shared.ui;

import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.example.astro_mobile.R;

public final class PullToRefresh {
    private PullToRefresh() { }

    // Envolve apenas o conteúdo da tela; a barra de navegação permanece fixa.
    public static void attach(Fragment fragment, View content, View scrollable, Runnable reload) {
        ViewGroup parent = (ViewGroup) content.getParent();
        int index = parent.indexOfChild(content);
        ViewGroup.LayoutParams params = content.getLayoutParams();
        parent.removeViewAt(index);
        SwipeRefreshLayout swipe = new DirectionalSwipeRefreshLayout(fragment.requireContext());
        swipe.setColorSchemeResources(R.color.astro_primary_purple);
        parent.addView(swipe, index, params);
        swipe.addView(content, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        swipe.setOnChildScrollUpCallback((layout, child) -> scrollable.canScrollVertically(-1));
        swipe.setOnRefreshListener(() -> {
            swipe.setRefreshing(false);
            reload.run();
        });
    }
}
