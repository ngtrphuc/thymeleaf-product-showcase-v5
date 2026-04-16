import { HealthController } from './health.controller';

describe('HealthController', () => {
  it('returns healthy status', () => {
    const controller = new HealthController();
    const response = controller.ping();

    expect(response.success).toBe(true);
    expect(response.data.status).toBe('ok');
  });
});
