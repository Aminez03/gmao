import { useState } from 'react';

import { Link } from 'react-router-dom';

import { useQuery } from '@tanstack/react-query';

import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
  LinearProgress,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { alpha } from '@mui/material/styles';

import Close from '@mui/icons-material/Close';

import EditOutlined from '@mui/icons-material/EditOutlined';
import ArticleOutlined from '@mui/icons-material/ArticleOutlined';
import CalendarMonthOutlined from '@mui/icons-material/CalendarMonthOutlined';
import InfoOutlined from '@mui/icons-material/InfoOutlined';
import QrCodeOutlined from '@mui/icons-material/QrCodeOutlined';

import { useAuth } from '../../app/Auth';
import { api, command, download, Entity, errorText, headers } from '../../services/api';
import { enqueue, fetchCached } from '../../services/offline';

import { labels, human, canCreate, Field, Resource, referenceRoutes } from '../domain';
import ResourceForm from '../ResourceForm';
import { actions, Action } from '../actions';

import { Status, display } from '../EntityDisplay';
import ReferenceValue from '../ReferenceValue';
import ChecklistExecution from './ChecklistExecution';
import Attachments from './Attachments';
import AssetHistory from './AssetHistory';

const hiddenFields = new Set(['answers', 'checklistSnapshot', 'steps']);
const longFields = new Set([
  'description',
  'comment',
  'report',
  'diagnostic',
  'cause',
  'solution',
  'reason',
  'justification',
  'scope',
  'qualityNote',
  'waitNote',
  'details',
]);

function DetailFields({
  title,
  icon,
  fields,
  resource,
  entity,
}: {
  title: string;
  icon: React.ReactNode;
  fields: Field[];
  resource: Resource;
  entity: Entity;
}) {
  if (!fields.length) return null;
  return (
    <Paper component="section" variant="outlined" sx={{ borderRadius: 3, overflow: 'hidden' }}>
      <Stack
        direction="row"
        alignItems="center"
        gap={1}
        sx={{ px: 2.5, py: 2, borderBottom: 1, borderColor: 'divider' }}
      >
        <Box sx={{ display: 'flex', color: 'primary.main' }}>{icon}</Box>
        <Typography component="h3" variant="subtitle1" fontWeight={700}>
          {title}
        </Typography>
      </Stack>
      <Box
        component="dl"
        sx={{
          m: 0,
          p: { xs: 2, sm: 2.5 },
          display: 'grid',
          gridTemplateColumns: { xs: 'minmax(0, 1fr)', sm: 'repeat(2, minmax(0, 1fr))' },
          gap: 2,
        }}
      >
        {fields.map((field) => (
          <Box
            key={field.name}
            sx={{
              minWidth: 0,
              p: 1.75,
              borderRadius: 2,
              bgcolor: (theme) => alpha(theme.palette.primary.main, 0.035),
              gridColumn:
                longFields.has(field.name) || ['x', 'j'].includes(field.type)
                  ? '1 / -1'
                  : undefined,
            }}
          >
            <Typography
              component="dt"
              variant="caption"
              color="text.secondary"
              sx={{ display: 'block', mb: 0.75, fontWeight: 600 }}
            >
              {labels[field.name] || human(field.name)}
            </Typography>
            <Box
              component="dd"
              sx={{
                m: 0,
                fontSize: '0.925rem',
                fontWeight: 500,
                lineHeight: 1.7,
                overflowWrap: 'anywhere',
                whiteSpace: 'pre-wrap',
              }}
            >
              {referenceRoutes[field.name] ? (
                <ReferenceValue
                  field={field.name}
                  value={entity[field.name]}
                  resource={resource}
                  entity={entity}
                />
              ) : ['status', 'priority', 'criticality'].includes(field.name) &&
                entity[field.name] ? (
                <Status value={entity[field.name]} />
              ) : typeof entity[field.name] === 'boolean' ? (
                <Chip
                  size="small"
                  variant="outlined"
                  label={display(entity[field.name])}
                  color={entity[field.name] ? 'success' : 'default'}
                />
              ) : (
                display(entity[field.name])
              )}
            </Box>
          </Box>
        ))}
      </Box>
    </Paper>
  );
}

