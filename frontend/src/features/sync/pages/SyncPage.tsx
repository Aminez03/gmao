import { useEffect, useState } from 'react';
import {
  Alert,
  Button,
  Dialog,
  DialogContent,
  DialogTitle,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { useAuth } from '../../../app/Auth';
import { api, Entity, errorText } from '../../../services/api';
import { pending, uploads, synchronize, resolveOperation } from '../../../services/offline';
import { human } from '../../../components/domain';
export default function SyncPage() {
  const { user } = useAuth();
  const [items, setItems] = useState<Entity[]>([]);
  const [photos, setPhotos] = useState(0);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [compare, setCompare] = useState<{ op: Entity; server: Entity } | null>(null);
  async function reload() {
    setItems(await pending(user!.id));
    setPhotos((await uploads(user!.id)).length);
  }
  useEffect(() => {
    void reload();
  }, [user]);
  async function sync() {
    setBusy(true);
    setError('');
    try {
      await synchronize(user!.id);
      await reload();
      setMessage('Synchronisation terminée. Les conflits éventuels restent à traiter.');
    } catch (e) {
      setError(errorText(e));
    } finally {
      setBusy(false);
    }
  }
  return (
    <Stack spacing={3}>
      <div>
        <Typography variant="overline" color="text.secondary">
          Continuité mobile
        </Typography>
        <Typography variant="h4">Synchronisation</Typography>
        <Typography color="text.secondary" mt={1}>
          Suivez vos saisies locales et comparez les modifications concurrentes.
        </Typography>
      </div>
      <Alert severity="info">
        Ne supprimez pas les données du navigateur tant que des saisies restent en attente. La
        validation finale et les mouvements de stock nécessitent une connexion.
      </Alert>
      {message && <Alert>{message}</Alert>}
      {error && <Alert severity="error">{error}</Alert>}
      <Paper variant="outlined" sx={{ p: 3 }}>
        <Stack direction="row" justifyContent="space-between" alignItems="center">
          <div>
            <Typography variant="h6">
              {items.length} opération(s) · {photos} photo(s)
            </Typography>
            <Typography color="text.secondary">
              {navigator.onLine ? 'Réseau disponible' : 'Hors connexion'}
            </Typography>
          </div>
          <Button variant="contained" onClick={sync} disabled={busy || !navigator.onLine}>
            {busy ? 'Synchronisation…' : 'Synchroniser maintenant'}
          </Button>
        </Stack>
      </Paper>
      {items.map((op) => (
        <Paper key={op.operationId} variant="outlined" sx={{ p: 3 }}>
          <Stack spacing={1}>
            <Typography fontWeight={600}>
              {op.action === 'answers' ? 'Réponses à la checklist' : 'Temps d’intervention'} ·{' '}
              {op.resourceId.slice(0, 8)}
            </Typography>
            <Typography color="text.secondary">
              {human(op.state)} · {new Date(op.createdAt).toLocaleString('fr-TN')}
            </Typography>
            {op.detail && <Alert severity="warning">{op.detail}</Alert>}
            {op.state !== 'PENDING' && (
              <Button
                disabled={!navigator.onLine}
                onClick={async () => {
                  try {
                    const { data } = await api.get(`/work-orders/${op.resourceId}`);
                    setCompare({ op, server: data });
                  } catch (e) {
                    setError(errorText(e));
                  }
                }}
              >
                Comparer et résoudre
              </Button>
            )}
          </Stack>
        </Paper>
      ))}
      <Dialog open={!!compare} onClose={() => setCompare(null)} fullWidth maxWidth="md">
        <DialogTitle>Comparer les deux versions</DialogTitle>
        <DialogContent>
          {compare && (
            <Stack spacing={2}>
              <Typography>
                Le serveur possède la version {compare.server.version}. Votre saisie locale reste
                conservée.
              </Typography>
              <Typography fontWeight={600}>Saisie sur cet appareil</Typography>
              <pre className="comparison">{JSON.stringify(compare.op.payload, null, 2)}</pre>
              <Typography fontWeight={600}>Données actuelles du serveur</Typography>
              <pre className="comparison">
                {JSON.stringify(
                  compare.op.action === 'answers' ? compare.server.answers : compare.server,
                  null,
                  2,
                )}
              </pre>
              <Alert severity="warning">
                Vérifiez les valeurs avant de proposer à nouveau votre saisie. Les autres opérations
                du même équipement doivent être examinées dans leur ordre.
              </Alert>
              <Button
                variant="contained"
                onClick={async () => {
                  await resolveOperation(user!.id, compare.op, compare.server.version);
                  setCompare(null);
                  await reload();
                }}
              >
                Conserver ma proposition sur la version actuelle
              </Button>
            </Stack>
          )}
        </DialogContent>
      </Dialog>
    </Stack>
  );
}
