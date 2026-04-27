package com.queuecare.queuecare.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AppointmentRequest {

    @NotBlank
    private String doctor;

    @NotNull
    @Future(message = "Appointment date must be in the future")
    private LocalDate appointmentDate;

    @NotBlank
    private String reason;
}
