import { useState } from 'react';

import { useQuery } from '@tanstack/react-query';

import { Alert, Button, MenuItem, Stack, TextField, Typography } from '@mui/material';

import { api, download, Entity, errorText } from '../../services/api';
import { queuePhoto } from '../../services/offline';
import { useAuth } from '../../app/Auth';

import { Resource } from '../domain';

const attachmentResources = new Set([
  'assets',
  'work-orders',
  'failure-reports',
  'external-interventions',
  'contracts',
]);

export default function Attachments({ resource, entity }: { resource: Resource; entity: Entity }) {
  const { user } = useAuth();
  const supportsAttachments = attachmentResources.has(resource.route);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [category, setCategory] = useState('Avant intervention');
  const { data, refetch } = useQuery({
    queryKey: ['attachments', entity.id],
    queryFn: async () =>
      (await api.get('/attachments', { params: { search: entity.id, size: 100 } })).data,
    enabled: supportsAttachments && navigator.onLine,
  });
  if (!supportsAttachments) return null;

  return (
    <Stack spacing={1.5}>
      <Typography variant="h6">Documents et photos</Typography>
      {error && <Alert severity="error">{error}</Alert>}
      {message && <Alert severity="success">{message}</Alert>}
      {data?.items?.map((file: Entity) => (
        <Button
          key={file.id}
          sx={{ justifyContent: 'flex-start' }}
          onClick={() =>
            download(`/files/${file.id}/download`, file.name).catch((e) => setError(errorText(e)))
          }
        >
          {file.name}
        </Button>
      ))}
      <TextField
        select
        size="small"
        label="Type de preuve photo"
        value={category}
        onChange={(event) => setCategory(event.target.value)}
      >
        {['Avant intervention', 'Après intervention', 'Anomalie', 'Autre preuve'].map((item) => (
          <MenuItem key={item} value={item}>
            {item}
          </MenuItem>
        ))}
      </TextField>
      <Button component="label" variant="outlined">
        Ajouter une photo ou un document
        <input
          type="file"
          hidden
          accept="application/pdf,image/jpeg,image/png,image/webp"
          onChange={async (e) => {
            const file = e.target.files?.[0];
            if (!file) return;
            try {
              if (!navigator.onLine) {
                if (!file.type.startsWith('image/'))
                  throw new Error('Hors connexion, seuls les fichiers image sont mis en attente.');
                await queuePhoto(
                  user!.id,
                  entity,
                  new File([file], `${category} - ${file.name}`, { type: file.type }),
                  resource.route,
                );
                setMessage(
                  'Photo conservée sur cet appareil ; elle sera envoyée à la reconnexion.',
                );
              } else {
                const form = new FormData();
                form.append(
                  'file',
                  new File([file], `${category} - ${file.name}`, { type: file.type }),
                );
                form.append('targetType', resource.route);
                form.append('targetId', entity.id);
                await api.post('/files', form);
                await refetch();
                setMessage('Document ajouté à la fiche.');
              }
            } catch (ex) {
              setError(errorText(ex));
            }
          }}
        />
      </Button>
      <Typography variant="caption" color="text.secondary">
        PDF, JPEG, PNG ou WebP · 25 Mio maximum · photos mises en attente hors connexion
      </Typography>
    </Stack>
  );
}
