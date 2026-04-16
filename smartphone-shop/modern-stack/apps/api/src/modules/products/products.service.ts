import { Injectable } from '@nestjs/common';
import type { ProductSummary } from '@smartphone-shop/shared';

import { SAMPLE_PRODUCTS } from '../shared/catalog.data';

@Injectable()
export class ProductsService {
  list(keyword?: string): ProductSummary[] {
    if (!keyword) {
      return SAMPLE_PRODUCTS;
    }
    const lower = keyword.toLowerCase();
    return SAMPLE_PRODUCTS.filter(
      (item) =>
        item.name.toLowerCase().includes(lower) ||
        item.brand.toLowerCase().includes(lower),
    );
  }
}
