package com.ebank.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DISPLAY_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    public static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DISPLAY_DATE_FORMATTER) : "";
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }

    public static LocalDate getStartOfFinancialYear(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        int year = d.getMonthValue() >= 4 ? d.getYear() : d.getYear() - 1;
        return LocalDate.of(year, 4, 1);
    }

    public static LocalDate getEndOfFinancialYear(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        int year = d.getMonthValue() >= 4 ? d.getYear() + 1 : d.getYear();
        return LocalDate.of(year, 3, 31);
    }

    public static boolean isOverdue(LocalDate dueDate) {
        return dueDate != null && dueDate.isBefore(LocalDate.now());
    }
}
