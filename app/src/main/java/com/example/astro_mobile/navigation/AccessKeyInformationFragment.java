package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;

public class AccessKeyInformationFragment extends Fragment {

    public AccessKeyInformationFragment() {
        super(R.layout.fragment_access_key_information);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View.OnClickListener navigateBack = clickedView -> getNavController().navigateUp();
        view.findViewById(R.id.button_access_key_information_back).setOnClickListener(navigateBack);
        view.findViewById(R.id.button_access_key_information_acknowledge)
                .setOnClickListener(navigateBack);
    }

    private NavController getNavController() {
        return NavHostFragment.findNavController(this);
    }
}
