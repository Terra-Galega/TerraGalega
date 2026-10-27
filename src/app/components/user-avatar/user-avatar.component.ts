import { Component, Input } from '@angular/core';

// Círculo con las iniciales del usuario
@Component({
  selector: 'app-user-avatar',
  templateUrl: './user-avatar.component.html',
  styleUrl: './user-avatar.component.scss',
})
export class UserAvatarComponent {
  // Nombre del usuario; su primera letra forma la primera inicial.
  @Input() name = '';
  // Apellido del usuario; su primera letra forma la segunda inicial.
  @Input() lastName = '';
  // Tamaño del avatar: 'lg' (grande, por defecto) o 'md' (mediano).
  @Input() size: 'md' | 'lg' = 'lg';

  // Calcula las iniciales en mayúsculas a partir del nombre y el apellido.
  get initials(): string {
    // Toma la primera letra de cada texto; si no hay valor, usa una cadena vacía.
    const first = this.name?.charAt(0) ?? '';
    const last = this.lastName?.charAt(0) ?? '';
    return (first + last).toUpperCase();
  }

  // Devuelve las clases de Tailwind de ancho, alto y tamaño de letra según el tamaño elegido.
  get sizeClasses(): string {
    return this.size === 'lg'
      ? 'w-24 h-24 md:w-28 md:h-28 text-3xl md:text-4xl'
      : 'w-20 h-20 text-2xl';
  }
}