import { useEffect, useState } from 'react';
import { Alert, Button, Stack, Typography } from '@mui/material';
import PlayArrowOutlined from '@mui/icons-material/PlayArrowOutlined';
import StopOutlined from '@mui/icons-material/StopOutlined';
import { command, Entity, errorText } from '../../services/api';
import { enqueue } from '../../services/offline';
import { useAuth } from '../../app/Auth';

export default function WorkTimer({
  order,
  onSaved,
}: {
  order: Entity;
  onSaved: () => Promise<void>;
}) {
  const { user } = useAuth();
  const key = `lmx:timer:${user!.id}:${order.id}`;
  const [started, setStarted] = useState<number | null>(
    () => Number(localStorage.getItem(key)) || null,
  );
  const [now, setNow] = useState(Date.now());
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  const seconds = started ? Math.max(0, Math.floor((now - started) / 1000)) : 0;
  const elapsed = `${String(Math.floor(seconds / 3600)).padStart(2, '0')}:${String(Math.floor((seconds % 3600) / 60)).padStart(2, '0')}:${String(seconds % 60).padStart(2, '0')}`;
  async function stop() {
    if (!started) return;
    if (seconds < 60) {
      setError('Le chronomètre doit durer au moins une minute pour enregistrer le temps.');
      return;
    }
    setBusy(true);
    setError('');
    const payload = { startsAt: new Date(started).toISOString(), endsAt: new Date().toISOString() };
    try {
      if (navigator.onLine) await command('work-orders', order, 'time', payload);
      else await enqueue(user!.id, order, 'time', payload);
      localStorage.removeItem(key);
      setStarted(null);
      await onSaved();
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Stack spacing={1}>
      <Stack direction={{ xs: 'column', sm: 'row' }} alignItems={{ sm: 'center' }} gap={1.5}>
        {started ? (
          <>
            <Typography variant="h5" sx={{ fontVariantNumeric: 'tabular-nums' }} aria-live="polite">
              {elapsed}
            </Typography>
            <Button
              color="error"
              variant="contained"
              startIcon={<StopOutlined />}
              onClick={() => void stop()}
              disabled={busy || seconds < 60}
            >
              Arrêter et enregistrer
            </Button>
          </>
        ) : (
          <Button
            variant="outlined"
            startIcon={<PlayArrowOutlined />}
            onClick={() => {
              const stamp = Date.now();
              localStorage.setItem(key, String(stamp));
              setStarted(stamp);
              setNow(stamp);
            }}
          >
            Démarrer le chronomètre
          </Button>
        )}
        {started && (
          <Typography variant="body2" color="text.secondary">
            Le compteur continue même si vous changez de page.{' '}
            {navigator.onLine
              ? 'L’arrêt enregistre le temps.'
              : 'L’arrêt sera synchronisé au retour du réseau.'}
          </Typography>
        )}
      </Stack>
      {error && (
        <Alert severity="warning" onClose={() => setError('')}>
          {error}
        </Alert>
      )}
    </Stack>
  );
}
