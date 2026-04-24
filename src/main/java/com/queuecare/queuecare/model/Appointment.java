package com.queuecare.queuecare.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @Column(nullable = false)
    private String doctor;

    @Column(nullable = false)
    private LocalDate appointmentDate;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private Integer queueNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public enum Status {
        SCHEDULED,
        COMPLETED,
        CANCELED
    }

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = Status.SCHEDULED;
        }
    }
}