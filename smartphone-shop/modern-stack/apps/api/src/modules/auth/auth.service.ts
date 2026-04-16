import { Injectable } from '@nestjs/common';

@Injectable()
export class AuthService {
  login(email: string) {
    const role = email.toLowerCase().startsWith('admin') ? 'ADMIN' : 'USER';
    return {
      accessToken: `demo-token-${Buffer.from(email).toString('base64url')}`,
      role,
      email,
    };
  }
}
