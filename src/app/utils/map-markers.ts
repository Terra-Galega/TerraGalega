// Dibujos de los marcadores del mapa de seguimiento (estilo isométrico: cada
// cara tiene su propio tono para que se vean "en 3D").
// La app móvil usa EXACTAMENTE los mismos SVG (ui/MapIcons.kt): si cambias uno,
// copia el cambio en el otro.

// Restaurante (fijo): tiendita con techo terracota, toldo a rayas y cartel.
// Mira de frente; su base toca el suelo en la parte inferior del dibujo.
export const RESTAURANT_SVG = `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 128" width="100%" height="100%">
  <defs>
    <linearGradient id="tgr-front" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#FBF4E8"/><stop offset="1" stop-color="#E9D9C0"/>
    </linearGradient>
    <linearGradient id="tgr-side" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#D6C2A2"/><stop offset="1" stop-color="#BFA783"/>
    </linearGradient>
    <linearGradient id="tgr-glass" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#CBEAF2"/><stop offset="1" stop-color="#6FA6BA"/>
    </linearGradient>
  </defs>
  <ellipse cx="62" cy="119" rx="47" ry="7.5" fill="#000" fill-opacity="0.22"/>

  <polygon points="84,60 102,50 102,104 84,114" fill="url(#tgr-side)"/>
  <polygon points="88,73 98,67.5 98,82 88,87.5" fill="#8DB7C5"/>
  <rect x="20" y="60" width="64" height="54" fill="url(#tgr-front)"/>
  <rect x="20" y="108" width="64" height="6" fill="#8A3A1E"/>
  <polygon points="84,108 102,98 102,104 84,114" fill="#6E2C16"/>

  <rect x="28" y="84" width="16" height="24" rx="2" fill="#7A3E22"/>
  <rect x="30.5" y="87" width="11" height="9" rx="1" fill="#93502D"/>
  <circle cx="41" cy="98" r="1.5" fill="#F2C26B"/>
  <rect x="52" y="84" width="26" height="18" rx="2" fill="url(#tgr-glass)" stroke="#8A3A1E" stroke-width="2"/>
  <line x1="65" y1="84" x2="65" y2="102" stroke="#8A3A1E" stroke-width="1.6"/>
  <polygon points="55,86.5 61,86.5 55,94" fill="#FFFFFF" fill-opacity="0.55"/>
  <rect x="51" y="102" width="28" height="5" rx="1.5" fill="#556141"/>
  <circle cx="55" cy="101.5" r="2.4" fill="#E8A33D"/>
  <circle cx="61" cy="101" r="2.4" fill="#C0392B"/>
  <circle cx="67" cy="101.5" r="2.4" fill="#E8A33D"/>
  <circle cx="73" cy="101" r="2.4" fill="#C0392B"/>

  <rect x="20" y="74" width="64" height="4" fill="#000" fill-opacity="0.08"/>
  <polygon points="84,60 102,50 102,62 84,74" fill="#9C4A1C"/>
  <rect x="20" y="60" width="8" height="14" fill="#B2571F"/>
  <rect x="28" y="60" width="8" height="14" fill="#FBF4E8"/>
  <rect x="36" y="60" width="8" height="14" fill="#B2571F"/>
  <rect x="44" y="60" width="8" height="14" fill="#FBF4E8"/>
  <rect x="52" y="60" width="8" height="14" fill="#B2571F"/>
  <rect x="60" y="60" width="8" height="14" fill="#FBF4E8"/>
  <rect x="68" y="60" width="8" height="14" fill="#B2571F"/>
  <rect x="76" y="60" width="8" height="14" fill="#FBF4E8"/>
  <circle cx="24" cy="74" r="4" fill="#B2571F"/>
  <circle cx="32" cy="74" r="4" fill="#FBF4E8"/>
  <circle cx="40" cy="74" r="4" fill="#B2571F"/>
  <circle cx="48" cy="74" r="4" fill="#FBF4E8"/>
  <circle cx="56" cy="74" r="4" fill="#B2571F"/>
  <circle cx="64" cy="74" r="4" fill="#FBF4E8"/>
  <circle cx="72" cy="74" r="4" fill="#B2571F"/>
  <circle cx="80" cy="74" r="4" fill="#FBF4E8"/>

  <polygon points="14,48 90,48 108,37 32,37" fill="#C0603A"/>
  <line x1="32" y1="37.6" x2="107" y2="37.6" stroke="#E39A70" stroke-width="1.4"/>
  <rect x="14" y="48" width="76" height="12" fill="#8A2E22"/>
  <polygon points="90,48 108,37 108,49 90,60" fill="#6B2219"/>

  <rect x="46" y="33" width="2.4" height="8" fill="#3F4930"/>
  <rect x="72" y="33" width="2.4" height="8" fill="#3F4930"/>
  <polygon points="80,18 84,15.6 84,31.6 80,34" fill="#3F4930"/>
  <rect x="40" y="18" width="40" height="16" rx="4" fill="#556141"/>
  <circle cx="60" cy="26" r="4.6" fill="none" stroke="#FBF4E8" stroke-width="1.6"/>
  <line x1="52" y1="21.5" x2="52" y2="30.5" stroke="#FBF4E8" stroke-width="1.6" stroke-linecap="round"/>
  <line x1="68" y1="21.5" x2="68" y2="30.5" stroke="#FBF4E8" stroke-width="1.6" stroke-linecap="round"/>
</svg>`;

