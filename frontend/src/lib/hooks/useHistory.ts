import { useCallback, useEffect, useState } from 'react'
import { deleteAnalysis, getAnalyses, getAnalysis } from '../../services/api'
import type { AnalysisSummary, HistoryRecord } from '../../lib/types/types'

export function useHistory() {
  const [records, setRecords] = useState<AnalysisSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [loadingMore, setLoadingMore] = useState(false)
  const [error, setError] = useState('')
  const [hasMore, setHasMore] = useState(false)
  const [page, setPage] = useState(0)

  const loadFirstPage = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const result = await getAnalyses(0, 20)
      setRecords(result.content)
      setHasMore(result.page < result.totalPages - 1)
      setPage(result.page)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'No se pudo cargar el historial.')
    } finally {
      setLoading(false)
    }
  }, [])

  const loadMore = useCallback(async () => {
    if (loadingMore || !hasMore) return
    setLoadingMore(true)
    setError('')
    try {
      const result = await getAnalyses(page + 1, 20)
      setRecords((current) => [...current, ...result.content])
      setHasMore(result.page < result.totalPages - 1)
      setPage(result.page)
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'No se pudieron cargar más análisis.')
    } finally {
      setLoadingMore(false)
    }
  }, [loadingMore, hasMore, page])

  const deleteRecord = useCallback(async (id: string) => {
    await deleteAnalysis(id)
    setRecords((current) => current.filter((record) => record.id !== id))
  }, [])

  const fetchRecord = useCallback(
    (id: string): Promise<HistoryRecord> => getAnalysis(id),
    [],
  )

  useEffect(() => {
    void loadFirstPage()
  }, [loadFirstPage])

  return { records, loading, loadingMore, error, hasMore, loadFirstPage, loadMore, deleteRecord, fetchRecord }
}
