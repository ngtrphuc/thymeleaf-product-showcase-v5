import { Controller, Get, Query } from '@nestjs/common';
import type { ProductSummary } from '@smartphone-shop/shared';

import { ApiResponse } from '../../common/dto/api-response.dto';
import { ProductsService } from './products.service';

@Controller('products')
export class ProductsController {
  constructor(private readonly productsService: ProductsService) {}

  @Get()
  list(@Query('keyword') keyword?: string): ApiResponse<{ items: ProductSummary[]; total: number }> {
    const items = this.productsService.list(keyword);
    return new ApiResponse(true, { items, total: items.length });
  }
}
