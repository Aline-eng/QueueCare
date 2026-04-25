package com.queuecare.queuecare.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.queuecare.queuecare.model.Appointment;
import com.queuecare.queuecare.model.User;

public interface AppointmentRepository extends JpaRepository<Appointment, Long>{

    List<Appointment> findByPatient(User patient);
    List<Appointment> findByAppointmentDate(LocalDate date);
    List<Appointment> findByPatientAndAppointmentDate(User patient, LocalDate date);
    
}
