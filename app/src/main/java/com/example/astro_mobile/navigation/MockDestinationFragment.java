package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
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
