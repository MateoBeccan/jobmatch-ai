export type AppRoute =
  | { name: 'home' }
  | { name: 'analyze' }
  | { name: 'history' }
  | { name: 'detail'; id: string }

export function normalizeRoute(pathname: string) {
  if (pathname === '/') return '/'
  if (pathname === '/analizar') return '/analizar'
  if (pathname === '/historial' || pathname.startsWith('/historial/')) return pathname
  return '/'
}

export function parseRoute(pathname: string): AppRoute {
  const normalized = normalizeRoute(pathname)
  if (normalized === '/') return { name: 'home' }
  if (normalized === '/analizar') return { name: 'analyze' }
  if (normalized === '/historial') return { name: 'history' }
  if (normalized.startsWith('/historial/')) {
    return { name: 'detail', id: decodeRouteId(normalized.slice('/historial/'.length)) }
  }
  return { name: 'home' }
}

export function decodeRouteId(value: string) {
  try {
    return decodeURIComponent(value)
  } catch {
    return ''
  }
}
