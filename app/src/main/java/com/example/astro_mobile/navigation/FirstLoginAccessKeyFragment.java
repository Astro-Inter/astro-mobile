package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;

public class FirstLoginAccessKeyFragment extends Fragment {

    private EditText[] digitInputs;
    private TextView errorText;

    public FirstLoginAccessKeyFragment() {
        super(R.layout.fragment_first_login_access_key);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        digitInputs = new EditText[]{
                view.findViewById(R.id.input_access_key_digit_1),
                view.findViewById(R.id.input_access_key_digit_2),
                view.findViewById(R.id.input_access_key_digit_3),
                view.findViewById(R.id.input_access_key_digit_4),
                view.findViewById(R.id.input_access_key_digit_5),
                view.findViewById(R.id.input_access_key_digit_6)
        };
        errorText = view.findViewById(R.id.text_first_login_access_key_error);

        configureDigitInputs();

        View continueButton = view.findViewById(R.id.button_first_login_access_key_continue);
        continueButton.setOnClickListener(clickedView -> {
            StringBuilder accessKey = new StringBuilder(digitInputs.length);
            for (EditText input : digitInputs) {
                accessKey.append(input.getText());
            }
            if ("111111".contentEquals(accessKey)) {
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_first_login_access_key_to_first_login_password);
            } else {
                showInvalidKeyMock();
            }
        });
        digitInputs[digitInputs.length - 1].setOnEditorActionListener(
                (textView, actionId, event) -> {
                    if (actionId != EditorInfo.IME_ACTION_DONE) {
                        return false;
                    }
                    continueButton.performClick();
                    return true;
                }
        );

        view.findViewById(R.id.button_first_login_access_key_back)
                .setOnClickListener(clickedView ->
                        NavHostFragment.findNavController(this).navigateUp());
    }

    private void configureDigitInputs() {
        for (int index = 0; index < digitInputs.length; index++) {
            EditText input = digitInputs[index];
            int position = index;
            input.setContentDescription(getString(
                    R.string.first_login_access_key_digit_description,
                    position + 1
            ));
            input.addTextChangedListener(new TextWatcher() {
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
                    hideErrorState();
                    if (text.length() == 1 && position < digitInputs.length - 1) {
                        digitInputs[position + 1].requestFocus();
                    }
                }
            });
            input.setOnKeyListener((view, keyCode, event) -> {
                boolean shouldMoveBack = keyCode == KeyEvent.KEYCODE_DEL
                        && event.getAction() == KeyEvent.ACTION_DOWN
                        && input.getText().length() == 0
                        && position > 0;
                if (!shouldMoveBack) {
                    return false;
                }
                EditText previousInput = digitInputs[position - 1];
                previousInput.requestFocus();
                previousInput.setText("");
                return true;
            });
        }
    }

    private void showInvalidKeyMock() {
        for (EditText input : digitInputs) {
            input.setBackgroundResource(R.drawable.bg_access_key_digit_error);
        }
        errorText.setVisibility(View.VISIBLE);

        for (EditText input : digitInputs) {
            if (input.getText().length() == 0) {
                input.requestFocus();
                break;
            }
        }
    }

    private void hideErrorState() {
        if (errorText.getVisibility() != View.VISIBLE) {
            return;
        }
        for (EditText input : digitInputs) {
            input.setBackgroundResource(R.drawable.bg_access_key_digit);
        }
        errorText.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        digitInputs = null;
        errorText = null;
        super.onDestroyView();
    }
}
