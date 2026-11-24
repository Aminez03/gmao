import {
  Accordion,
  AccordionDetails,
  AccordionSummary,
  Alert,
  Avatar,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  Paper,
  Stack,
  Typography,
} from '@mui/material';
import { alpha } from '@mui/material/styles';
import CloseRounded from '@mui/icons-material/CloseRounded';
import ExpandMoreRounded from '@mui/icons-material/ExpandMoreRounded';
import LockOutlined from '@mui/icons-material/LockOutlined';
import LogoutOutlined from '@mui/icons-material/LogoutOutlined';
import MailOutlineRounded from '@mui/icons-material/MailOutlineRounded';
import ShieldOutlined from '@mui/icons-material/ShieldOutlined';
import { useAuth } from '../../app/Auth';
import { human } from '../../components/domain';
import ResourceForm from '../../components/ResourceForm';
import { api } from '../../services/api';

export default function ProfileDialog({
  open,
  onClose,
  onLogout,
  online,
}: {
  open: boolean;
  onClose: () => void;
  onLogout: () => void;
  online: boolean;
}) {
  const { user } = useAuth();
  if (!user) return null;
  const initials =
    user.name
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0])
      .join('')
      .toLocaleUpperCase('fr') || '?';

  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="sm"
      aria-labelledby="profile-dialog-title"
      slotProps={{
        paper: {
          sx: {
            m: { xs: 1, sm: 3 },
            width: { xs: 'calc(100% - 16px)', sm: '100%' },
            maxHeight: 'calc(100% - 32px)',
            borderRadius: 4,
          },
        },
      }}
    >
      <DialogTitle component="div" sx={{ p: 0 }}>
        <Box
          sx={{
            position: 'relative',
            overflow: 'hidden',
            px: { xs: 2.5, sm: 3.5 },
            pt: 2.5,
            pb: 4,
            color: 'white',
            background: 'linear-gradient(115deg, #12394b, #087f8c)',
            '&::after': {
              content: '""',
              position: 'absolute',
              width: 190,
              height: 190,
              border: '28px solid rgba(255,255,255,.05)',
              borderRadius: '50%',
              right: -45,
              bottom: -80,
              pointerEvents: 'none',
            },
          }}
        >
          <Stack direction="row" alignItems="center" justifyContent="space-between" mb={2.5}>
            <Typography
              id="profile-dialog-title"
              component="h2"
              variant="overline"
              sx={{ color: '#d9edef', letterSpacing: 1.5 }}
            >
              Mon profil
            </Typography>
            <IconButton
              onClick={onClose}
              aria-label="Fermer le profil"
              sx={{ color: 'white', '&:hover': { bgcolor: '#ffffff18' } }}
            >
              <CloseRounded />
            </IconButton>
          </Stack>
          <Stack
            direction="row"
            alignItems="center"
            gap={2}
            sx={{ position: 'relative', zIndex: 1 }}
          >
            <Avatar
              sx={{
                width: { xs: 60, sm: 72 },
                height: { xs: 60, sm: 72 },
                bgcolor: '#ffffff18',
                color: 'white',
                border: '2px solid #ffffff50',
                fontSize: '1.5rem',
                fontWeight: 700,
              }}
            >
              {initials}
            </Avatar>
            <Box sx={{ minWidth: 0 }}>
              <Typography
                component="p"
                variant="h5"
                fontWeight={700}
                sx={{ lineHeight: 1.3, overflowWrap: 'anywhere' }}
              >
                {user.name}
              </Typography>
              <Typography variant="body2" sx={{ color: '#cce7eb', mt: 0.75 }}>
                {human(user.role)}
              </Typography>
              <Chip
                label={user.active ? 'Compte actif' : 'Compte désactivé'}
                size="small"
                sx={{
                  mt: 1.25,
                  bgcolor: '#ffffff18',
                  color: 'white',
                  border: '1px solid #ffffff30',
                  height: 25,
                  fontSize: '0.75rem',
                }}
              />
            </Box>
          </Stack>
        </Box>
      </DialogTitle>
      <DialogContent sx={{ p: { xs: 2, sm: 3 }, bgcolor: 'background.default' }}>
        <Stack spacing={2.5} sx={{ pt: 2.5 }}>
          <Paper component="section" variant="outlined" sx={{ p: 2.5, borderRadius: 3 }}>
            <Typography component="h3" variant="subtitle1" fontWeight={700} mb={2}>
              Informations du compte
            </Typography>
            <Box component="dl" sx={{ m: 0 }}>
              {[
                {
                  label: 'Adresse e-mail',
                  value: user.email,
                  icon: <MailOutlineRounded fontSize="small" />,
                },
                {
                  label: 'Rôle et permissions',
                  value: human(user.role),
                  icon: <ShieldOutlined fontSize="small" />,
                },
              ].map((item, index) => (
                <Stack
                  direction="row"
                  gap={1.5}
                  alignItems="flex-start"
                  key={item.label}
                  sx={{
                    pt: index ? 2 : 0,
                    mt: index ? 2 : 0,
                    borderTop: index ? 1 : 0,
                    borderColor: 'divider',
                  }}
                >
                  <Box
                    sx={{
                      p: 1,
                      display: 'flex',
                      borderRadius: 2,
                      color: 'primary.main',
                      bgcolor: (theme) => alpha(theme.palette.primary.main, 0.08),
                    }}
                  >
                    {item.icon}
                  </Box>
                  <Box sx={{ minWidth: 0 }}>
                    <Typography component="dt" variant="caption" color="text.secondary">
                      {item.label}
                    </Typography>
                    <Typography
                      component="dd"
                      variant="body2"
                      fontWeight={600}
                      sx={{ m: 0, mt: 0.5, overflowWrap: 'anywhere' }}
                    >
                      {item.value}
                    </Typography>
                  </Box>
                </Stack>
              ))}
            </Box>
          </Paper>
          <Accordion
            disableGutters
            elevation={0}
            sx={{
              border: 1,
              borderColor: 'divider',
              borderRadius: '12px !important',
              '&::before': { display: 'none' },
              overflow: 'hidden',
            }}
          >
            <AccordionSummary
              expandIcon={<ExpandMoreRounded />}
              id="profile-security-heading"
              aria-controls="profile-security-content"
              sx={{ px: 2.5, py: 1 }}
            >
              <Stack direction="row" gap={1.5} alignItems="center">
                <LockOutlined color="primary" />
                <Box>
                  <Typography component="h3" variant="subtitle2" fontWeight={700}>
                    Changer mon mot de passe
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Sécurité du compte
                  </Typography>
                </Box>
              </Stack>
            </AccordionSummary>
            <AccordionDetails sx={{ px: 2.5, pb: 2.5 }}>
              {online ? (
                <>
                  <Typography variant="body2" color="text.secondary" mb={2.5}>
                    Choisissez un nouveau mot de passe d’au moins 12 caractères.
                  </Typography>
                  <ResourceForm
                    fields={[
                      { name: 'currentPassword', type: 's', required: true },
                      { name: 'newPassword', type: 's', required: true },
                    ]}
                    onCancel={onClose}
                    onSave={async (values) => {
                      await api.post('/profile/password', values);
                      window.location.assign('/');
                    }}
                    submitLabel="Mettre à jour"
                  />
                </>
              ) : (
                <Alert severity="info">
                  Connectez-vous à Internet pour modifier votre mot de passe.
                </Alert>
              )}
            </AccordionDetails>
          </Accordion>
        </Stack>
      </DialogContent>
      <DialogActions
        sx={{
          p: { xs: 2, sm: 2.5 },
          borderTop: 1,
          borderColor: 'divider',
          justifyContent: 'space-between',
          gap: 1,
          flexWrap: 'wrap',
        }}
      >
        <Button color="error" startIcon={<LogoutOutlined />} onClick={onLogout}>
          Se déconnecter
        </Button>
        <Button variant="outlined" onClick={onClose} sx={{ borderRadius: 2 }}>
          Fermer
        </Button>
      </DialogActions>
    </Dialog>
  );
}
