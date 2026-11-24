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
  Divider,
  IconButton,
  InputAdornment,
  Paper,
  Skeleton,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import AddRounded from '@mui/icons-material/AddRounded';
import CloseRounded from '@mui/icons-material/CloseRounded';
import EditOutlined from '@mui/icons-material/EditOutlined';
import MailOutlineRounded from '@mui/icons-material/MailOutlineRounded';
import SearchRounded from '@mui/icons-material/SearchRounded';
import ShieldOutlined from '@mui/icons-material/ShieldOutlined';
import PeopleOutlineRounded from '@mui/icons-material/PeopleOutlineRounded';
import ResourceForm from '../../../components/ResourceForm';
import { Field, human } from '../../../components/domain';
import { api, headers, Entity, errorText } from '../../../services/api';
import { useAuth } from '../../../app/Auth';
export default function UsersPage() {
  const { user } = useAuth();
  const canView = !!user && ['ADMIN', 'RESPONSABLE_TECHNIQUE'].includes(user.role);
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['users', user?.id],
    enabled: canView,
    queryFn: async () => (await api.get('/users')).data,
  });
  const [editing, setEditing] = useState<Entity | null>(null);
  const [search, setSearch] = useState('');
  const accounts: Entity[] = data?.items || [];
  const searchTerm = search.trim().toLocaleLowerCase('fr');
  const filteredAccounts = accounts.filter((account) =>
    [account.name, account.email, human(account.role)].some((value) =>
      String(value || '')
        .toLocaleLowerCase('fr')
        .includes(searchTerm),
    ),
  );
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
        {user!.role === 'ADMIN' && (
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
          placeholder="Nom, e-mail ou rôle"
          value={search}
          onChange={(event) => setSearch(event.target.value)}
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
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: 'minmax(0, 1fr)',
            md: 'repeat(2, minmax(0, 1fr))',
            xl: 'repeat(3, minmax(0, 1fr))',
          },
          gap: 2.5,
        }}
      >
        {isLoading
          ? [0, 1, 2].map((key) => (
              <Paper variant="outlined" key={key} sx={{ p: 3, borderRadius: 3 }}>
                <Skeleton variant="circular" width={54} height={54} />
                <Skeleton width="65%" height={36} sx={{ mt: 2 }} />
                <Skeleton width="85%" />
                <Skeleton height={48} sx={{ mt: 2 }} />
              </Paper>
            ))
          : filteredAccounts.map((account) => {
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
                <Paper
                  component="article"
                  variant="outlined"
                  key={account.id}
                  sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    minWidth: 0,
                    overflow: 'hidden',
                    borderRadius: 3,
                    transition: 'border-color .2s ease',
                    borderColor: isCurrent ? 'primary.main' : undefined,
                    '&:hover': { borderColor: 'primary.main' },
                  }}
                >
                  <Box
                    sx={{
                      height: 5,
                      bgcolor: (theme) =>
                        alpha(
                          account.active ? theme.palette.primary.main : theme.palette.text.disabled,
                          0.6,
                        ),
                    }}
                  />
                  <Box sx={{ p: 2.5, flex: 1 }}>
                    <Stack
                      direction="row"
                      justifyContent="space-between"
                      alignItems="flex-start"
                      gap={1.5}
                      mb={2}
                    >
                      <Avatar
                        sx={{
                          width: 56,
                          height: 56,
                          borderRadius: 3,
                          fontSize: '1.1rem',
                          fontWeight: 700,
                          bgcolor: (theme) => alpha(theme.palette.primary.main, 0.1),
                          color: 'primary.main',
                        }}
                        variant="rounded"
                      >
                        {initials}
                      </Avatar>
                      <Chip
                        size="small"
                        label={account.active ? 'Actif' : 'Désactivé'}
                        color={account.active ? 'success' : 'default'}
                        variant="outlined"
                      />
                    </Stack>
                    <Stack direction="row" alignItems="center" gap={1} mb={0.75} flexWrap="wrap">
                      <Typography component="h3" variant="h6" sx={{ overflowWrap: 'anywhere' }}>
                        {account.name || 'Utilisateur sans nom'}
                      </Typography>
                      {isCurrent && (
                        <Chip
                          label="Vous"
                          size="small"
                          color="primary"
                          sx={{ height: 22, fontSize: '0.7rem' }}
                        />
                      )}
                    </Stack>
                    <Stack
                      direction="row"
                      gap={1}
                      alignItems="center"
                      sx={{ color: 'text.secondary', minWidth: 0 }}
                    >
                      <MailOutlineRounded sx={{ fontSize: 17, flexShrink: 0 }} />
                      <Typography variant="body2" sx={{ overflowWrap: 'anywhere' }}>
                        {account.email}
                      </Typography>
                    </Stack>
                    <Box
                      sx={{
                        mt: 2.5,
                        p: 1.5,
                        borderRadius: 2,
                        bgcolor: (theme) => alpha(theme.palette.primary.main, 0.035),
                      }}
                    >
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        sx={{ display: 'block', mb: 0.5 }}
                      >
                        Rôle et permissions
                      </Typography>
                      <Stack direction="row" gap={1} alignItems="center">
                        <ShieldOutlined sx={{ fontSize: 18, color: 'primary.main' }} />
                        <Typography variant="body2" fontWeight={600}>
                          {human(account.role)}
                        </Typography>
                      </Stack>
                    </Box>
                  </Box>
                  {user!.role === 'ADMIN' && (
                    <>
                      <Divider />
                      <Box sx={{ px: 2.5, py: 1.5 }}>
                        <Button
                          variant="outlined"
                          fullWidth
                          startIcon={<EditOutlined />}
                          onClick={() => setEditing(account)}
                          aria-label={`Modifier le compte de ${account.name}`}
                          sx={{ borderRadius: 2 }}
                        >
                          Modifier le compte
                        </Button>
                      </Box>
                    </>
                  )}
                </Paper>
              );
            })}
      </Box>
      {!isLoading && !error && !filteredAccounts.length && (
        <Paper variant="outlined" sx={{ p: 5, textAlign: 'center', borderRadius: 3 }}>
          <PeopleOutlineRounded sx={{ fontSize: 42, color: 'text.disabled', mb: 1 }} />
          <Typography variant="h6">
            {searchTerm ? 'Aucun utilisateur trouvé' : 'Aucun compte pour le moment'}
          </Typography>
          <Typography color="text.secondary" mt={1}>
            {searchTerm
              ? 'Essayez un autre nom, une adresse e-mail ou un rôle.'
              : 'Les comptes de votre équipe apparaîtront ici.'}
          </Typography>
          {searchTerm && (
            <Button onClick={() => setSearch('')} sx={{ mt: 2 }}>
              Effacer la recherche
            </Button>
          )}
        </Paper>
      )}
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
