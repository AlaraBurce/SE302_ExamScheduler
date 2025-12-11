package org.example.se302_examscheduler;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public class ExamSlot {
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;

    public ExamSlot(LocalDate date, LocalTime startTime, LocalTime endTime) {
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    @Override
    public String toString() {
        return date + " " + startTime + "-" + endTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExamSlot)) return false;
        ExamSlot examSlot = (ExamSlot) o;
        return Objects.equals(date, examSlot.date)
                && Objects.equals(startTime, examSlot.startTime)
                && Objects.equals(endTime, examSlot.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, startTime, endTime);
    }
}

