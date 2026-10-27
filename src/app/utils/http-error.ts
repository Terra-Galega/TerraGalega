import { HttpErrorResponse } from '@angular/common/http';

// Mensaje cuando no hay respuesta del servidor (apagado o sin internet)
const NETWORK_ERROR_MESSAGE =
    'No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo.';

// Saca el texto que se le muestra al usuario a partir de un error HTTP.
export function getErrorMessage(
    error: any,
    fallback = 'Ocurrió un error inesperado.',
): string {
    // Estado 0: la petición nunca llegó al servidor
    if (error?.status === 0) return NETWORK_ERROR_MESSAGE;

    const body = error?.error;
    if (typeof body === 'string' && body.trim()) return body;
    if (body?.message) return body.message;

    // El mensaje de un HttpErrorResponse es técnico
    if (error instanceof HttpErrorResponse) return fallback;

    return error?.message ?? fallback;
}

// true si no hubo respuesta (estado 0) o el servidor falló (5xx)
export function isNetworkOrServerError(error: any): boolean {
    return error?.status === 0 || error?.status >= 500;
}

// Título de la página de error según el código HTTP
export function getErrorTitle(status: number): string {
    if (status === 0) return 'Sin conexión';
    if (status === 403) return 'No tienes permiso';
    if (status === 404) return 'No encontrado';
    if (status >= 500) return 'Error del servidor';
    return 'Error';
}