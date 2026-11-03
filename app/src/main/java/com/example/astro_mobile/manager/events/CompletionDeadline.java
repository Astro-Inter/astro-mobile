package com.example.astro_mobile.manager.events;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/** Compara datas da turma com o horário local informado pela API. */
public final class CompletionDeadline {
    public enum Result { ALLOWED, EXPIRED, UNAVAILABLE }

    private CompletionDeadline() { }

    public static Result assignment(String endDate, LocalDateTime now) {
        LocalDateTime end = parse(endDate);
        if (end == null || now == null) return Result.UNAVAILABLE;
        return now.isAfter(end) ? Result.EXPIRED : Result.ALLOWED;
    }

    public static Result validation(String endDate, String completionDate) {
        LocalDateTime end = parse(endDate);
        LocalDateTime submitted = parse(completionDate);
        if (end == null || submitted == null) return Result.UNAVAILABLE;
        return submitted.isAfter(end) ? Result.EXPIRED : Result.ALLOWED;
    }

    private static LocalDateTime parse(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try { return LocalDateTime.parse(value); }
        catch (DateTimeParseException ignored) { return null; }
    }
}
