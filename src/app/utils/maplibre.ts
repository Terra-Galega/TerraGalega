// Carga de MapLibre (los mapas de dirección y de seguimiento de pedidos).
//
// MapLibre dibuja el mapa con ayuda de un worker (maplibre-gl-worker.mjs). Por defecto lo
// busca junto a su propio archivo: en producción eso es la raíz de la web, pero con
// "ng serve" la librería vive en la caché de Vite y el worker da 404, así que el mapa no
// llega a cargar. angular.json ya copia el worker a la raíz; aquí se le indica a MapLibre
// que lo tome de ahí, en desarrollo y en producción por igual.
//
// La librería se importa de forma diferida: solo se descarga al abrir una página con mapa.
let workerConfigured = false;

export async function loadMapLibre(): Promise<typeof import('maplibre-gl')> {
  const maplibre = await import('maplibre-gl');
  if (!workerConfigured) {
    maplibre.setWorkerUrl(new URL('maplibre-gl-worker.mjs', document.baseURI).href);
    workerConfigured = true;
  }
  return maplibre;
}
