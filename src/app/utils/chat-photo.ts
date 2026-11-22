// Prepara la foto que el cliente adjunta en el chat del pedido:
// 1. Solo acepta HEIC, PNG, JPG y JPEG.
// 2. HEIC (iPhone) se convierte a JPG, porque Chrome, Edge y Firefox no lo muestran.
// 3. Si es muy grande, se achica a 1600 px como máximo y se pasa a JPG.

// Para el atributo "accept" del <input type="file">
export const CHAT_PHOTO_ACCEPT = '.heic,.png,.jpg,.jpeg,image/heic,image/png,image/jpeg';

const ALLOWED_EXTENSIONS = ['heic', 'png', 'jpg', 'jpeg'];
const ALLOWED_TYPES = ['image/heic', 'image/heif', 'image/png', 'image/jpeg'];
const MAX_SIDE = 1600;
// Si ya es pequeña (lado <= 1600 y <= 1.5 MB) se envía tal cual
const KEEP_AS_IS_BYTES = 1.5 * 1024 * 1024;
// Lo que acepta el back
const MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
// Fotos absurdamente grandes ni se intentan procesar
const MAX_INPUT_BYTES = 30 * 1024 * 1024;

export type ChatPhotoErrorCode = 'type' | 'size' | 'convert';

export class ChatPhotoError extends Error {
  constructor(public readonly code: ChatPhotoErrorCode) {
    super(code);
  }
}

function extensionOf(name: string): string {
  const dot = name.lastIndexOf('.');
  return dot >= 0 ? name.slice(dot + 1).toLowerCase() : '';
}

function isHeic(file: File): boolean {
  return (
    extensionOf(file.name) === 'heic' ||
    file.type === 'image/heic' ||
    file.type === 'image/heif'
  );
}

export async function prepareChatPhoto(file: File): Promise<Blob> {
  const extensionOk = ALLOWED_EXTENSIONS.includes(extensionOf(file.name));
  // Algunos navegadores no reconocen HEIC y dejan el tipo vacío: ahí manda la extensión
  const typeOk = file.type === '' || ALLOWED_TYPES.includes(file.type);
  if (!extensionOk || !typeOk) {
    throw new ChatPhotoError('type');
  }
  if (file.size > MAX_INPUT_BYTES) {
    throw new ChatPhotoError('size');
  }

  let blob: Blob = file;

  if (isHeic(file)) {
    try {
      // La librería solo se descarga cuando alguien elige una foto HEIC
      const { default: heic2any } = await import('heic2any');
      const result = await heic2any({ blob: file, toType: 'image/jpeg', quality: 0.85 });
      blob = Array.isArray(result) ? result[0] : result;
    } catch {
      throw new ChatPhotoError('convert');
    }
  }

  const prepared = await shrink(blob);
  if (prepared.size > MAX_UPLOAD_BYTES) {
    throw new ChatPhotoError('size');
  }
  return prepared;
}

async function shrink(blob: Blob): Promise<Blob> {
  let bitmap: ImageBitmap;
  try {
    // from-image: respeta la orientación de la foto del celular (EXIF)
    bitmap = await createImageBitmap(blob, { imageOrientation: 'from-image' });
  } catch {
    // No es una imagen de verdad (o está dañada)
    throw new ChatPhotoError('type');
  }

  const scale = Math.min(1, MAX_SIDE / Math.max(bitmap.width, bitmap.height));
  if (scale === 1 && blob.size <= KEEP_AS_IS_BYTES && blob.type !== '') {
    bitmap.close();
    return blob;
  }

  const canvas = document.createElement('canvas');
  canvas.width = Math.round(bitmap.width * scale);
  canvas.height = Math.round(bitmap.height * scale);
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    bitmap.close();
    throw new ChatPhotoError('convert');
  }

  // Fondo blanco por si era un PNG con transparencia (el JPG no la tiene)
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
  bitmap.close();

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (result) => (result ? resolve(result) : reject(new ChatPhotoError('convert'))),
      'image/jpeg',
      0.85,
    );
  });
}