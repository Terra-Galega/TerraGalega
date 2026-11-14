package com.example.demo.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;


//Cuerpo JSON único para todas las respuestas de error.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        String timestamp,
        List<String> products,
        String code) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return of(status, message, path, null);
    }

    // Con un código (ej. EMAIL_NOT_VERIFIED) que el front puede revisar
    public static ApiError of(HttpStatus status, String message, String path, String code) {
        return new ApiError(status.value(), status.getReasonPhrase(), message, path,
                Instant.now().toString(), null, code);
    }

    // Copia del error con la lista de productos que impiden eliminar la categoría
    public ApiError withProducts(List<String> products) {
        return new ApiError(status, error, message, path, timestamp, products, code);
    }
}