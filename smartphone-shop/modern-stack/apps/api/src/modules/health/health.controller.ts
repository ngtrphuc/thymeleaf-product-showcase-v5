import { Controller, Get } from '@nestjs/common';

import { ApiResponse } from '../../common/dto/api-response.dto';

@Controller('health')
export class HealthController {
  @Get()
  ping(): ApiResponse<{ status: string; timestamp: string }> {
    return new ApiResponse(true, {
      status: 'ok',
      timestamp: new Date().toISOString(),
    });
  }
}
