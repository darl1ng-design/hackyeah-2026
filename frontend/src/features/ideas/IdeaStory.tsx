// "Opowiedz swój pomysł": one recording → LLM splits it into the idea form (fills only empty fields).
import { useState } from 'react';
import { api } from '../../api/client';
import type { IdeaRequest } from '../../api/types';
import { Alert, Badge, Icon } from '../../components/ds';
import { useSession } from '../../session';
import { Dictation } from '../dictation/Dictation';
import { appendText } from '../dictation/useDictation';
import s from './IdeaStory.module.css';

type Form = Required<IdeaRequest>;
type Props = { form: Form; setForm: (f: Form) => void };

export function IdeaStory({ form, setForm }: Props) {
  const { whisperOk, showToast } = useSession();
  const [parsing, setParsing] = useState(false);
  const [filled, setFilled] = useState(false);

  async function parse(text: string) {
    setParsing(true);
    try {
      const r = await api.parseIdea(text);
      const prev = form;
      setForm({
        title: prev.title || r.title || '',
        essence: prev.essence || r.essence || '',
        targetGroup: prev.targetGroup || r.targetGroup || '',
        stage: prev.stage === 'MYSL' && r.stage ? r.stage : prev.stage,
        description: appendText(prev.description, r.description || '', true),
      });
      setFilled(true);
      showToast('Uzupełniliśmy formularz z nagrania.', 'success', {
        label: 'Cofnij',
        run: () => {
          setForm(prev);
          setFilled(false);
        },
      });
    } catch {
      showToast('Nie udało się ułożyć wypowiedzi w formularzu. Spróbuj ponownie albo wypełnij pola ręcznie.', 'danger');
    } finally {
      setParsing(false);
    }
  }

  return (
    <>
      {whisperOk && (
        <section aria-label="Opowiedz swój pomysł" className={s.story}>
          <div className={s.head}>
            <span className={s.icon}>
              <Icon name="mic" size={24} />
            </span>
            <div className={s.text}>
              <div className={s.titleRow}>
                <h2 className={s.title}>Opowiedz swój pomysł</h2>
                <Badge tone="info" size="sm">
                  Etap 2
                </Badge>
              </div>
              <p className={s.lead}>
                Nagraj do 2 minut, jak opowiadasz komuś przy kawie. Rozłożymy to na tytuł, istotę, grupę docelową, etap i
                opis. Wypełniamy tylko puste pola, a Ty wszystko sprawdzasz.
              </p>
            </div>
          </div>
          <Dictation
            big
            label="Opowiedz swój pomysł"
            bigLabel="Nagraj opowieść"
            bigHint="Do 2 minut. Nagranie nie jest nigdzie zapisywane."
            stopLabel="Zakończ i uzupełnij"
            busyText={parsing ? 'Układamy wypowiedź w formularzu…' : undefined}
            onText={parse}
          />
        </section>
      )}
      {filled && (
        <Alert tone="success" title="Uzupełniliśmy formularz z nagrania" onClose={() => setFilled(false)}>
          Sprawdź każde pole — nazwy miejscowości i osób mogą być przekręcone.
        </Alert>
      )}
    </>
  );
}
