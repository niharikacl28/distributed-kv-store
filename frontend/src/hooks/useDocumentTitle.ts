import { useEffect } from 'react'

/**
 * Sets the document title for the lifetime of the component,
 * restoring the previous title on unmount.
 */
export function useDocumentTitle(title: string) {
  useEffect(() => {
    const previous = document.title
    document.title = title

    return () => {
      document.title = previous
    }
  }, [title])
}
