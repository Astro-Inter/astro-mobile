package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;

public class LoginPasswordFragment extends Fragment {

    public LoginPasswordFragment() {
        super(R.layout.fragment_login_password);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText passwordInput = view.findViewById(R.id.input_login_password);
        View inputContainer = view.findViewById(R.id.container_login_password_input);
        TextView error = view.findViewById(R.id.text_login_password_error);
        ImageButton visibilityButton = view.findViewById(R.id.button_login_password_visibility);
        View enterButton = view.findViewById(R.id.button_login_password_enter);

        visibilityButton.setOnClickListener(clickedView -> {
            boolean isPasswordHidden = passwordInput.getTransformationMethod()
                    instanceof PasswordTransformationMethod;
            passwordInput.setTransformationMethod(isPasswordHidden
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            visibilityButton.setContentDescription(getString(isPasswordHidden
                    ? R.string.login_password_hide_description
                    : R.string.login_password_show_description));
            visibilityButton.setImageResource(isPasswordHidden
                    ? R.drawable.ic_login_eye_off
                    : R.drawable.ic_login_eye);
            passwordInput.setSelection(passwordInput.length());
        });

        enterButton.setOnClickListener(clickedView -> {
            inputContainer.setBackgroundResource(R.drawable.bg_login_password_input_error);
            error.setVisibility(View.VISIBLE);
        });
        passwordInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_DONE) {
                return false;
            }
            enterButton.performClick();
            return true;
        });
        passwordInput.addTextChangedListener(new TextWatcher() {
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
                if (error.getVisibility() == View.VISIBLE) {
                    inputContainer.setBackgroundResource(R.drawable.bg_email_identification_input);
                    error.setVisibility(View.GONE);
                }
            }
        });

        view.findViewById(R.id.button_login_password_back)
                .setOnClickListener(clickedView ->
                        NavHostFragment.findNavController(this).navigateUp());
    }
}
