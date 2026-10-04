import { ApiError } from '../api/client';

export type PageError = { status: number | string; title: string; text: string };

export const ERR_404 = (text?: string): PageError => ({
  status: 404,
  title: 'Nie znaleźliśmy tej strony',
  text: text || 'Adres może być nieaktualny albo treść została usunięta.',
});
export const ERR_403: PageError = {
  status: 403,
  title: 'Nie masz dostępu do tej części',
  text: 'Ta część jest dostępna dla innej roli. Jeśli to pomyłka, skontaktuj się z administratorem serwisu.',
};

export function toPageError(e: unknown): PageError {
  if (e instanceof ApiError && e.status === 404) return ERR_404(e.message);
  if (e instanceof ApiError && e.status === 403) return ERR_403;
  return {
    status: e instanceof ApiError && e.status ? e.status : 500,
    title: 'Coś poszło nie tak',
    text: 'Nie udało się wczytać danych. Spróbuj ponownie za chwilę.',
  };
}

/** Field errors + message from a failed form submit. */
export const fieldErrors = (e: unknown) => (e instanceof ApiError ? e.fields : {});
export const errorMessage = (e: unknown) => (e instanceof Error ? e.message : 'Coś poszło nie tak.');
