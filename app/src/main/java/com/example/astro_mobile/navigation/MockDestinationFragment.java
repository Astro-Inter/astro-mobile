package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.graphics.Color;
import android.text.Editable;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.astro_mobile.R;

public class MockDestinationFragment extends Fragment {

    public MockDestinationFragment() {
        super(R.layout.fragment_mock_destination);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        EditText emailInput = view.findViewById(R.id.input_email_identification);
        View emailInputContainer = view.findViewById(R.id.container_email_identification_input);
        TextView emailError = view.findViewById(R.id.text_email_identification_error);
        View continueButton = view.findViewById(R.id.button_email_identification_continue);
        continueButton.setOnClickListener(clickedView -> {
            String mockEmail = emailInput.getText().toString().trim();
            if ("1".equals(mockEmail)) {
                Navigation.findNavController(clickedView)
                        .navigate(R.id.action_mock_destination_to_first_login_access_key);
            } else if ("2".equals(mockEmail)) {
                Navigation.findNavController(clickedView)
                        .navigate(R.id.action_mock_destination_to_login_password);
            } else {
                emailInputContainer.setBackgroundResource(R.drawable.bg_login_password_input_error);
                emailError.setVisibility(View.VISIBLE);
            }
        });

        emailInput.setOnEditorActionListener((textView, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_GO
                    && actionId != EditorInfo.IME_ACTION_DONE
                    && actionId != EditorInfo.IME_ACTION_NEXT) {
                return false;
            }
            continueButton.performClick();
            return true;
        });
        emailInput.addTextChangedListener(new TextWatcher() {
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
                if (emailError.getVisibility() == View.VISIBLE) {
                    emailInputContainer.setBackgroundResource(R.drawable.bg_email_identification_input);
                    emailError.setVisibility(View.GONE);
                }
            }
        });

        TextView help = view.findViewById(R.id.text_email_identification_help);
        String helpText = help.getText().toString();
        String linkText = "Saiba mais";
        int linkStart = helpText.indexOf(linkText);
        if (linkStart < 0) {
            return;
        }

        SpannableString clickableText = new SpannableString(help.getText());
        clickableText.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                Navigation.findNavController(widget)
                        .navigate(R.id.action_mock_destination_to_access_key_information);
            }

            @Override
            public void updateDrawState(@NonNull TextPaint drawState) {
                drawState.setColor(Color.rgb(0, 153, 255));
                drawState.setUnderlineText(true);
            }
        }, linkStart, linkStart + linkText.length(),
                SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        help.setText(clickableText);
        help.setMovementMethod(LinkMovementMethod.getInstance());
        help.setHighlightColor(ContextCompat.getColor(requireContext(), R.color.access_key_help_ripple));
    }
}
