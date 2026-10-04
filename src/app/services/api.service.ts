import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserResponse } from '../interfaces/user';
import { PostResponse } from '../interfaces/post';
import { CommentResponse } from '../interfaces/comment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private baseUrl = 'https://dummyjson.com';

  constructor(private http: HttpClient) { }

  //Busca un usuario por su username
  getUserByUsername(username: string): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/users/filter?key=username&value=${username}`);
  }

  // Trae los posts relacionados al usuario mediante su ID
  getPostsByUser(userId: number): Observable<PostResponse> {
    return this.http.get<PostResponse>(`${this.baseUrl}/posts/user/${userId}`);
  }

  // Trae los comentarios relacionados a un post específico
  getCommentsByPost(postId: number): Observable<CommentResponse> {
    return this.http.get<CommentResponse>(`${this.baseUrl}/comments/post/${postId}`);
  }
}