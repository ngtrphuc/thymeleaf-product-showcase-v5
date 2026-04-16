import { Injectable } from '@nestjs/common';

import { CartService } from '../cart/cart.service';

interface Order {
  id: string;
  userId: string;
  customerName: string;
  shippingAddress: string;
  createdAt: string;
  items: { productId: number; quantity: number }[];
}

export type OrderRecord = Order;

@Injectable()
export class OrdersService {
  private readonly orders: Order[] = [];

  constructor(private readonly cartService: CartService) {}

  create(userId: string, customerName: string, shippingAddress: string): OrderRecord {
    const cartItems = this.cartService.getCart(userId);
    const order: Order = {
      id: `ORD-${Date.now()}`,
      userId,
      customerName,
      shippingAddress,
      createdAt: new Date().toISOString(),
      items: cartItems,
    };
    this.orders.unshift(order);
    this.cartService.clear(userId);
    return order;
  }

  listByUser(userId: string): OrderRecord[] {
    return this.orders.filter((order) => order.userId === userId);
  }
}
