package com.example.astro_mobile.manager.forms.answer;

import android.app.DatePickerDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.astro_mobile.R;
import com.example.astro_mobile.data.camera.FormEvidenceRepository;
import com.example.astro_mobile.data.api.dto.FormResponseRequest;
import com.example.astro_mobile.data.api.dto.ReceivedFormDetails;
import com.google.gson.JsonElement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Cria os campos de resposta a partir das perguntas retornadas pela API. */
final class FormQuestionRenderer {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final LayoutInflater inflater;
    private final FormAnswerViewModel state;
    private final Runnable changed;
    private final Consumer<Long> capturePhoto;
    private final BiConsumer<Long, Uri> removePhoto;
    private final BiConsumer<Long, String> openImage;
    private final FormEvidenceRepository evidence;

    FormQuestionRenderer(LayoutInflater inflater, FormAnswerViewModel state, Runnable changed,
                         Consumer<Long> capturePhoto, BiConsumer<Long, Uri> removePhoto,
                         BiConsumer<Long, String> openImage, FormEvidenceRepository evidence) {
        this.inflater = inflater;
        this.state = state;
        this.changed = changed;
        this.capturePhoto = capturePhoto;
        this.removePhoto = removePhoto;
        this.openImage = openImage;
        this.evidence = evidence;
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
                renderPhotos(row, question);
                break;
            default:
                TextView unsupported = row.findViewById(R.id.text_form_question_notice);
                unsupported.setVisibility(View.VISIBLE);
                unsupported.setText(R.string.form_answer_unknown_question);
        }
        return row;
    }

    private void renderPhotos(View row, ReceivedFormDetails.Question question) {
        LinearLayout container = row.findViewById(R.id.container_form_question_photos);
        LinearLayout previews = row.findViewById(R.id.list_form_question_photos);
        container.setVisibility(View.VISIBLE);
        previews.removeAllViews();
        TextView notice = row.findViewById(R.id.text_form_question_notice);
        notice.setVisibility(View.VISIBLE);
        if (!state.isCompleted()) {
            int limit = question.maxFiles == null ? 1 : Math.min(10, Math.max(1, question.maxFiles));
            int maxSize = question.maxFileSizeMb == null ? 10
                    : Math.min(50, Math.max(1, question.maxFileSizeMb));
            notice.setText(row.getContext().getString(R.string.form_answer_photo_limits, limit, maxSize));
            for (Uri photo : state.photos(question.id)) addLocalPhoto(row, previews, question.id, photo);
            View button = container.findViewById(R.id.button_form_question_take_photo);
            button.setVisibility(View.VISIBLE);
            button.setEnabled(state.canAddPhoto(question));
            button.setOnClickListener(clicked -> capturePhoto.accept(question.id));
            return;
        }

        container.findViewById(R.id.button_form_question_take_photo).setVisibility(View.GONE);
        notice.setText(R.string.form_answer_read_only);
        if (question.answer == null) return;
        for (JsonElement image : question.answer) {
            if (!image.isJsonObject()) continue;
            String id = image.getAsJsonObject().has("id")
                    ? image.getAsJsonObject().get("id").getAsString() : null;
            String name = image.getAsJsonObject().has("name")
                    ? image.getAsJsonObject().get("name").getAsString() : "Imagem";
            if (id == null) continue;
            TextView item = new TextView(row.getContext());
            item.setId(R.id.form_answer_submitted_photo);
            item.setText(name);
            item.setTextColor(row.getResources().getColor(R.color.astro_light_background, null));
            item.setTextSize(14);
            item.setMinHeight((int) (48 * row.getResources().getDisplayMetrics().density));
            item.setGravity(android.view.Gravity.CENTER_VERTICAL);
            item.setBackgroundResource(R.drawable.bg_home_row_ripple);
            item.setPadding(12, 4, 12, 4);
            item.setOnClickListener(clicked -> openImage.accept(question.id, id));
            previews.addView(item, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private void addLocalPhoto(View row, LinearLayout previews, Long questionId, Uri photo) {
        LinearLayout item = new LinearLayout(row.getContext());
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(android.view.Gravity.CENTER);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                (int) (88 * row.getResources().getDisplayMetrics().density), -2);
        params.setMarginEnd((int) (10 * row.getResources().getDisplayMetrics().density));
        ImageView thumbnail = new ImageView(row.getContext());
        int size = (int) (76 * row.getResources().getDisplayMetrics().density);
        thumbnail.setLayoutParams(new LinearLayout.LayoutParams(size, size));
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try {
            Bitmap bitmap = evidence.preview(photo);
            thumbnail.setImageBitmap(bitmap);
        } catch (java.io.IOException error) {
            thumbnail.setContentDescription(row.getContext().getString(R.string.form_answer_photo_invalid));
        }
        item.addView(thumbnail);
        TextView remove = new TextView(row.getContext());
        remove.setText(R.string.form_answer_photo_remove);
        remove.setTextColor(row.getResources().getColor(R.color.astro_error, null));
        remove.setTextSize(12);
        remove.setGravity(android.view.Gravity.CENTER);
        remove.setMinHeight((int) (40 * row.getResources().getDisplayMetrics().density));
        remove.setOnClickListener(clicked -> {
            removePhoto.accept(questionId, photo);
            changed.run();
        });
        item.addView(remove);
        previews.addView(item, params);
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
