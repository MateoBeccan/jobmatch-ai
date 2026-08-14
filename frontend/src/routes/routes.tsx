export function normalizeRoute(pathname: string) {
  if (pathname === '/' || pathname === '/analizar') return '/analizar'
  return '/analizar'
}

export function decodeRouteId(value: string) {
  try {
    return decodeURIComponent(value)
  } catch {
    return ''
  }
}
