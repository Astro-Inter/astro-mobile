package com.example.astro_mobile.navigation;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.astro_mobile.R;
import com.example.astro_mobile.auth.AuthArgs;
import com.example.astro_mobile.auth.AccessKeyViewModel;
import com.example.astro_mobile.data.api.FailureKind;

public class FirstLoginAccessKeyFragment extends Fragment {

    private EditText[] digitInputs;
    private TextView errorText;
    private AccessKeyViewModel viewModel;

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
        viewModel = new ViewModelProvider(requireActivity(), new AccessKeyViewModel.Factory())
                .get(AccessKeyViewModel.class);
        Bundle args = getArguments();
        viewModel.setContext(args == null ? null : args.getString(AuthArgs.EMAIL),
                args == null ? null : args.getString(AuthArgs.USER_TYPE));

        configureDigitInputs();
        String previousKey = viewModel.getLastKey();
        if (previousKey != null && previousKey.length() == digitInputs.length) {
            for (int index = 0; index < digitInputs.length; index++) {
                digitInputs[index].setText(String.valueOf(previousKey.charAt(index)));
            }
        }
        String pendingError = viewModel.consumeInlineError();
        if (pendingError != null) {
            showError(pendingError);
        }

        // Uma chave incompleta não chega à API; seis dígitos são validados pelo backend.
        View continueButton = view.findViewById(R.id.button_first_login_access_key_continue);
        TextView continueLabel = view.findViewById(R.id.text_first_login_access_key_continue);
        ImageView continueIcon = view.findViewById(R.id.image_first_login_access_key_continue);
        ProgressBar progress = view.findViewById(R.id.progress_first_login_access_key);
        continueButton.setOnClickListener(clickedView -> {
            if (viewModel.isLoading()) {
                return;
            }
            StringBuilder accessKey = new StringBuilder(digitInputs.length);
            for (EditText input : digitInputs) {
                accessKey.append(input.getText());
            }
            if (!accessKey.toString().matches("[0-9]{6}")) {
                showError(getString(R.string.first_login_access_key_incomplete_error));
                return;
            }
            if (viewModel.getEmail() == null || viewModel.getEmail().isEmpty()) {
                NavHostFragment.findNavController(this)
                        .popBackStack(R.id.emailIdentificationFragment, false);
                return;
            }
            hideErrorState();
            setLoading(continueButton, continueLabel, continueIcon, progress, true);
            viewModel.verifyKey(accessKey.toString(), (failure, message) -> {
                if (getView() != view || !isAdded()) {
                    return;
                }
                setLoading(continueButton, continueLabel, continueIcon, progress, false);
                if (failure == null) {
                    Bundle nextArgs = new Bundle();
                    nextArgs.putString(AuthArgs.EMAIL, viewModel.getEmail());
                    NavHostFragment.findNavController(this).navigate(
                            R.id.action_first_login_access_key_to_first_login_password, nextArgs);
                } else if (failure == FailureKind.BUSINESS) {
                    showError(message != null ? message
                            : getString(R.string.first_login_access_key_error));
                } else {
                    NavHostFragment.findNavController(this).navigate(
                            failure == FailureKind.CONNECTION
                                    ? R.id.action_first_login_access_key_to_connection_error
                                    : R.id.action_first_login_access_key_to_generic_error);
                }
            });
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

        // Quem já criou a conta autentica sua senha, sem repetir a chave consumida.
        view.findViewById(R.id.button_first_login_access_key_resume)
                .setOnClickListener(clickedView -> {
                    if (viewModel.isLoading()) {
                        return;
                    }
                    Bundle nextArgs = new Bundle();
                    nextArgs.putString(AuthArgs.EMAIL, viewModel.getEmail());
                    nextArgs.putBoolean(AuthArgs.RESUME_ACTIVATION, true);
                    NavHostFragment.findNavController(this).navigate(
                            R.id.action_first_login_access_key_to_first_login_password, nextArgs);
                });
    }

    private void configureDigitInputs() {
        // Avança o foco ao digitar e volta ao campo anterior ao apagar.
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

    private void showError(String message) {
        // Destaca os seis campos e foca o primeiro que ainda está vazio.
        for (EditText input : digitInputs) {
            input.setBackgroundResource(R.drawable.bg_access_key_digit_error);
        }
        errorText.setText(message);
        errorText.setVisibility(View.VISIBLE);

        for (EditText input : digitInputs) {
            if (input.getText().length() == 0) {
                input.requestFocus();
                break;
            }
        }
    }

    private void setLoading(View button, TextView label, ImageView icon,
                            ProgressBar progress, boolean loading) {
        // Desabilita os campos e o botão para não enviar duas chaves ao mesmo tempo.
        button.setEnabled(!loading);
        label.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        icon.setVisibility(loading ? View.INVISIBLE : View.VISIBLE);
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        for (EditText input : digitInputs) {
            input.setEnabled(!loading);
        }
    }

    private void hideErrorState() {
        // Retira o aviso assim que o usuário altera algum dígito.
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
        if (viewModel != null) {
            viewModel.cancelCurrentRequest();
        }
        // Solta as referências aos campos quando a interface é destruída.
        digitInputs = null;
        errorText = null;
        super.onDestroyView();
    }
}
