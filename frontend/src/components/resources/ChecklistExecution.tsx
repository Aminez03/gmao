import { useEffect, useState } from 'react';

import {
  Alert,
  Button,
  MenuItem,
  Paper,
  Stack,
  TextField,
  Typography,
  Checkbox,
  FormControlLabel,
} from '@mui/material';

import { useAuth } from '../../app/Auth';
import { api, command, Entity, errorText } from '../../services/api';
import { cache, cached, enqueue, pending, queuePhoto, removeCached } from '../../services/offline';

export default function ChecklistExecution({
  order,
  onRefresh,
}: {
  order: Entity;
  onRefresh: () => Promise<void>;
}) {
  const { user } = useAuth();
  const [answers, setAnswers] = useState<Entity>(order.answers || {});
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const canEdit =
    order.status === 'EN_COURS' &&
    ['ADMIN', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN', 'SOUS_TRAITANT'].includes(user!.role);

  useEffect(() => {
    void cached(user!.id, `draft:${order.id}`).then((d) => {
      setAnswers(d || order.answers || {});
    });
  }, [order.id, order.version, user]);

  async function change(key: string, value: any) {
    const next = { ...answers, [key]: value };
    setAnswers(next);
    try {
      await cache(user!.id, `draft:${order.id}`, next);
    } catch {
      setError(
        'Le stockage local est plein ou indisponible. Votre saisie ne sera pas conservée après fermeture.',
      );
    }
  }

  async function save() {
    setError('');
    try {
      const waiting = await pending(user!.id);
      if (!navigator.onLine || waiting.some((o) => o.resourceId === order.id)) {
        await enqueue(user!.id, order, 'answers', { answers });
        setMessage('Enregistré sur cet appareil · à synchroniser.');
      } else {
        await command('work-orders', order, 'answers', { answers });
        await removeCached(user!.id, `draft:${order.id}`);
        setMessage('Réponses synchronisées.');
        await onRefresh();
      }
    } catch (e) {
      setError(errorText(e));
    }
  }

  if (!order.checklistSnapshot?.length)
    return <Alert severity="info">Aucune gamme opératoire associée à cette intervention.</Alert>;

  return (
    <Stack spacing={2}>
      <Typography variant="h6">Gamme opératoire</Typography>
      <Typography variant="body2" color="text.secondary">
        Les étapes obligatoires et les anomalies sont contrôlées avant soumission.
      </Typography>
      {message && <Alert>{message}</Alert>}
      {error && <Alert severity="error">{error}</Alert>}
      {order.checklistSnapshot.map((s: Entity) => (
        <Paper key={s.key} variant="outlined" sx={{ p: 2 }}>
          <Typography mb={1}>
            {s.label} {s.required ? '*' : ''}
          </Typography>
          {['CHECKBOX', 'ATTESTATION'].includes(s.type) ? (
            <FormControlLabel
              control={
                <Checkbox
                  disabled={!canEdit}
                  checked={!!answers[s.key]}
                  onChange={(e) => void change(s.key, e.target.checked)}
                />
              }
              label={
                s.type === 'ATTESTATION'
                  ? 'Je confirme avoir exécuté ce contrôle'
                  : 'Contrôle effectué'
              }
            />
          ) : s.type === 'CONFORMITY' ? (
            <TextField
              select
              fullWidth
              label="Résultat"
              disabled={!canEdit}
              value={answers[s.key] || ''}
              onChange={(e) => void change(s.key, e.target.value)}
            >
              <MenuItem value="CONFORME">Conforme</MenuItem>
              <MenuItem value="NON_CONFORME">Non conforme</MenuItem>
            </TextField>
          ) : s.type === 'PHOTO' ? (
            <>
              <Button component="label" disabled={!canEdit}>
                Joindre une photo
                <input
                  type="file"
                  accept="image/jpeg,image/png,image/webp"
                  hidden
                  onChange={async (e) => {
                    const file = e.target.files?.[0];
                    if (!file) return;
                    try {
                      if (!navigator.onLine) {
                        const local = await queuePhoto(user!.id, order, file);
                        await change(s.key, `local:${local}`);
                      } else {
                        const form = new FormData();
                        form.append('file', file);
                        form.append('targetType', 'work-orders');
                        form.append('targetId', order.id);
                        const { data } = await api.post('/files', form);
                        await change(s.key, data.id);
                      }
                    } catch (ex) {
                      setError(errorText(ex));
                    }
                  }}
                />
              </Button>
              {answers[s.key] && (
                <Typography variant="body2">
                  Photo enregistrée{' '}
                  {String(answers[s.key]).startsWith('local:') ? 'sur cet appareil' : ''}
                </Typography>
              )}
            </>
          ) : (
            <TextField
              fullWidth
              label={s.unit ? `Valeur (${s.unit})` : 'Réponse'}
              type={['NUMBER', 'MEASURE'].includes(s.type) ? 'number' : 'text'}
              disabled={!canEdit}
              value={answers[s.key] ?? ''}
              onChange={(e) =>
                void change(
                  s.key,
                  ['NUMBER', 'MEASURE'].includes(s.type) && e.target.value !== ''
                    ? Number(e.target.value)
                    : e.target.value,
                )
              }
              helperText={
                s.min !== undefined || s.max !== undefined
                  ? `Plage attendue : ${s.min ?? '—'} à ${s.max ?? '—'} ${s.unit || ''}`
                  : undefined
              }
            />
          )}
          <Typography variant="caption" color="text.secondary">
            Référence : {s.key}
          </Typography>
        </Paper>
      ))}
      {canEdit && (
        <Button variant="contained" onClick={save}>
          Enregistrer les réponses
        </Button>
      )}
    </Stack>
  );
}
