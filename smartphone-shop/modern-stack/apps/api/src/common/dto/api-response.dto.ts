export class ApiResponse<T> {
  constructor(
    public readonly success: boolean,
    public readonly data: T,
    public readonly message?: string,
  ) {}
}
