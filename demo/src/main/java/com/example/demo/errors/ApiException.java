package com.example.demo.errors;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    // Código legible por el front para distinguir errores con el mismo estado HTTP
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";

    private final HttpStatus status;
    private final String code;

    // Evalua que tipo de error es
    private ApiException(HttpStatus status, String message) {
        this(status, message, null);
    }

    private ApiException(HttpStatus status, String message, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    // 403 con código: la contraseña es correcta pero el correo aún no se confirmó
    public static ApiException emailNotVerified(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message, EMAIL_NOT_VERIFIED);
    }

    // 400: los datos enviados no son válidos o están incompletos
    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }

    // 401: no se pudo identificar a la persona (credenciales incorrectas)
    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, message);
    }

    // 403: la persona está identificada pero no puede hacer esta acción
    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message);
    }

    // 404: el recurso pedido no existe
    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    // 409: la acción choca con el estado actual (duplicados, estados de pedido,
    // etc.)
    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    public static ApiException serviceUnavailable(String message) {
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}