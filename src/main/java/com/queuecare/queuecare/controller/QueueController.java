package com.queuecare.queuecare.controller;

import com.queuecare.queuecare.dto.AppointmentResponse;
import com.queuecare.queuecare.service.AppointmentService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/queue")
public class QueueController {

    private final AppointmentService appointmentService;

    public QueueController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @GetMapping("/today")
    public List<AppointmentResponse> getTodayQueue() {
        return appointmentService.getTodayQueue();
    }

    @PatchMapping("/{id}/serve")
    public AppointmentResponse markServed(@PathVariable Long id, Authentication auth) {
        return appointmentService.markServed(getUserId(auth), id);
    }

    private Long getUserId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }
}
