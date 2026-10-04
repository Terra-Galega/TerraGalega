import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Post } from '../../interfaces/post';
import { Comment } from '../../interfaces/comment';

@Component({
  selector: 'app-user-posts',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-posts.component.html',
  styleUrls: ['./user-posts.component.scss']
})
export class UserPostsComponent {
  // Recibimos los arreglos desde el componente padre
  @Input() posts: Post[] = [];
  @Input() commentsByPost: { [postId: number]: Comment[] } = {};
}