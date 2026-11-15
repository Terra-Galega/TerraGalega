package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entities.Notification;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;

// Notificaciones del cliente con sesión. Ruta: /notifications
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    // GET /notifications/me?platform=web|mobile
    @Operation(summary = "Get the notifications of the logged-in client")
    @GetMapping("/me")
    public List<Notification> getMine(@RequestParam(defaultValue = "web") String platform) {
        return notificationService.getMine(authenticatedUser.client().getId(), platform.equals("mobile"));
    }

    // PUT /notifications/{id}/read
    @Operation(summary = "Mark one notification as read")
    @PutMapping("/{id}/read")
    public void markRead(@PathVariable Integer id) {
        notificationService.markRead(authenticatedUser.client().getId(), id);
    }

    // PUT /notifications/me/read-all
    @Operation(summary = "Mark all notifications as read")
    @PutMapping("/me/read-all")
    public void markAllRead() {
        notificationService.markAllRead(authenticatedUser.client().getId());
    }

    // DELETE /notifications/me
    @Operation(summary = "Delete all notifications of the logged-in client")
    @DeleteMapping("/me")
    public void deleteAll() {
        notificationService.deleteAll(authenticatedUser.client().getId());
    }
}