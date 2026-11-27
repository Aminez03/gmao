import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  Dialog,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  Paper,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import AddRounded from '@mui/icons-material/AddRounded';
import CloseRounded from '@mui/icons-material/CloseRounded';
import EditOutlined from '@mui/icons-material/EditOutlined';
import SearchRounded from '@mui/icons-material/SearchRounded';
import ShieldOutlined from '@mui/icons-material/ShieldOutlined';
import PeopleOutlineRounded from '@mui/icons-material/PeopleOutlineRounded';
import ResourceForm from '../../../components/ResourceForm';
import { Field, human } from '../../../components/domain';
import { businessReference } from '../../../components/businessReference';
import { api, headers, Entity, errorText } from '../../../services/api';
import { useAuth } from '../../../app/Auth';
export default function UsersPage() {
  const { user } = useAuth();
  const canView = !!user && ['ADMIN', 'RESPONSABLE_TECHNIQUE'].includes(user.role);
  const canManage = user?.role === 'ADMIN';
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['users', user?.id],
    enabled: canView,
    queryFn: async () => (await api.get('/users')).data,
  });
  const [editing, setEditing] = useState<Entity | null>(null);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const accounts: Entity[] = data?.items || [];
  const searchTerm = search.trim().toLocaleLowerCase('fr');
  const filteredAccounts = accounts.filter((account) =>
    [account.displayNumber, account.name, account.email, human(account.role)].some((value) =>
      String(value || '')
        .toLocaleLowerCase('fr')
        .includes(searchTerm),
    ),
  );
  const currentPage = Math.min(
    page,
    Math.max(0, Math.ceil(filteredAccounts.length / rowsPerPage) - 1),
  );
  const visibleAccounts = filteredAccounts.slice(
    currentPage * rowsPerPage,
    (currentPage + 1) * rowsPerPage,
  );
  const columnCount = canManage ? 6 : 5;
  const fields: Field[] = [
    { name: 'name', type: 's', required: true },
    { name: 'email', type: 's', required: true },
    { name: 'role', type: 's', required: true },
    { name: 'contractorId', type: 'u', required: false },
    { name: 'active', type: 'b', required: false },
    { name: 'password', type: 's', required: !editing?.id },
  ];
  if (!canView) return <Alert severity="warning">Accès réservé à la supervision.</Alert>;
  return (
    <Stack spacing={3}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        gap={2}
        justifyContent="space-between"
        alignItems={{ sm: 'center' }}
      >
        <Box>
          <Typography variant="overline" color="text.secondary">
            Administration · Équipe
          </Typography>
          <Typography variant="h4">Utilisateurs et accès</Typography>
          <Typography color="text.secondary" mt={1}>
            Retrouvez les membres de votre équipe et leurs droits d’accès.
          </Typography>
        </Box>
        {canManage && (
          <Button
            startIcon={<AddRounded />}
            variant="contained"
            sx={{ alignSelf: { xs: 'flex-start', sm: 'center' }, flexShrink: 0, borderRadius: 2.5 }}
            onClick={() => setEditing({ role: 'TECHNICIEN' })}
          >
            Créer un compte
          </Button>
        )}
      </Stack>
      {error && <Alert severity="error">{errorText(error)}</Alert>}
      <Alert severity="info" icon={<ShieldOutlined />} sx={{ borderRadius: 3 }}>
        Les changements de droits révoquent les sessions. Les comptes prestataires sont associés à
        leur entreprise.
      </Alert>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        gap={2}
        alignItems={{ sm: 'center' }}
        justifyContent="space-between"
      >
        <Stack direction="row" gap={1} alignItems="center">
          <PeopleOutlineRounded color="primary" />
          <Typography variant="h6">Membres de l’équipe</Typography>
          {!isLoading && !error && <Chip size="small" label={accounts.length} variant="outlined" />}
        </Stack>
        <TextField
          label="Rechercher un utilisateur"
          placeholder="Numéro, nom, e-mail ou rôle"
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
            setPage(0);
          }}
          size="small"
          sx={{ width: { xs: '100%', sm: 300 } }}
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchRounded fontSize="small" />
                </InputAdornment>
              ),
            },
          }}
        />
      </Stack>
      <Paper variant="outlined" sx={{ overflow: 'hidden', borderRadius: 3 }}>
        <TableContainer aria-busy={isLoading}>
          <Table aria-label="Utilisateurs et accès" sx={{ minWidth: canManage ? 900 : 750 }}>
            <TableHead>
              <TableRow>
                <TableCell scope="col">Numéro</TableCell>
                <TableCell scope="col">Utilisateur</TableCell>
                <TableCell scope="col">Adresse e-mail</TableCell>
                <TableCell scope="col">Rôle</TableCell>
                <TableCell scope="col">État</TableCell>
                {canManage && (
                  <TableCell align="right" scope="col">
                    Actions
                  </TableCell>
                )}
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading
                ? [0, 1, 2, 3, 4].map((row) => (
                    <TableRow key={row}>
                      {Array.from({ length: columnCount }, (_, column) => (
                        <TableCell key={column}>
                          <Skeleton height={32} />
                        </TableCell>
                      ))}
                    </TableRow>
                  ))
                : visibleAccounts.map((account) => {
                    const isCurrent = account.id === user!.id;
                    const initials =
                      String(account.name || '')
                        .trim()
                        .split(/\s+/)
                        .filter(Boolean)
                        .slice(0, 2)
                        .map((part) => part[0])
                        .join('')
                        .toLocaleUpperCase('fr') || '?';
                    return (
                      <TableRow
                        hover
                        key={account.id}
                        sx={{
                          bgcolor: (theme) =>
                            isCurrent ? alpha(theme.palette.primary.main, 0.04) : undefined,
                        }}
                      >
                        <TableCell
                          component="th"
                          scope="row"
                          sx={{
                            fontWeight: 700,
                            whiteSpace: 'nowrap',
                            fontVariantNumeric: 'tabular-nums',
                          }}
                        >
                          {businessReference(account)}
                        </TableCell>
                        <TableCell>
                          <Stack direction="row" alignItems="center" gap={1.5}>
                            <Avatar
                              sx={{
                                width: 36,
                                height: 36,
                                fontSize: '0.8rem',
                                fontWeight: 700,
                                bgcolor: (theme) => alpha(theme.palette.primary.main, 0.1),
                                color: 'primary.main',
                              }}
                            >
                              {initials}
                            </Avatar>
                            <Typography
                              variant="body2"
                              fontWeight={600}
                              sx={{ whiteSpace: 'nowrap' }}
                            >
                              {account.name || 'Utilisateur sans nom'}
                            </Typography>
                            {isCurrent && <Chip label="Vous" size="small" color="primary" />}
                          </Stack>
                        </TableCell>
                        <TableCell sx={{ color: 'text.secondary' }}>
                          {account.email || '—'}
                        </TableCell>
                        <TableCell>
                          <Chip label={human(account.role)} size="small" variant="outlined" />
                        </TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={account.active ? 'Actif' : 'Désactivé'}
                            color={account.active ? 'success' : 'default'}
                            variant="outlined"
                          />
                        </TableCell>
                        {canManage && (
                          <TableCell align="right">
                            <Button
                              size="small"
                              variant="outlined"
                              startIcon={<EditOutlined />}
                              onClick={() => setEditing(account)}
                              aria-label={`Modifier le compte de ${account.name}`}
                              sx={{ whiteSpace: 'nowrap' }}
                            >
                              Modifier
                            </Button>
                          </TableCell>
                        )}
                      </TableRow>
                    );
                  })}
              {!isLoading && !error && !filteredAccounts.length && (
                <TableRow>
                  <TableCell colSpan={columnCount} sx={{ p: 0 }}>
                    <Stack
                      alignItems="center"
                      sx={{
                        py: 5,
                        px: 2,
                        textAlign: 'center',
                        position: { xs: 'sticky', sm: 'static' },
                        left: 0,
                        width: { xs: 'calc(100vw - 66px)', sm: '100%' },
                      }}
                    >
                      <PeopleOutlineRounded sx={{ fontSize: 42, color: 'text.disabled', mb: 1 }} />
                      <Typography variant="h6">
                        {searchTerm ? 'Aucun utilisateur trouvé' : 'Aucun compte pour le moment'}
                      </Typography>
                      <Typography color="text.secondary" mt={1}>
                        {searchTerm
                          ? 'Essayez un autre numéro, un nom, une adresse e-mail ou un rôle.'
                          : 'Les comptes de votre équipe apparaîtront ici.'}
                      </Typography>
                      {searchTerm && (
                        <Button onClick={() => setSearch('')} sx={{ mt: 2 }}>
                          Effacer la recherche
                        </Button>
                      )}
                    </Stack>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={filteredAccounts.length}
          page={currentPage}
          rowsPerPage={rowsPerPage}
          rowsPerPageOptions={[10, 25, 50]}
          onPageChange={(_, nextPage) => setPage(nextPage)}
          onRowsPerPageChange={(event) => {
            setRowsPerPage(Number(event.target.value));
            setPage(0);
          }}
          labelRowsPerPage="Lignes par page"
          labelDisplayedRows={({ from, to, count }) => `${from}–${to} sur ${count}`}
          getItemAriaLabel={(type) =>
            ({
              first: 'Première page',
              last: 'Dernière page',
              next: 'Page suivante',
              previous: 'Page précédente',
            })[type]
          }
        />
      </Paper>
      <Dialog
        open={!!editing}
        onClose={() => setEditing(null)}
        maxWidth="sm"
        fullWidth
        aria-labelledby="user-form-title"
        slotProps={{ paper: { sx: { borderRadius: 3 } } }}
      >
        <DialogTitle component="div" sx={{ borderBottom: 1, borderColor: 'divider', mb: 1 }}>
          <Stack direction="row" gap={2} alignItems="center" justifyContent="space-between">
            <Box>
              <Typography id="user-form-title" component="h2" variant="h6">
                {editing?.id ? 'Modifier le compte' : 'Créer un compte'}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {editing?.id
                  ? editing.name
                  : 'Renseignez le profil et les accès du nouveau membre.'}
              </Typography>
            </Box>
            <IconButton aria-label="Fermer" onClick={() => setEditing(null)}>
              <CloseRounded />
            </IconButton>
          </Stack>
        </DialogTitle>
        <DialogContent sx={{ pt: '16px !important' }}>
          {editing && (
            <ResourceForm
              fields={fields}
              initial={editing}
              onCancel={() => setEditing(null)}
              onSave={async (values) => {
                if (editing.id)
                  await api.put(`/users/${editing.id}`, values, {
                    headers: headers(editing.version),
                  });
                else await api.post('/users', values, { headers: headers() });
                setEditing(null);
                await refetch();
              }}
            />
          )}
        </DialogContent>
      </Dialog>
    </Stack>
  );
}
