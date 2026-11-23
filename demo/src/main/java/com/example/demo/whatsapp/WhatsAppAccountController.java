package com.example.demo.whatsapp;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.security.AuthenticatedUser;

@RestController
@RequestMapping("/clients/me/whatsapp")
public class WhatsAppAccountController {
    private final AuthenticatedUser user;
    private final WhatsAppAccountService accounts;

    public WhatsAppAccountController(AuthenticatedUser user, WhatsAppAccountService accounts) {
        this.user = user;
        this.accounts = accounts;
    }

    public record TokenRequest(String token) { }

    @GetMapping
    public ResponseEntity<WhatsAppAccountService.Status> status() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(accounts.status(user.client().getId()));
    }

    // POST para que el secreto no aparezca en URLs ni en registros de acceso.
    @PostMapping("/preview")
    public ResponseEntity<WhatsAppAccountService.Preview> preview(@RequestBody TokenRequest request) {
        user.client();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(accounts.preview(request.token()));
    }

    @PostMapping("/confirm")
    public ResponseEntity<WhatsAppAccountService.Status> confirm(@RequestBody TokenRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(accounts.confirm(user.client().getId(), request.token()));
    }

    @DeleteMapping
    public ResponseEntity<Void> unlink() {
        accounts.unlink(user.client().getId());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
