/**
 * 从 catch 到的未知值中安全地提取错误信息。
 *
 * TS 的 `catch (err)` 中 err 类型为 unknown，不能直接访问 `.message`；
 * 统一通过本函数取值，配合 ApiError（继承 Error）可拿到后端返回的 message。
 */
export function errorMessage(err: unknown, fallback = '操作失败'): string {
  if (err instanceof Error) return err.message || fallback
  if (typeof err === 'string' && err) return err
  return fallback
}
