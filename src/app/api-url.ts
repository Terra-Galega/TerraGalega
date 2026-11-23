// URL del back.
// - En producción (Railway): el Dockerfile reemplaza __API_URL__ por la variable API_URL
//   del servicio del front (p. ej. https://terragalega-back.up.railway.app).
// - En desarrollo: el mismo equipo desde el que se abrió la web, puerto 8080.
//   · En el PC (http://localhost:4200)         -> http://localhost:8080
//   · En el celular (http://10.195.23.83:4200) -> http://10.195.23.83:8080
// El interceptor solo agrega el token a las peticiones hacia aquí.
const API_URL_PRODUCCION = '__API_URL__';

export const API_URL = API_URL_PRODUCCION.startsWith('__')
  ? `${window.location.protocol}//${window.location.hostname}:8080`
  : API_URL_PRODUCCION.replace(/\/+$/, '');
