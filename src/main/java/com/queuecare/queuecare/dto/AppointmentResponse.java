package com.queuecare.queuecare.dto;

import com.queuecare.queuecare.model.Appointment;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AppointmentResponse {

    private Long id;
    private Long patientId;
    private String patientName;
    private String doctor;
    private LocalDate appointmentDate;
    private String reason;
    private Integer queueNumber;
    private String status;
    private LocalDateTime createdAt;

    public static AppointmentResponse from(Appointment a) {
        AppointmentResponse r = new AppointmentResponse();
        r.id = a.getId();
        r.patientId = a.getPatient().getId();
        r.patientName = a.getPatient().getName();
        r.doctor = a.getDoctor();
        r.appointmentDate = a.getAppointmentDate();
        r.reason = a.getReason();
        r.queueNumber = a.getQueueNumber();
        r.status = a.getStatus().name();
        r.createdAt = a.getCreatedAt();
        return r;
    }
}
