import { useEffect, useState } from 'react'

export function useObjectUrl(file: File | null) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null)

  useEffect(() => {
    if (!file) {
      setObjectUrl(null)
      return
    }

    const url = URL.createObjectURL(file)
    setObjectUrl(url)

    return () => URL.revokeObjectURL(url)
  }, [file])

  return file ? objectUrl : null
}
