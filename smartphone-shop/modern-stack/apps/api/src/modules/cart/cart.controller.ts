import { Body, Controller, Delete, Get, Headers, Post } from '@nestjs/common';

import { ApiResponse } from '../../common/dto/api-response.dto';
import { AddCartItemDto } from './dto/add-cart-item.dto';
import { CartService } from './cart.service';

@Controller('cart')
export class CartController {
  constructor(private readonly cartService: CartService) {}

  @Get()
  get(@Headers('x-user-id') userId = 'guest'): ApiResponse<{ items: { productId: number; quantity: number }[] }> {
    return new ApiResponse(true, { items: this.cartService.getCart(userId) });
  }

  @Post('items')
  add(
    @Headers('x-user-id') userId = 'guest',
    @Body() dto: AddCartItemDto,
  ): ApiResponse<{ items: { productId: number; quantity: number }[] }> {
    return new ApiResponse(true, {
      items: this.cartService.add(userId, dto.productId, dto.quantity),
    });
  }

  @Delete()
  clear(@Headers('x-user-id') userId = 'guest'): ApiResponse<{ cleared: boolean }> {
    this.cartService.clear(userId);
    return new ApiResponse(true, { cleared: true });
  }
}
