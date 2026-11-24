import { useState } from 'react';

import { Link } from 'react-router-dom';

import { useQuery } from '@tanstack/react-query';

import {
  Alert,
  Button,
  LinearProgress,
  Paper,
  Stack,
  TablePagination,
  Typography,
} from '@mui/material';

import { useAuth } from '../../app/Auth';
import { Entity, errorText } from '../../services/api';
import { fetchCached } from '../../services/offline';

import { Status, display } from '../EntityDisplay';
export default function AssetHistory({ id }: { id: string }) {
  const { user } = useAuth();
  const [page, setPage] = useState(0);

  const { data, error, isLoading } = useQuery({
    queryKey: ['asset-history', id, page, user!.id],
    queryFn: () =>
      fetchCached(
        user!.id,
        `asset-history/${id}/${page}`,
        `/work-orders?search=${id}&size=10&page=${page}`,
      ),
  });

  return (
    <Stack spacing={1.5}>
      <Typography variant="h6">Historique des interventions</Typography>
      {error && <Alert severity="error">{errorText(error)}</Alert>}
      {isLoading && <LinearProgress />}
      {data?.items?.map((work: Entity) => (
        <Paper key={work.id} variant="outlined" sx={{ p: 2 }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} gap={1} justifyContent="space-between">
            <Button
              component={Link}
              to={`/m/work-orders?id=${work.id}`}
              sx={{ justifyContent: 'flex-start' }}
            >
              {work.number} · {work.title}
            </Button>
            <Status value={work.status} />
          </Stack>
          <Typography variant="body2" color="text.secondary">
            {display(work.createdAt)} · {work.actualMinutes || 0} min
          </Typography>
        </Paper>
      ))}
      {!isLoading && !data?.items?.length && (
        <Typography color="text.secondary">
          Aucune intervention accessible pour cet équipement.
        </Typography>
      )}
      <TablePagination
        component="div"
        count={data?.totalElements || 0}
        page={page}
        rowsPerPage={10}
        rowsPerPageOptions={[10]}
        onPageChange={(_, value) => setPage(value)}
        labelRowsPerPage="Lignes par page"
      />
    </Stack>
  );
}
