// URL del back: el mismo equipo desde el que se abrió la web, puerto 8080.
// - En el PC (http://localhost:4200)            -> http://localhost:8080
// - En el celular (http://10.195.23.83:4200)    -> http://10.195.23.83:8080
// Así el enlace del correo de confirmación funciona también en el celular.
// El interceptor solo agrega el token a las peticiones hacia aquí.
export const API_URL = `${window.location.protocol}//${window.location.hostname}:8080`;
