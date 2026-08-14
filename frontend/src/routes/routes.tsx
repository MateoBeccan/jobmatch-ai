export function normalizeRoute(pathname: string) {
  if (pathname === '/' || pathname === '/historial') return '/historial'
  if (pathname === '/analizar') return '/analizar'
  if (/^\/analisis\/[^/]+$/.test(pathname)) return pathname
  return '/historial'
}

export function decodeRouteId(value: string) {
  try {
    return decodeURIComponent(value)
  } catch {
    return ''
  }
}
