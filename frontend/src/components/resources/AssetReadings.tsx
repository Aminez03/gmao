import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Paper, Stack, TextField, Typography } from '@mui/material';
import { api, Entity, errorText, headers } from '../../services/api';
import { enqueue, fetchCached } from '../../services/offline';
import { useAuth } from '../../app/Auth';

export default function AssetReadings({ asset }: { asset: Entity }) {
  const { user } = useAuth();
  const [metric, setMetric] = useState('');
  const [value, setValue] = useState('');
  const [unit, setUnit] = useState('');
  const [note, setNote] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const { data, refetch } = useQuery({
    queryKey: ['asset-readings', asset.id, user?.id],
    queryFn: () =>
      fetchCached(user!.id, `asset-readings/${asset.id}`, `/assets/${asset.id}/readings`),
    enabled: !!user,
  });
  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setError('');
    setMessage('');
    const payload = {
      metric: metric.trim(),
      value,
      unit: unit.trim(),
      note: note.trim(),
      recordedAt: new Date().toISOString(),
    };
    try {
      if (navigator.onLine)
        await api.post(`/assets/${asset.id}/readings`, payload, { headers: headers() });
      else await enqueue(user!.id, asset, 'asset-reading', payload);
      setMessage(
        navigator.onLine
          ? 'Relevé enregistré.'
          : 'Relevé conservé sur cet appareil ; il sera envoyé à la reconnexion.',
      );
      setMetric('');
      setValue('');
      setUnit('');
      setNote('');
      if (navigator.onLine) await refetch();
    } catch (e) {
      setError(errorText(e));
    }
  }
  return (
    <Stack spacing={1.5}>
      <BoxTitle />
      {!navigator.onLine && (
        <Alert severity="info">Saisie hors connexion : le relevé sera synchronisé plus tard.</Alert>
      )}
      {message && <Alert severity="success">{message}</Alert>}
      {error && <Alert severity="error">{error}</Alert>}
      <Stack component="form" onSubmit={submit} direction={{ xs: 'column', sm: 'row' }} gap={1}>
        <TextField
          size="small"
          required
          label="Compteur / mesure"
          placeholder="Température, heures…"
          value={metric}
          onChange={(e) => setMetric(e.target.value)}
        />
        <TextField
          size="small"
          required
          type="number"
          label="Valeur"
          value={value}
          onChange={(e) => setValue(e.target.value)}
          inputProps={{ step: 'any' }}
        />
        <TextField
          size="small"
          required
          label="Unité"
          placeholder="°C, h, bar…"
          value={unit}
          onChange={(e) => setUnit(e.target.value)}
        />
        <TextField
          size="small"
          label="Note"
          value={note}
          onChange={(e) => setNote(e.target.value)}
        />
        <Button type="submit" variant="contained">
          Enregistrer
        </Button>
      </Stack>
      <Stack spacing={1}>
        {(data || []).slice(0, 8).map((reading: Entity) => (
          <Paper key={reading.id} variant="outlined" sx={{ p: 1.5 }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={0.5}>
              <Typography fontWeight={650}>
                {reading.metric} : {reading.value} {reading.unit}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {new Date(reading.recordedAt).toLocaleString('fr-TN')}
              </Typography>
            </Stack>
            {reading.note && (
              <Typography variant="body2" color="text.secondary">
                {reading.note}
              </Typography>
            )}
          </Paper>
        ))}
        {navigator.onLine && data && !data.length && (
          <Typography color="text.secondary">Aucun relevé pour cet équipement.</Typography>
        )}
      </Stack>
    </Stack>
  );
}

function BoxTitle() {
  return <Typography variant="h6">Relevés de l’équipement</Typography>;
}
