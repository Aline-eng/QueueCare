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

import java.time.LocalDate;
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

        if (hasActiveAppointmentOnDate(patient, request.getAppointmentDate(), null)) {
            throw new ConflictException("You already have an appointment on this date");
        }

        Appointment appointment = Appointment.builder()
                .patient(patient)
                .doctor(request.getDoctor())
                .appointmentDate(request.getAppointmentDate())
                .reason(request.getReason())
                .queueNumber(nextQueueNumber(request.getAppointmentDate()))
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

        if (hasActiveAppointmentOnDate(appointment.getPatient(), request.getAppointmentDate(), appointment.getId())) {
            throw new ConflictException("You already have an appointment on this date");
        }

        if (!appointment.getAppointmentDate().equals(request.getAppointmentDate())) {
            appointment.setQueueNumber(nextQueueNumber(request.getAppointmentDate()));
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

    // TODAY'S QUEUE
    public List<AppointmentResponse> getTodayQueue(Long userId) {
        User user = getUser(userId);
        if (!isPrivileged(user)) {
            throw new ForbiddenException("Only staff or admin can view today's queue");
        }
        return appointmentRepository
                .findByAppointmentDateOrderByQueueNumberAsc(LocalDate.now())
                .stream()
                .filter(appointment -> appointment.getStatus() != Appointment.Status.CANCELED)
                .map(AppointmentResponse::from).toList();
    }

    // MARK AS SERVED
    public AppointmentResponse markServed(Long userId, Long appointmentId) {
        User user = getUser(userId);
        if (!isPrivileged(user)) {
            throw new ForbiddenException("Only staff or admin can mark patients as served");
        }
        Appointment appointment = getAppointment(appointmentId);
        if (appointment.getStatus() == Appointment.Status.COMPLETED) {
            throw new ConflictException("Patient is already marked as served");
        }
        if (appointment.getStatus() == Appointment.Status.CANCELED) {
            throw new ConflictException("Cannot serve a cancelled appointment");
        }
        appointment.setStatus(Appointment.Status.COMPLETED);
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

    private boolean hasActiveAppointmentOnDate(User patient, LocalDate appointmentDate, Long excludedAppointmentId) {
        return appointmentRepository.findByPatientAndAppointmentDate(patient, appointmentDate).stream()
                .filter(appointment -> excludedAppointmentId == null || !appointment.getId().equals(excludedAppointmentId))
                .anyMatch(appointment -> appointment.getStatus() != Appointment.Status.CANCELED);
    }

    private int nextQueueNumber(LocalDate appointmentDate) {
        return (int) appointmentRepository.findByAppointmentDate(appointmentDate).stream()
                .filter(appointment -> appointment.getStatus() != Appointment.Status.CANCELED)
                .count() + 1;
    }

    private void checkAccess(User user, Appointment appointment) {
        if (!isPrivileged(user) && !appointment.getPatient().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }
}