// Repartidor: carrito (furgoneta) de reparto con la caja de carga y el logo.
// Mira a la DERECHA; si avanza hacia el oeste se refleja. Las ruedas tocan el
// suelo en la parte inferior.
export const COURIER_SVG = `
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 100" width="100%" height="100%">
  <defs>
    <linearGradient id="tgc-cargo" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#FFFAF1"/><stop offset="1" stop-color="#E6D5BA"/>
    </linearGradient>
    <linearGradient id="tgc-cab" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="#D97B45"/><stop offset="1" stop-color="#9C4318"/>
    </linearGradient>
    <linearGradient id="tgc-glass" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="#CBEAF2"/><stop offset="1" stop-color="#6FA6BA"/>
    </linearGradient>
    <radialGradient id="tgc-tire" cx="0.5" cy="0.5" r="0.5">
      <stop offset="0.62" stop-color="#3A3A3A"/><stop offset="1" stop-color="#1B1B1B"/>
    </radialGradient>
  </defs>
  <ellipse cx="60" cy="90" rx="52" ry="5.5" fill="#000" fill-opacity="0.22"/>

  <polygon points="8,24 72,24 79,17 15,17" fill="#FFFDF8"/>
  <polygon points="72,24 79,17 79,36 72,36" fill="#D9C6A6"/>
  <rect x="8" y="24" width="64" height="50" rx="3" fill="url(#tgc-cargo)"/>
  <rect x="8" y="62" width="64" height="5" fill="#B2571F"/>
  <rect x="8" y="56" width="3" height="8" rx="1" fill="#C62828"/>
  <circle cx="40" cy="42" r="11.5" fill="#B2571F"/>
  <circle cx="40" cy="42" r="6" fill="none" stroke="#FBF4E8" stroke-width="1.6"/>
  <line x1="31.5" y1="36" x2="31.5" y2="48" stroke="#FBF4E8" stroke-width="1.7" stroke-linecap="round"/>
  <line x1="48.5" y1="36" x2="48.5" y2="48" stroke="#FBF4E8" stroke-width="1.7" stroke-linecap="round"/>

  <polygon points="72,34 92,34 97,29 79,29" fill="#E39060"/>
  <path d="M72 34 L92 34 C99 34 102 38 105 45 L110 57 C111.5 60 112 63 112 66 L112 74 L72 74 Z" fill="url(#tgc-cab)"/>
  <path d="M78 38 L91 38 C95.5 38 97.5 41 99.5 46 L102.5 54 L78 54 Z" fill="url(#tgc-glass)" stroke="#7A3414" stroke-width="1.4"/>
  <polygon points="81,40 87,40 81,48" fill="#FFFFFF" fill-opacity="0.55"/>
  <line x1="76" y1="38" x2="76" y2="72" stroke="#7A3414" stroke-opacity="0.45" stroke-width="1.2"/>
  <rect x="80" y="58" width="7" height="2.2" rx="1.1" fill="#7A3414"/>
  <rect x="96" y="47" width="4" height="6" rx="1.5" fill="#7A3414"/>
  <rect x="107.5" y="60" width="4.5" height="6" rx="1.5" fill="#FFE59A" stroke="#8A2E22" stroke-width="1"/>

  <rect x="98" y="70" width="17" height="5" rx="2.5" fill="#2C2C2C"/>
  <rect x="4" y="70" width="10" height="5" rx="2.5" fill="#2C2C2C"/>
  <path d="M17 74 A13 13 0 0 1 43 74 Z" fill="#2C2C2C"/>
  <path d="M77 74 A13 13 0 0 1 103 74 Z" fill="#2C2C2C"/>
  <circle cx="30" cy="76" r="11" fill="url(#tgc-tire)"/>
  <circle cx="30" cy="76" r="5" fill="#B8BEC4"/>
  <circle cx="30" cy="76" r="2" fill="#5A5F64"/>
  <circle cx="90" cy="76" r="11" fill="url(#tgc-tire)"/>
  <circle cx="90" cy="76" r="5" fill="#B8BEC4"/>
  <circle cx="90" cy="76" r="2" fill="#5A5F64"/>
</svg>`;

// Crea el elemento HTML de un marcador de MapLibre con el dibujo dentro.
// El dibujo va en un hijo para poder reflejarlo sin pelear con el transform
// que MapLibre pone en el elemento para posicionarlo.
export function markerElement(svg: string, width: number, height: number, label: string): HTMLElement {
  const root = document.createElement('div');
  root.setAttribute('role', 'img');
  root.setAttribute('aria-label', label);
  root.style.width = `${width}px`;
  root.style.height = `${height}px`;
  root.style.pointerEvents = 'none';

  const art = document.createElement('div');
  art.style.width = '100%';
  art.style.height = '100%';
  art.style.transition = 'transform 300ms ease';
  art.style.filter = 'drop-shadow(0 3px 4px rgba(44, 44, 44, 0.25))';
  art.innerHTML = svg;
  root.appendChild(art);

  return root;
}

// Refleja el dibujo (true = mirando a la izquierda)
export function setFacingLeft(element: HTMLElement, left: boolean): void {
  const art = element.firstElementChild as HTMLElement | null;
  if (art) art.style.transform = left ? 'scaleX(-1)' : 'none';
}
