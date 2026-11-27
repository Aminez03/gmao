import { useEffect, useState } from 'react';

import { useLocation } from 'react-router-dom';

import { useQuery, useQueryClient } from '@tanstack/react-query';

import {
  Alert,
  Box,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  LinearProgress,
  MenuItem,
  Paper,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';

import Add from '@mui/icons-material/Add';

import Search from '@mui/icons-material/Search';

import Inventory2Outlined from '@mui/icons-material/Inventory2Outlined';

import { useAuth } from '../../app/Auth';
import { api, Entity, errorText, headers } from '../../services/api';
import { fetchCached } from '../../services/offline';

import { Resource, labels, human, canCreate, visible, isReferenceField } from '../domain';
import ResourceForm from '../ResourceForm';

import { Status, display } from '../EntityDisplay';
import ReferenceValue from '../ReferenceValue';
import { businessReference } from '../businessReference';
import Detail from './ResourceDetail';
export interface ResourcePageProps {
  resource: Resource;
  statusOptions?: readonly string[];
}

export default function ResourcePage({ resource, statusOptions }: ResourcePageProps) {
  const location = useLocation();
  const route = resource.route;
  const { user } = useAuth();
  const canView = !!user && visible(resource, user.role);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [create, setCreate] = useState(false);
  const [selected, setSelected] = useState<string | null>(null);
  const [notice, setNotice] = useState('');
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['list', route, page, size, search, status, user?.id],
    queryFn: () =>
      fetchCached(
        user!.id,
        `${route}?${page}:${size}:${search}:${status}`,
        `/${route}?page=${page}&size=${size}&search=${encodeURIComponent(search)}&status=${status}`,
      ),
    enabled: canView,
  });

  useEffect(() => {
    setPage(0);
    setSelected(new URLSearchParams(location.search).get('id'));
    setSearch('');
    setStatus('');
  }, [route, location.search]);

  if (!canView) return <Alert severity="warning">Module non accessible avec votre rôle.</Alert>;

  const preferred = [
    'number',
    'code',
    'title',
    'name',
    'status',
    'priority',
    'criticality',
    'dueAt',
    'startsAt',
    'stock',
    'reserved',
  ];

  const detailColumns = [...resource.fields]
    .sort(
      (a, b) =>
        (preferred.includes(a.name) ? preferred.indexOf(a.name) : 99) -
        (preferred.includes(b.name) ? preferred.indexOf(b.name) : 99),
    )
    .filter(
      (f) =>
        !['j', 'x'].includes(f.type) &&
        !['archived', 'createdBy', 'contentType', 'size', 'checklistSnapshot', 'answers'].includes(
          f.name,
        ),
    )
    .slice(0, 6);

  const columns = [
    { name: 'displayNumber', type: 's', required: false },
    ...detailColumns.filter((field) => !['id', 'displayNumber', 'number'].includes(field.name)),
  ];

  async function refresh() {
    await queryClient.invalidateQueries();
  }

  return (
    <>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        gap={2}
        alignItems={{ sm: 'center' }}
        justifyContent="space-between"
        mb={3}
      >
        <Box>
          <Typography variant="overline" color="text.secondary">
            {human(resource.group)}
          </Typography>
          <Typography variant="h4">{resource.label}</Typography>
          <Typography color="text.secondary" mt={1}>
            Référentiel partagé et suivi de l’activité technique.
          </Typography>
        </Box>
        {canCreate(resource, user!.role) && (
          <Button
            variant="contained"
            startIcon={<Add />}
            onClick={() => setCreate(true)}
            disabled={!navigator.onLine}
          >
            Ajouter
          </Button>
        )}
      </Stack>
      {notice && (
        <Alert onClose={() => setNotice('')} sx={{ mb: 2 }}>
          {notice}
        </Alert>
      )}
      {!navigator.onLine && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          Données téléchargées sur cet appareil. Les nouvelles affectations et décisions nécessitent
          une connexion.
        </Alert>
      )}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {errorText(error)}
        </Alert>
      )}
      <Paper variant="outlined">
        <Stack direction={{ xs: 'column', sm: 'row' }} gap={2} p={2.5}>
          <TextField
            placeholder="Rechercher dans ce module"
            size="small"
            value={search}
            onChange={(e) => {
              setSearch(e.target.value);
              setPage(0);
            }}
            slotProps={{
              input: { startAdornment: <Search sx={{ mr: 1, color: 'text.secondary' }} /> },
            }}
            sx={{ minWidth: { sm: 300 } }}
          />
          {resource.fields.some((f) => f.name === 'status') && (
            <TextField
              select={!!statusOptions}
              size="small"
              label="État"
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setPage(0);
              }}
              sx={{ minWidth: 180 }}
            >
              <MenuItem value="">Tous les états</MenuItem>
              {statusOptions?.map((state) => (
                <MenuItem key={state} value={state}>
                  {human(state)}
                </MenuItem>
              ))}
            </TextField>
          )}
          <Typography sx={{ ml: { sm: 'auto' }, alignSelf: 'center' }} color="text.secondary">
            {data?.totalElements || 0} résultat(s)
          </Typography>
        </Stack>
        {isLoading && <LinearProgress />}
        <TableContainer>
          <Table>
            <TableHead>
              <TableRow>
                {columns.map((f) => (
                  <TableCell key={f.name} scope="col">
                    {labels[f.name] || human(f.name)}
                  </TableCell>
                ))}
                <TableCell align="right">Détails</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {data?.items?.map((row: Entity) => (
                <TableRow
                  hover
                  key={row.id}
                  sx={{ cursor: 'pointer' }}
                  onClick={() => setSelected(row.id)}
                  tabIndex={0}
                  onKeyDown={(e) => e.key === 'Enter' && setSelected(row.id)}
                >
                  {columns.map((f, index) => (
                    <TableCell
                      key={f.name}
                      component={index === 0 ? 'th' : 'td'}
                      scope={index === 0 ? 'row' : undefined}
                    >
                      {index === 0 ? (
                        <Tooltip title={businessReference(row)} arrow>
                          <Box
                            component="span"
                            sx={{
                              fontWeight: 700,
                              whiteSpace: 'nowrap',
                              fontVariantNumeric: 'tabular-nums',
                            }}
                          >
                            {businessReference(row)}
                          </Box>
                        </Tooltip>
                      ) : ['status', 'priority', 'criticality'].includes(f.name) && row[f.name] ? (
                        <Status value={row[f.name]} />
                      ) : isReferenceField(f.name) ? (
                        <ReferenceValue
                          field={f.name}
                          value={row[f.name]}
                          resource={resource}
                          entity={row}
                        />
                      ) : (
                        display(row[f.name])
                      )}
                    </TableCell>
                  ))}
                  <TableCell align="right">
                    <Button size="small">Ouvrir</Button>
                  </TableCell>
                </TableRow>
              ))}
              {!isLoading && !data?.items?.length && (
                <TableRow>
                  <TableCell colSpan={columns.length + 1}>
                    <Stack
                      alignItems="center"
                      py={6}
                      spacing={1}
                      sx={{
                        position: { xs: 'sticky', sm: 'static' },
                        left: 0,
                        width: { xs: 'calc(100vw - 66px)', sm: '100%' },
                        px: 2,
                        textAlign: 'center',
                      }}
                    >
                      <Inventory2Outlined sx={{ fontSize: 42, color: 'text.disabled' }} />
                      <Typography variant="h6">Aucun élément pour le moment</Typography>
                      <Typography color="text.secondary">
                        Ajoutez votre première entrée ou ajustez les filtres.
                      </Typography>
                    </Stack>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={data?.totalElements || 0}
          page={page}
          rowsPerPage={size}
          rowsPerPageOptions={[10, 25, 50]}
          onPageChange={(_, n) => setPage(n)}
          onRowsPerPageChange={(e) => {
            setSize(Number(e.target.value));
            setPage(0);
          }}
          labelRowsPerPage="Lignes par page"
        />
      </Paper>

      <Dialog open={create} onClose={() => setCreate(false)} fullWidth maxWidth="sm">
        <DialogTitle>Ajouter · {resource.label}</DialogTitle>
        <DialogContent sx={{ pt: '16px !important' }}>
          <ResourceForm
            key={route}
            resource={resource}
            onCancel={() => setCreate(false)}
            onSave={async (fields) => {
              await api.post(`/${route}`, { fields }, { headers: headers() });
              setCreate(false);
              setNotice('Entrée enregistrée.');
              await refresh();
            }}
          />
        </DialogContent>
      </Dialog>

      {selected && (
        <Detail
          resource={resource}
          id={selected}
          onClose={() => setSelected(null)}
          onRefresh={refresh}
        />
      )}
    </>
  );
}
