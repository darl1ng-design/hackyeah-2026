// Match description by reportId (sessionStorage) so "Rozwiń w pomysł" can prefill the idea form. Backend doesn't return it.
const KEY = 'hub-matchdesc';

function all(): Record<string, string> {
  try {
    return JSON.parse(sessionStorage.getItem(KEY) || '{}');
  } catch {
    return {};
  }
}

export function saveMatchDesc(reportId: number, desc: string) {
  try {
    sessionStorage.setItem(KEY, JSON.stringify({ ...all(), [reportId]: desc }));
  } catch {
    /* storage unavailable: prefill just won't happen */
  }
}

export const loadMatchDesc = (reportId: string) => all()[reportId] || '';
