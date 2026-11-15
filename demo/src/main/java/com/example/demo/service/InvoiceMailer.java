package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class InvoiceMailer {

    private static final Logger log = LoggerFactory.getLogger(InvoiceMailer.class);

    public record OrderPaidEvent(InvoiceService.InvoiceData invoice) {
    }

    @Autowired
    private EmailService emailService;

    @Autowired
    private InvoiceService invoices;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaid(OrderPaidEvent event) {
        InvoiceService.InvoiceData d = event.invoice();
        try {
            emailService.sendInvoiceEmail(d, invoices.electronicPdf(d));
        } catch (Exception e) {
            log.error("No se pudo enviar la factura {} a {}: {}", d.number(), d.clientEmail(), e.getMessage());
        }
    }
}