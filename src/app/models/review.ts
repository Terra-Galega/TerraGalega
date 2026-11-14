// Reseña de un cliente, tal como la manda el back
export interface Review {
  id: number;
  // Estrellas enteras, de 1 a 5
  rating: number;
  comment: string;
  // Nombre y inicial del apellido, por ejemplo "María L."
  authorName: string;
  // Fecha de creación en formato ISO
  createdAt: string;
  // false = oculta por el admin (solo llega en el listado del admin)
  visible: boolean;
}

// Lo que el cliente envía al crear o editar su reseña
export interface ReviewRequest {
  rating: number;
  comment: string;
}