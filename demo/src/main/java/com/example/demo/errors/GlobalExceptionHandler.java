package com.example.demo.errors;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
 
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
 
    // Errores de negocio lanzados por los servicios: traen su propio código
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException e, WebRequest request) {
        return build(e.getStatus(), e.getMessage(), request);
    }
 
    // 404: el producto ya no existe o no está disponible
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ApiError> handleProductNotFound(ProductNotFoundException e, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }
 
    // 409: además del mensaje, se devuelven los productos que lo impiden
    @ExceptionHandler(CategoryHasProductsException.class)
    public ResponseEntity<ApiError> handleCategoryHasProducts(CategoryHasProductsException e, WebRequest request) {
        ApiError body = ApiError.of(HttpStatus.CONFLICT, e.getMessage(), path(request))
                .withProducts(e.getProducts());
 
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
 
    // 409: la base de datos rechazó el cambio (llave única, llave foránea, etc.)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException e, WebRequest request) {
        log.warn("Violación de integridad en {}: {}", path(request), e.getMostSpecificCause().getMessage());
 
        return build(HttpStatus.CONFLICT,
                "No se pudo guardar porque los datos entran en conflicto con otros registros.", request);
    }
 
    // Red de seguridad: si algún código lanza IllegalArgumentException, se
    // responde 400 sin mostrar el mensaje técnico al usuario
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException e, WebRequest request) {
        log.warn("Argumento inválido en {}: {}", path(request), e.getMessage());
 
        return build(HttpStatus.BAD_REQUEST, defaultMessage(HttpStatus.BAD_REQUEST), request);
    }
 
    // 500: cualquier error no previsto. El detalle queda en el log, no en la respuesta
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception e, WebRequest request) {
        log.error("Error inesperado en {}", path(request), e);
 
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                defaultMessage(HttpStatus.INTERNAL_SERVER_ERROR), request);
    }
 
    // Errores que maneja Spring por su cuenta: se les da el mismo cuerpo JSON
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
 
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        ApiError error = ApiError.of(status, defaultMessage(status), path(request));
 
        return ResponseEntity.status(status).headers(headers).body((Object) error);
    }
 
    private ResponseEntity<ApiError> build(HttpStatus status, String message, WebRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(status, message, path(request)));
    }
 
    // WebRequest describe la ruta como "uri=/products/3"
    private String path(WebRequest request) {
        return request.getDescription(false).replaceFirst("^uri=", "");
    }
 
    // Mensaje en español para los errores que no traen uno propio
    private String defaultMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "La solicitud no es válida. Revisa los datos enviados.";
            case NOT_FOUND -> "No se encontró el recurso solicitado.";
            case METHOD_NOT_ALLOWED -> "Este método no está permitido para este recurso.";
            case NOT_ACCEPTABLE, UNSUPPORTED_MEDIA_TYPE -> "El formato de contenido no es compatible.";
            default -> status.is5xxServerError()
                    ? "Ha ocurrido un error inesperado."
                    : "No se pudo procesar la solicitud.";
        };
    }
}