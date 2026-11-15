package com.example.demo.controller;

import java.util.Base64;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.errors.ApiException;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.InvoiceService;

@RestController
@RequestMapping("/invoices")
public class InvoiceController {

    // previewUrl: enlace firmado al ticket; qrPngBase64: el mismo enlace como
    // imagen QR
    public record InvoiceLinkDTO(String previewUrl, String qrPngBase64) {
    }

    private final InvoiceService invoices;
    private final AuthenticatedUser auth;

    public InvoiceController(InvoiceService invoices, AuthenticatedUser auth) {
        this.invoices = invoices;
        this.auth = auth;
    }

    // Web y móvil (con sesión): datos para mostrar el QR / abrir la factura
    @GetMapping("/{orderId}/link")
    public InvoiceLinkDTO link(@PathVariable Integer orderId) {
        InvoiceService.InvoiceData d = invoices.snapshotById(orderId);
        if (!d.clientId().equals(auth.client().getId())) {
            throw ApiException.forbidden("Esta factura no te pertenece.");
        }
        String url = invoices.previewUrl(orderId);
        return new InvoiceLinkDTO(url, Base64.getEncoder().encodeToString(invoices.qrPng(url)));
    }

    // Lo que abre el QR en el celular: sin sesión, protegido por la firma "t"
    @GetMapping("/public/{orderId}")
    public ResponseEntity<byte[]> preview(@PathVariable Integer orderId, @RequestParam("t") String token) {
        if (!invoices.validToken(orderId, token)) {
            throw ApiException.notFound("Factura inexistente.");
        }
        InvoiceService.InvoiceData d = invoices.snapshotById(orderId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("factura-" + d.number() + ".pdf").build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(invoices.physicalPdf(d));
    }
}