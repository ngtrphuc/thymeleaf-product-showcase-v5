import { Injectable, NotFoundException } from '@nestjs/common';

import { ProductsService } from '../products/products.service';

interface CartLine {
  productId: number;
  quantity: number;
}

@Injectable()
export class CartService {
  private readonly carts = new Map<string, CartLine[]>();

  constructor(private readonly productsService: ProductsService) {}

  getCart(userId: string): CartLine[] {
    return this.carts.get(userId) ?? [];
  }

  add(userId: string, productId: number, quantity: number): CartLine[] {
    const productExists = this.productsService.list().some((item) => item.id === productId);
    if (!productExists) {
      throw new NotFoundException('Product not found.');
    }

    const cart = this.getCart(userId);
    const existing = cart.find((item) => item.productId === productId);
    if (existing) {
      existing.quantity += quantity;
    } else {
      cart.push({ productId, quantity });
    }
    this.carts.set(userId, cart);
    return cart;
  }

  clear(userId: string): void {
    this.carts.delete(userId);
  }
}
