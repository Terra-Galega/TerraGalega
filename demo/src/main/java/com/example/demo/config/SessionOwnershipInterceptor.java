package com.example.demo.config;

import com.example.demo.controller.AuthController;
import com.example.demo.entities.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

// Interceptor encargado de verificar que un usuario solo pueda acceder
// a recursos que correspondan a su propio ID
@Component
public class SessionOwnershipInterceptor implements HandlerInterceptor {

    // Se ejecuta antes de llegar al Controller
    // Obtiene el ID de la URL y lo compara con el ID del usuario
    // almacenado en la sesión

    // Si coinciden, permite continuar
    // Si no coinciden, cierra la sesión y redirige al login

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) throws Exception {

        Integer urlId = extractIdFromPath(request);

        // Si la URL no tiene un ID, no hay nada que verificar
        if (urlId == null) {
            return true;
        }

        // Obtiene la sesión existente y recupera el usuario que
        // inició sesión. Se utiliza getSession(false) para no crear
        // una sesión nueva si el usuario no tiene una
        HttpSession session = request.getSession(false);

        User loggedUser = (session != null)
                ? (User) session.getAttribute(AuthController.SESSION_Client)
                : null;

        // Comprueba que exista un usuario logueado y que el ID de la URL
        // sea el mismo que el ID de dicho usuario

        boolean idIsntMine = loggedUser == null || !urlId.equals(loggedUser.getId());

        // Si el usuario no tiene permiso, se elimina su sesión,
        // se redirige al login y se detiene la petición
        if (idIsntMine) {

            if (session != null) {
                session.invalidate();
            }

            response.sendRedirect(
                    request.getContextPath() + "/login");

            return false;
        }

        // Si el ID es correcto, permite que la petición llegue al Controller
        return true;
    }

    // Obtiene el ID de la URL utilizando las variables que Spring
    // guarda durante el procesamiento de la ruta

    @SuppressWarnings("unchecked")
    private Integer extractIdFromPath(HttpServletRequest request) {

        Map<String, String> vars = (Map<String, String>) request
                .getAttribute(
                        HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        // Si no existe un ID en la URL, devuelve null
        if (vars == null || !vars.containsKey("id")) {
            return null;
        }

        // Convierte el ID de la URL de String a Integer
        try {
            return Integer.valueOf(vars.get("id"));
        } catch (NumberFormatException e) {
            // Si el ID no es numérico, se considera inválido
            return null;
        }
    }
}