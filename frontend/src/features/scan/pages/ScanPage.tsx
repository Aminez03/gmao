import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { Alert, Button, Paper, Stack, TextField, Typography } from '@mui/material';
import { Html5Qrcode } from 'html5-qrcode';
import { api, Entity, errorText } from '../../../services/api';
import { findCachedAssetByQr } from '../../../services/offline';
import { useAuth } from '../../../app/Auth';
export default function ScanPage() {
  const { user } = useAuth();
  const [params] = useSearchParams();
  const [code, setCode] = useState(params.get('code') || '');
  const [asset, setAsset] = useState<Entity | null>(null);
  const [error, setError] = useState('');
  const [scanning, setScanning] = useState(false);
  const scanner = useRef<Html5Qrcode | null>(null);
  async function resolve(value: string) {
    try {
      let token = value;
      try {
        if (value.includes('code=')) token = new URL(value).searchParams.get('code') || '';
      } catch {
        /* QR payload can be a raw token. */
      }
      if (!navigator.onLine) {
        const local = await findCachedAssetByQr(user!.id, token);
        if (!local) throw new Error('Équipement absent des données téléchargées sur cet appareil.');
        setAsset(local);
        setError('');
        return;
      }
      const { data } = await api.get(`/assets/by-qr/${token}`);
      setAsset(data);
      setError('');
    } catch (e) {
      setError(errorText(e));
    }
  }
  useEffect(
    () => () => {
      const current = scanner.current;
      if (current?.isScanning) void current.stop().catch(() => {});
    },
    [],
  );
  return (
    <Stack spacing={3}>
      <div>
        <Typography variant="overline" color="text.secondary">
          Accès terrain
        </Typography>
        <Typography variant="h4">Scanner un équipement</Typography>
      </div>
      <Paper variant="outlined" sx={{ p: 3, maxWidth: 650 }}>
        <Stack spacing={2}>
          <Typography>Scannez le QR code ou saisissez son identifiant.</Typography>
          <div id="qr-reader" />
          <Button
            variant="contained"
            disabled={scanning}
            onClick={async () => {
              try {
                const reader = new Html5Qrcode('qr-reader');
                scanner.current = reader;
                setScanning(true);
                await reader.start(
                  { facingMode: 'environment' },
                  { fps: 10, qrbox: 220 },
                  async (value) => {
                    await reader.stop();
                    setScanning(false);
                    setCode(value);
                    await resolve(value);
                  },
                  () => {},
                );
              } catch (e) {
                setScanning(false);
                setError(
                  'Caméra indisponible ou autorisation refusée. Vous pouvez saisir le code ci-dessous.',
                );
              }
            }}
          >
            Ouvrir la caméra
          </Button>
          {scanning && (
            <Button
              onClick={async () => {
                await scanner.current?.stop();
                setScanning(false);
              }}
            >
              Arrêter le scan
            </Button>
          )}
          <TextField
            label="Identifiant ou lien du QR code"
            value={code}
            onChange={(e) => setCode(e.target.value)}
          />
          <Button onClick={() => resolve(code)} disabled={!code}>
            Consulter l’équipement
          </Button>
          {error && <Alert severity="error">{error}</Alert>}
          {asset && (
            <Stack spacing={1}>
              <Alert severity="success">
                <Typography fontWeight={600}>
                  {asset.code} · {asset.name}
                </Typography>
                <Typography>
                  {asset.category} · {asset.status}
                </Typography>
              </Alert>
              <Button component={Link} to={`/m/assets?id=${asset.id}`} variant="contained">
                Ouvrir la fiche équipement
              </Button>
            </Stack>
          )}
        </Stack>
      </Paper>
    </Stack>
  );
}
