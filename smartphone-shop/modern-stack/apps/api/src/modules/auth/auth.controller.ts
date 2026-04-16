import { Body, Controller, Post } from '@nestjs/common';

import { ApiResponse } from '../../common/dto/api-response.dto';
import { AuthService } from './auth.service';
import { LoginDto } from './dto/login.dto';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('login')
  login(@Body() dto: LoginDto): ApiResponse<{ accessToken: string; role: string; email: string }> {
    return new ApiResponse(true, this.authService.login(dto.email));
  }
}
