package com.queuecare.queuecare.service;

import com.queuecare.queuecare.dto.AppointmentRequest;
import com.queuecare.queuecare.dto.AppointmentResponse;
import com.queuecare.queuecare.exception.ConflictException;
import com.queuecare.queuecare.exception.ForbiddenException;
import com.queuecare.queuecare.exception.NotFoundException;
import com.queuecare.queuecare.model.Appointment;
import com.queuecare.queuecare.model.User;
import com.queuecare.queuecare.repository.AppointmentRepository;
import com.queuecare.queuecare.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    public AppointmentService(AppointmentRepository appointmentRepository, UserRepository userRepository) {
        this.appointmentRepository = appointmentRepository;
        this.userRepository = userRepository;
    }

    public AppointmentResponse create(Long userId, AppointmentRequest request) {
        User patient = getUser(userId);

        List<Appointment> existing = appointmentRepository.findByPatientAndAppointmentDate(patient, request.getAppointmentDate());
        boolean hasActive = existing.stream().anyMatch(a -> a.getStatus() != Appointment.Status.CANCELED);
        if (hasActive) {
            throw new ConflictException("You already have an appointment on this date");
        }

        int queueNumber = appointmentRepository.findByAppointmentDate(request.getAppointmentDate()).size() + 1;

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(request.getDoctor())
                .appointmentDate(request.getAppointmentDate())
                .reason(request.getReason())
                .queueNumber(queueNumber)
                .build();

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    public List<AppointmentResponse> getAll(Long userId) {
        User user = getUser(userId);
        List<Appointment> appointments = isPrivileged(user)
                ? appointmentRepository.findAll()
                : appointmentRepository.findByPatient(user);
        return appointments.stream().map(AppointmentResponse::from).toList();
    }

    public AppointmentResponse getById(Long userId, Long appointmentId) {
        User user = getUser(userId);
        Appointment appointment = getAppointment(appointmentId);
        checkAccess(user, appointment);
        return AppointmentResponse.from(appointment);
    }

    public AppointmentResponse update(Long userId, Long appointmentId, AppointmentRequest request) {
        User user = getUser(userId);
        Appointment appointment = getAppointment(appointmentId);
        checkAccess(user, appointment);

        if (appointment.getStatus() == Appointment.Status.CANCELED) {
            throw new ConflictException("Cannot update a cancelled appointment");
        }

        appointment.setDoctor(request.getDoctor());
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setReason(request.getReason());

        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    public AppointmentResponse cancel(Long userId, Long appointmentId) {
        User user = getUser(userId);
        Appointment appointment = getAppointment(appointmentId);
        checkAccess(user, appointment);

        if (appointment.getStatus() == Appointment.Status.CANCELED) {
            throw new ConflictException("Appointment is already cancelled");
        }

        appointment.setStatus(Appointment.Status.CANCELED);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }


    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Appointment getAppointment(Long appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new NotFoundException("Appointment not found"));
    }

    private boolean isPrivileged(User user) {
        return user.getRole() == User.Role.STAFF || user.getRole() == User.Role.ADMIN;
    }

    private void checkAccess(User user, Appointment appointment) {
        if (!isPrivileged(user) && !appointment.getPatient().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }
}
