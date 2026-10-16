package com.example.astro_mobile.manager.forms.answer;

import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;

public final class FormOutsideLocationFragment extends Fragment {
    private final Runnable returnToDetails = () -> NavHostFragment.findNavController(this)
            .popBackStack(R.id.formDetailsFragment, false);

    public FormOutsideLocationFragment() { super(R.layout.fragment_form_outside_location); }

    @Override public void onResume() {
        super.onResume();
        // Mostra o aviso por 3s; voltar aos detalhes não apaga as respostas e fotos da tentativa.
        requireView().postDelayed(returnToDetails, 3_000L);
    }

    @Override public void onPause() {
        View view = getView();
        if (view != null) view.removeCallbacks(returnToDetails);
        super.onPause();
    }
}
