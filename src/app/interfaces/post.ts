export interface Post {
  id: number;
  title: string;
  body: string;
  userId: number;
  reactions: {
    likes: number;
    dislikes: number;
  }; 
}

export interface PostResponse {
  posts: Post[];
  total: number;
  skip: number;
  limit: number;
}