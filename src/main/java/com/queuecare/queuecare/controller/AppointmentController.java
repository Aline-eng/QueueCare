package com.queuecare.queuecare.controller;

import com.queuecare.queuecare.dto.AppointmentRequest;
import com.queuecare.queuecare.dto.AppointmentResponse;
import com.queuecare.queuecare.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@Valid @RequestBody AppointmentRequest request, Authentication auth) {
        return appointmentService.create(getUserId(auth), request);
    }

    @GetMapping
    public List<AppointmentResponse> getAll(Authentication auth) {
        return appointmentService.getAll(getUserId(auth));
    }

    @GetMapping("/{id}")
    public AppointmentResponse getById(@PathVariable Long id, Authentication auth) {
        return appointmentService.getById(getUserId(auth), id);
    }

    @PutMapping("/{id}")
    public AppointmentResponse update(@PathVariable Long id,
                                      @Valid @RequestBody AppointmentRequest request,
                                      Authentication auth) {
        return appointmentService.update(getUserId(auth), id, request);
    }

    @DeleteMapping("/{id}")
    public AppointmentResponse cancel(@PathVariable Long id, Authentication auth) {
        return appointmentService.cancel(getUserId(auth), id);
    }

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
