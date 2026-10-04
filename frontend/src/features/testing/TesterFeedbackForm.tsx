import { useState, type FormEvent } from "react";
import { api } from "../../api/client";
import { Alert, Button, Select, Textarea } from "../../components/ds";
import { errorMessage } from "../../lib/errors";
import { loginHref } from "../../lib/router";
import { useSession } from "../../session";

export function TesterFeedbackForm({ innovationId }: { innovationId: number }) {
  const { me, role } = useSession();
  const [interested, setInterested] = useState(false);
  const [rating, setRating] = useState("");
  const [feedback, setFeedback] = useState("");
  const [suggestion, setSuggestion] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [saved, setSaved] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setError("");
    setSaved(false);
    setBusy(true);
    try {
      await api.testerSubmit(innovationId, {
        interested: true,
        ...(rating ? { rating: Number(rating) } : {}),
        feedback: feedback.trim(),
        suggestion: suggestion.trim(),
      });
      setSaved(true);
    } catch (cause) {
      setError(errorMessage(cause));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="box stack" aria-labelledby="tester-heading" style={{ padding: "var(--space-6)" }}>
      <h2 id="tester-heading" className="h2">Przetestuj tę innowację</h2>
      <p className="muted">Zgłoś chęć udziału albo podziel się opinią i propozycją usprawnienia. Zgłoszenie wymaga konta.</p>
      {!me ? (
        <a href={"#" + loginHref(location.hash.slice(1) || `/innowacje/${innovationId}`)}>Zaloguj się, aby zgłosić udział</a>
      ) : role !== "MEMBER" ? (
        <p className="small">Ta funkcja jest przeznaczona dla kont mieszkańców i organizacji.</p>
      ) : (
        <form className="stack" onSubmit={submit}>
          <label className="row">
            <input type="checkbox" checked={interested} onChange={(event) => setInterested(event.target.checked)} required />
            <span>Chcę wziąć udział w testach tej innowacji</span>
          </label>
          <Select id="tester-rating" label="Ocena rozwiązania" optional value={rating}
            options={[1, 2, 3, 4, 5].map((value) => ({ value: String(value), label: `${value} / 5` }))}
            placeholder="Nie oceniam teraz" onChange={(event) => setRating(event.target.value)} />
          <Textarea id="tester-feedback" label="Twoja opinia" optional maxLength={3000} rows={4}
            value={feedback} onChange={(event) => setFeedback(event.target.value)} />
          <Textarea id="tester-suggestion" label="Co warto usprawnić?" optional maxLength={2000} rows={3}
            value={suggestion} onChange={(event) => setSuggestion(event.target.value)} />
          <p role="status" aria-live="polite">{busy ? "Zapisuję zgłoszenie…" : ""}</p>
          {error && <p role="alert">{error}</p>}
          {saved && <div role="status"><Alert tone="success">Zgłoszenie zostało zapisane. Możesz je później zaktualizować.</Alert></div>}
          <Button type="submit" disabled={busy || !interested}>{busy ? "Zapisuję…" : "Zgłoś udział i opinię"}</Button>
        </form>
      )}
    </section>
  );
}
