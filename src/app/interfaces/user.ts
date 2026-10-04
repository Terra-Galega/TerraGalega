export interface User {
  id: number;
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  image: string;
}

export interface UserResponse {
  users: User[];
  total: number;
  skip: number;
  limit: number;
}