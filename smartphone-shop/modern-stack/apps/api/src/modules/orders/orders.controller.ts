import { Body, Controller, Get, Headers, Post } from '@nestjs/common';

import { ApiResponse } from '../../common/dto/api-response.dto';
import { CreateOrderDto } from './dto/create-order.dto';
import type { OrderRecord } from './orders.service';
import { OrdersService } from './orders.service';

@Controller('orders')
export class OrdersController {
  constructor(private readonly ordersService: OrdersService) {}

  @Get()
  list(@Headers('x-user-id') userId = 'guest'): ApiResponse<{ items: OrderRecord[] }> {
    return new ApiResponse(true, {
      items: this.ordersService.listByUser(userId),
    });
  }

  @Post()
  create(
    @Headers('x-user-id') userId = 'guest',
    @Body() dto: CreateOrderDto,
  ): ApiResponse<{ order: OrderRecord }> {
    return new ApiResponse(true, {
      order: this.ordersService.create(userId, dto.customerName, dto.shippingAddress),
    });
  }
}
