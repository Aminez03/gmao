import { useState } from 'react';

import { useQuery } from '@tanstack/react-query';

import { Alert, Button, Stack, Typography } from '@mui/material';

import { api, download, Entity, errorText } from '../../services/api';

import { Resource } from '../domain';

const attachmentResources = new Set([
  'assets',
  'work-orders',
  'failure-reports',
  'external-interventions',
  'contracts',
]);

export default function Attachments({ resource, entity }: { resource: Resource; entity: Entity }) {
  const supportsAttachments = attachmentResources.has(resource.route);
  const [error, setError] = useState('');
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
      <Button component="label" variant="outlined" disabled={!navigator.onLine}>
        Ajouter un document
        <input
          type="file"
          hidden
          accept="application/pdf,image/jpeg,image/png,image/webp"
          onChange={async (e) => {
            const file = e.target.files?.[0];
            if (!file) return;
            try {
              const form = new FormData();
              form.append('file', file);
              form.append('targetType', resource.route);
              form.append('targetId', entity.id);
              await api.post('/files', form);
              await refetch();
            } catch (ex) {
              setError(errorText(ex));
            }
          }}
        />
      </Button>
      <Typography variant="caption" color="text.secondary">
        PDF, JPEG, PNG ou WebP · 25 Mio maximum · accès privé
      </Typography>
    </Stack>
  );
}
