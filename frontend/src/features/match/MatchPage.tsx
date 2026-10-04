import { useState } from 'react';
import { Alert, Button, Select, Tag, Textarea, TextField } from '../../components/ds';
import { errorMessage, fieldErrors } from '../../lib/errors';
import { go } from '../../lib/router';
import { useSession } from '../../session';
import { api } from '../../api/client';
import { WithDictation } from '../dictation/WithDictation';
import { saveMatchDesc } from './matchDesc';

const EXAMPLES = [
  ['Brak transportu do lekarza', 'Seniorzy z naszej wsi nie mają jak dojechać do przychodni, autobus jeździ dwa razy dziennie.'],
  ['Samotni seniorzy na osiedlu', 'Na naszym osiedlu mieszka wiele samotnych starszych osób. Nikt nie wie, czy czegoś potrzebują.'],
  ['Młodzież bez miejsca spotkań', 'Młodzież po szkole nie ma gdzie się spotykać, świetlica jest zamknięta od roku.'],
];

export function MatchPage() {
  const { regions, showToast } = useSession();
  const [desc, setDesc] = useState('');
  const [region, setRegion] = useState('');
  const [author, setAuthor] = useState('');
  const [err, setErr] = useState<string>();
  const [sending, setSending] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    const d = desc.trim();
    if (!d) return setErr('Opisz problem — wystarczą 2–3 zdania.');
    setSending(true);
    setErr(undefined);
    try {
      const res = await api.match({ description: d, region: region || undefined, authorName: author.trim() || undefined });
      saveMatchDesc(res.reportId, d);
      go('/dopasuj/' + res.reportId);
    } catch (e) {
      setSending(false);
      const f = fieldErrors(e).description;
      setErr(f);
      if (!f) showToast(errorMessage(e) || 'Nie udało się wysłać opisu.', 'danger');
    }
  }

  return (
    <div className="container">
      <form className="stack narrow" onSubmit={submit} noValidate>
        <h1 className="h1">Opisz problem, dopasujemy rozwiązania</h1>
        <p className="lead">
          Napisz własnymi słowami, co dzieje się w Twojej okolicy. Pokażemy innowacje, które działają gdzie indziej, i
          wyjaśnimy, dlaczego pasują. Nie musisz zakładać konta.
        </p>
        <div className="row">
          <span className="small muted">Przykłady:</span>
          {EXAMPLES.map(([label, text]) => (
            <Tag
              key={label}
              onClick={() => {
                setDesc(text);
                setErr(undefined);
              }}
            >
              {label}
            </Tag>
          ))}
        </div>
        <WithDictation value={desc} onChange={setDesc} label="Podyktuj opis problemu">
            <Textarea
            id="m-desc"
            label="Opis problemu"
            hint="Kogo dotyczy, gdzie i od kiedy. Np. „Seniorzy z naszej wsi nie mają jak dojechać do przychodni.”"
            rows={6}
            maxLength={2000}
            value={desc}
            onChange={(e) => setDesc(e.target.value)}
            error={err}
            required
          />
        </WithDictation>
        <Select
          id="m-region"
          label="Region"
          optional
          options={regions.map((r) => ({ value: r.code, label: r.label }))}
          placeholder="Wybierz region"
          value={region}
          onChange={(e) => setRegion(e.target.value)}
        />
        <TextField
          id="m-author"
          label="Twoje imię"
          optional
          hint="Zobaczą je pracownicy, którzy przeglądają raporty."
          value={author}
          onChange={(e) => setAuthor(e.target.value)}
        />
        {sending && (
          <Alert tone="info" title="Analizujemy Twój opis">
            Porównujemy go z bazą innowacji. To zwykle trwa kilka sekund.
          </Alert>
        )}
        <div>
          <Button type="submit" size="lg" iconRight="arrow-right" disabled={sending}>
            {sending ? 'Dopasowujemy…' : 'Dopasuj rozwiązania'}
          </Button>
        </div>
      </form>
    </div>
  );
}
