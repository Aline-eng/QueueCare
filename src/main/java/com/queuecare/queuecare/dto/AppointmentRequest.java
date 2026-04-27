package com.queuecare.queuecare.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AppointmentRequest {

    @NotBlank
    private String doctor;

    @NotNull
    @FutureOrPresent(message = "Appointment date must be today or in the future")
    private LocalDate appointmentDate;

    @NotBlank
    private String reason;
}
