import { Component, Input } from '@angular/core';

// Círculo con las iniciales del usuario 
@Component({
  selector: 'app-user-avatar',
  templateUrl: './user-avatar.component.html',
  styleUrl: './user-avatar.component.scss',
})
export class UserAvatarComponent {
  @Input() name = '';
  @Input() lastName = '';
  @Input() size: 'md' | 'lg' = 'lg';

  get initials(): string {
    const first = this.name?.charAt(0) ?? '';
    const last = this.lastName?.charAt(0) ?? '';
    return (first + last).toUpperCase();
  }

  get sizeClasses(): string {
    return this.size === 'lg'
      ? 'w-24 h-24 md:w-28 md:h-28 text-3xl md:text-4xl'
      : 'w-20 h-20 text-2xl';
  }
}