export default function Detail({
  resource,
  id,
  onClose,
  onRefresh,
}: {
  resource: Resource;
  id: string;
  onClose: () => void;
  onRefresh: () => Promise<void>;
}) {
  const { user } = useAuth();
  const [action, setAction] = useState<Action | null>(null);
  const [edit, setEdit] = useState(false);
  const [notice, setNotice] = useState('');
  const [error, setError] = useState('');
  const {
    data: entity,
    refetch,
    error: loadError,
    isLoading,
  } = useQuery({
    queryKey: ['detail', resource.route, id, user!.id],
    queryFn: () => fetchCached(user!.id, `${resource.route}/${id}`, `/${resource.route}/${id}`),
  });
  const fields = resource.fields.filter(
    (field) => !hiddenFields.has(field.name) && entity?.[field.name] != null,
  );
  const dateFields = fields.filter((field) => ['t', 'd'].includes(field.type));
  const informationFields = fields.filter((field) => !['t', 'd'].includes(field.type));
  const availableActions = entity ? actions(resource.route, entity, user!.role) : [];
  const canEdit = canCreate(resource, user!.role);

  async function changed() {
    await refetch();
    await onRefresh();
  }

  return (
    <>
      <Dialog
        open
        onClose={onClose}
        fullWidth
        maxWidth="md"
        aria-labelledby="resource-detail-title"
        slotProps={{
          paper: {
            sx: {
              m: { xs: 1, sm: 3 },
              width: { xs: 'calc(100% - 16px)', sm: '100%' },
              maxHeight: { xs: 'calc(100% - 16px)', sm: 'calc(100% - 48px)' },
              borderRadius: 4,
            },
          },
        }}
      >
        <DialogTitle
          component="div"
          sx={{
            p: { xs: 2.5, sm: 3 },
            borderBottom: 1,
            borderColor: 'divider',
            background: (theme) =>
              `linear-gradient(120deg, ${alpha(theme.palette.primary.main, 0.1)}, ${alpha(theme.palette.primary.main, 0.02)})`,
          }}
        >
          <Stack direction="row" alignItems="flex-start" gap={2}>
            <Avatar
              variant="rounded"
              sx={{
                bgcolor: 'primary.main',
                width: 48,
                height: 48,
                borderRadius: 2.5,
                display: { xs: 'none', sm: 'flex' },
              }}
            >
              <ArticleOutlined />
            </Avatar>
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography
                variant="overline"
                color="text.secondary"
                sx={{ lineHeight: 1.5, letterSpacing: 1 }}
              >
                {human(resource.group)} · {resource.label}
              </Typography>
              <Typography
                id="resource-detail-title"
                component="h2"
                variant="h5"
                sx={{ mt: 0.5, overflowWrap: 'anywhere', lineHeight: 1.3 }}
              >
                {entity?.title || entity?.name || entity?.number || entity?.code || resource.label}
              </Typography>
              {entity && (
                <Stack direction="row" gap={1} flexWrap="wrap" alignItems="center" mt={1.5}>
                  {entity.status && <Status value={entity.status} />}
                  {entity.priority && <Status value={entity.priority} />}
                  {(entity.number || entity.code) && (
                    <Chip label={entity.number || entity.code} variant="outlined" size="small" />
                  )}
                  {entity.published && <Chip label="Gamme publiée" color="success" size="small" />}
                </Stack>
              )}
            </Box>
            <IconButton onClick={onClose} aria-label="Fermer">
              <Close />
            </IconButton>
          </Stack>
        </DialogTitle>
        <DialogContent sx={{ p: { xs: 2, sm: 3 }, bgcolor: 'background.default' }}>
          {!entity ? (
            <Box sx={{ py: 3 }}>
              {loadError ? (
                <Alert severity="error">{errorText(loadError)}</Alert>
              ) : (
                isLoading && (
                  <>
                    <Typography color="text.secondary" mb={2}>
                      Chargement de la fiche…
                    </Typography>
                    <LinearProgress aria-label="Chargement de la fiche" />
                  </>
                )
              )}
            </Box>
          ) : (
            <Stack spacing={2.5} sx={{ pt: 2.5 }}>
              {loadError && <Alert severity="error">{errorText(loadError)}</Alert>}
              {notice && <Alert onClose={() => setNotice('')}>{notice}</Alert>}
              {error && (
                <Alert severity="error" onClose={() => setError('')}>
                  {error}
                </Alert>
              )}
              {(canEdit || availableActions.length > 0 || resource.route === 'assets') && (
                <Paper variant="outlined" sx={{ p: 2, borderRadius: 3 }}>
                  <Typography
                    variant="overline"
                    color="text.secondary"
                    sx={{ display: 'block', mb: 1 }}
                  >
                    Actions disponibles
                  </Typography>
                  <Stack
                    direction="row"
                    gap={1}
                    flexWrap="wrap"
                    sx={{ '& > button': { flexGrow: { xs: 1, sm: 0 } } }}
                  >
                    {canEdit && (
                      <Button
                        variant="contained"
                        startIcon={<EditOutlined />}
                        onClick={() => setEdit(true)}
                        disabled={!navigator.onLine}
                      >
                        Modifier
                      </Button>
                    )}
                    {availableActions.map((a) => (
                      <Button
                        key={a.key}
                        variant="outlined"
                        size="small"
                        onClick={() => setAction(a)}
                        disabled={!navigator.onLine && a.key !== 'time'}
                      >
                        {a.label}
                      </Button>
                    ))}
                    {resource.route === 'assets' && (
                      <Button
                        startIcon={<QrCodeOutlined />}
                        variant="outlined"
                        onClick={() =>
                          download(`/assets/${id}/qr`, `${entity.code}-qr.png`).catch((e) =>
                            setError(errorText(e)),
                          )
                        }
                      >
                        Télécharger le QR code
                      </Button>
                    )}
                  </Stack>
                </Paper>
              )}
              {resource.route === 'notifications' && entity.resourceType && entity.resourceId && (
                <Button
                  component={Link}
                  to={`/m/${entity.resourceType}?id=${entity.resourceId}`}
                  onClick={onClose}
                >
                  Ouvrir la ressource associée
                </Button>
              )}
              <DetailFields
                title="Informations générales"
                icon={<InfoOutlined fontSize="small" />}
                fields={informationFields}
                resource={resource}
                entity={entity}
              />
              <DetailFields
                title="Dates et suivi"
                icon={<CalendarMonthOutlined fontSize="small" />}
                fields={dateFields}
                resource={resource}
                entity={entity}
              />
              {resource.route === 'checklists' &&
                entity.steps?.map((s: Entity) => (
                  <Paper variant="outlined" key={s.key} sx={{ p: 2.5, borderRadius: 3 }}>
                    <Typography>{s.label}</Typography>
                    <Typography color="text.secondary" variant="body2">
                      {human(s.type)} · {s.required ? 'Obligatoire' : 'Facultative'} {s.unit || ''}
                    </Typography>
                  </Paper>
                ))}
              {resource.route === 'work-orders' && (
                <Paper variant="outlined" sx={{ p: { xs: 2, sm: 2.5 }, borderRadius: 3 }}>
                  <ChecklistExecution order={entity} onRefresh={changed} />
                </Paper>
              )}
              {resource.route === 'assets' && (
                <Paper variant="outlined" sx={{ p: { xs: 2, sm: 2.5 }, borderRadius: 3 }}>
                  <AssetHistory id={id} />
                </Paper>
              )}
              {[
                'assets',
                'work-orders',
                'failure-reports',
                'external-interventions',
                'contracts',
              ].includes(resource.route) && (
                <Paper variant="outlined" sx={{ p: { xs: 2, sm: 2.5 }, borderRadius: 3 }}>
                  <Attachments resource={resource} entity={entity} />
                </Paper>
              )}
            </Stack>
          )}
        </DialogContent>
        <DialogActions
          sx={{
            px: { xs: 2, sm: 3 },
            py: 1.5,
            borderTop: 1,
            borderColor: 'divider',
            justifyContent: 'space-between',
            gap: 2,
          }}
        >
          <Box sx={{ minWidth: 0 }}>
            {entity?.updatedAt && (
              <Typography variant="caption" color="text.secondary" display="block">
                Mise à jour : {display(entity.updatedAt)}
              </Typography>
            )}
            {entity?.version != null && (
              <Typography variant="caption" color="text.secondary" display="block">
                Version {entity.version}
              </Typography>
            )}
          </Box>
          <Button onClick={onClose} variant="outlined">
            Fermer
          </Button>
        </DialogActions>
      </Dialog>

      {entity && (
        <Dialog
          open={!!action || edit}
          onClose={() => {
            setAction(null);
            setEdit(false);
          }}
          fullWidth
          maxWidth="sm"
        >
          <DialogTitle>{action?.label || 'Modifier la fiche'}</DialogTitle>
          <DialogContent sx={{ pt: '16px !important' }}>
            <ResourceForm
              key={action?.key || 'edit'}
              resource={action ? undefined : resource}
              fields={action?.fields}
              initial={action ? {} : entity}
              onCancel={() => {
                setAction(null);
                setEdit(false);
              }}
              onSave={async (fields) => {
                if (action) {
                  if (!navigator.onLine && action.key === 'time')
                    await enqueue(user!.id, entity, 'time', fields);
                  else await command(resource.route, entity, action.key, fields);
                } else
                  await api.put(
                    `/${resource.route}/${id}`,
                    { fields },
                    { headers: headers(entity.version) },
                  );
                setAction(null);
                setEdit(false);
                setNotice('Opération enregistrée.');
                await changed();
              }}
              submitLabel={action?.fields.length === 0 ? 'Confirmer' : 'Enregistrer'}
            />
          </DialogContent>
        </Dialog>
      )}
    </>
  );
}
