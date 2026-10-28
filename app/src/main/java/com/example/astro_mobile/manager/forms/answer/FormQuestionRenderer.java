package com.example.astro_mobile.manager.forms.answer;

import android.app.DatePickerDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.astro_mobile.R;
import com.example.astro_mobile.data.api.dto.FormResponseRequest;
import com.example.astro_mobile.data.api.dto.ReceivedFormDetails;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/** Cria os campos de resposta a partir das perguntas retornadas pela API. */
final class FormQuestionRenderer {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final LayoutInflater inflater;
    private final FormAnswerViewModel state;
    private final Runnable changed;

    FormQuestionRenderer(LayoutInflater inflater, FormAnswerViewModel state, Runnable changed) {
        this.inflater = inflater;
        this.state = state;
        this.changed = changed;
    }

    View create(ViewGroup parent, ReceivedFormDetails.Question question, int index) {
        View row = inflater.inflate(R.layout.item_form_answer_question, parent, false);
        ((TextView) row.findViewById(R.id.text_form_question_label)).setText(row.getContext().getString(
                R.string.form_answer_question_label, index + 1, question.question,
                Boolean.TRUE.equals(question.required) ? " *" : ""));
        FormResponseRequest.Answer answer = state.answer(question.id);
        switch (question.type) {
            case "texto":
                input(row.findViewById(R.id.input_form_question_text), answer.text, 10_000,
                        value -> answer.text = value);
                break;
            case "alternativa":
                renderOptions(row, question, answer);
                break;
            case "data":
                renderDate(row, answer);
                break;
            case "foto":
                TextView photoNotice = row.findViewById(R.id.text_form_question_notice);
                photoNotice.setVisibility(View.VISIBLE);
                photoNotice.setText(R.string.form_answer_photo_pending);
                break;
            default:
                TextView unsupported = row.findViewById(R.id.text_form_question_notice);
                unsupported.setVisibility(View.VISIBLE);
                unsupported.setText(R.string.form_answer_unknown_question);
        }
        return row;
    }

    private void input(EditText field, String initial, int maxLength, Consumer<String> update) {
        field.setVisibility(View.VISIBLE);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
        field.setText(initial);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable text) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                update.accept(text.toString());
                changed.run();
            }
        });
    }

    private void renderOptions(View row, ReceivedFormDetails.Question question,
                               FormResponseRequest.Answer answer) {
        boolean multiple = Boolean.TRUE.equals(question.multiple);
        ViewGroup options = row.findViewById(multiple
                ? R.id.list_form_question_options : R.id.group_form_question_options);
        options.setVisibility(View.VISIBLE);
        EditText other = row.findViewById(R.id.input_form_question_other);
        input(other, answer.otherText, 1_000, value -> answer.otherText = value);
        other.setVisibility(state.selectedOther(question) ? View.VISIBLE : View.GONE);
        if (question.options == null) return;

        for (ReceivedFormDetails.Option option : question.options) {
            CompoundButton button = (CompoundButton) inflater.inflate(multiple
                    ? R.layout.view_form_dynamic_checkbox : R.layout.view_form_dynamic_radio, options, false);
            button.setId(View.generateViewId());
            button.setText(option.text == null || option.text.isEmpty()
                    ? row.getContext().getString(R.string.form_answer_other_label) : option.text);
            button.setTag(option.id);
            options.addView(button);
            button.setChecked(answer.optionIds.contains(option.id));
            if (multiple) button.setOnCheckedChangeListener((view, checked) -> {
                state.select(question, option.id, checked);
                other.setVisibility(state.selectedOther(question) ? View.VISIBLE : View.GONE);
                changed.run();
            });
        }
        if (!multiple) ((RadioGroup) options).setOnCheckedChangeListener((group, selectedId) -> {
            CompoundButton selected = group.findViewById(selectedId);
            if (selected == null) return;
            state.select(question, (Long) selected.getTag(), true);
            other.setVisibility(state.selectedOther(question) ? View.VISIBLE : View.GONE);
            changed.run();
        });
    }

    private void renderDate(View row, FormResponseRequest.Answer answer) {
        row.findViewById(R.id.container_form_question_date).setVisibility(View.VISIBLE);
        TextView date = row.findViewById(R.id.button_form_question_date);
        if (answer.date != null) date.setText(LocalDate.parse(answer.date).format(DATE));
        date.setOnClickListener(clicked -> {
            LocalDate current = answer.date == null ? LocalDate.now() : LocalDate.parse(answer.date);
            new DatePickerDialog(row.getContext(), (picker, year, month, day) -> {
                LocalDate selected = LocalDate.of(year, month + 1, day);
                answer.date = selected.toString();
                date.setText(selected.format(DATE));
                changed.run();
            }, current.getYear(), current.getMonthValue() - 1, current.getDayOfMonth()).show();
        });
    }
}
