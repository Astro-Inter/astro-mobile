package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;

public class FirstLoginPasswordFragment extends Fragment {

    public FirstLoginPasswordFragment() {
        super(R.layout.fragment_first_login_password);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText passwordInput = view.findViewById(R.id.input_first_login_password);
        EditText confirmationInput = view.findViewById(R.id.input_first_login_password_confirmation);
        View passwordContainer = view.findViewById(R.id.container_first_login_password_input);
        View confirmationContainer = view.findViewById(R.id.container_first_login_password_confirmation);
        TextView mismatchError = view.findViewById(R.id.text_first_login_password_mismatch);
        View enterButton = view.findViewById(R.id.button_first_login_password_enter);

        enterButton.setOnClickListener(clickedView -> {
            String password = passwordInput.getText().toString();
            String confirmation = confirmationInput.getText().toString();
            if (password.isEmpty()) {
                passwordInput.requestFocus();
            } else if (confirmation.isEmpty()) {
                confirmationInput.requestFocus();
            } else if (!password.equals(confirmation)) {
                passwordContainer.setBackgroundResource(R.drawable.bg_login_password_input_error);
                confirmationContainer.setBackgroundResource(R.drawable.bg_login_password_input_error);
                mismatchError.setVisibility(View.VISIBLE);
            } else {
                Navigation.findNavController(clickedView)
                        .navigate(R.id.action_first_login_password_to_flow_choice);
            }
        });

        confirmationInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return false;
            }
            enterButton.performClick();
            return true;
        });

        TextWatcher clearMismatch = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                // No-op.
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                // No-op.
            }

            @Override
            public void afterTextChanged(Editable text) {
                if (mismatchError.getVisibility() == View.VISIBLE) {
                    passwordContainer.setBackgroundResource(R.drawable.bg_email_identification_input);
                    confirmationContainer.setBackgroundResource(R.drawable.bg_email_identification_input);
                    mismatchError.setVisibility(View.GONE);
                }
            }
        };
        passwordInput.addTextChangedListener(clearMismatch);
        confirmationInput.addTextChangedListener(clearMismatch);

        view.findViewById(R.id.button_first_login_password_back)
                .setOnClickListener(clickedView ->
                        NavHostFragment.findNavController(this).navigateUp());
    }
}
