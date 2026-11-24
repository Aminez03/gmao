import { useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { Alert, Button, Paper, Stack, TextField, Typography } from '@mui/material';
import { Html5Qrcode } from 'html5-qrcode';
import { api, Entity, errorText } from '../../../services/api';
export default function ScanPage() {
  const [params] = useSearchParams();
  const [code, setCode] = useState(params.get('code') || '');
  const [asset, setAsset] = useState<Entity | null>(null);
  const [error, setError] = useState('');
  const [scanning, setScanning] = useState(false);
  const scanner = useRef<Html5Qrcode | null>(null);
  async function resolve(value: string) {
    try {
      const token = value.includes('code=') ? new URL(value).searchParams.get('code') : value;
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
            disabled={scanning || !navigator.onLine}
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
          <Button onClick={() => resolve(code)} disabled={!code || !navigator.onLine}>
            Consulter l’équipement
          </Button>
          {error && <Alert severity="error">{error}</Alert>}
          {asset && (
            <Alert severity="success">
              <Typography fontWeight={600}>
                {asset.code} · {asset.name}
              </Typography>
              <Typography>
                {asset.category} · {asset.status}
              </Typography>
            </Alert>
          )}
        </Stack>
      </Paper>
    </Stack>
  );
}
