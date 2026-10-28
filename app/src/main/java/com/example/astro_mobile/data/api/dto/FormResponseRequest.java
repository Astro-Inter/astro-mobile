package com.example.astro_mobile.data.api.dto;

import java.util.List;

public final class FormResponseRequest {
    public final List<Answer> answers;
    public FormResponseRequest(List<Answer> answers) { this.answers = answers; }

    public static final class Answer {
        public final Long questionId;
        public String text, otherText, date;
        public List<Long> optionIds;
        public Answer(Long questionId) { this.questionId = questionId; }
    }
}
