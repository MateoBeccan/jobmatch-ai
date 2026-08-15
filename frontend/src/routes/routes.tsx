export type AppRoute =
  | { name: 'analyze' }
  | { name: 'history' }
  | { name: 'detail'; id: string }

export function normalizeRoute(pathname: string) {
  if (pathname === '/historial' || pathname.startsWith('/historial/')) return pathname
  return '/analizar'
}

export function parseRoute(pathname: string): AppRoute {
  const normalized = normalizeRoute(pathname)
  if (normalized === '/historial') return { name: 'history' }
  if (normalized.startsWith('/historial/')) {
    return { name: 'detail', id: decodeRouteId(normalized.slice('/historial/'.length)) }
  }
  return { name: 'analyze' }
}

export function decodeRouteId(value: string) {
  try {
    return decodeURIComponent(value)
  } catch {
    return ''
  }
}
