export type Role = 'ADMIN' | 'USER';

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  message?: string;
}

export interface ProductSummary {
  id: number;
  name: string;
  brand: string;
  price: number;
  stock: number;
  imageUrl: string;
}

export interface CartLine {
  productId: number;
  quantity: number;
}
