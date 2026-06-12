export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserDto;
}

export interface UserDto {
  id: string;
  name: string;
  email: string;
  createdAt?: string;
  lastLoginAt?: string | null;
}

export interface CurrentUser {
  id: string;
  name: string;
  email: string;
  createdAt: string;
  lastLoginAt: string | null;
}
