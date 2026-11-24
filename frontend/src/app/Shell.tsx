import BuildOutlined from '@mui/icons-material/BuildOutlined';
import CalendarMonthOutlined from '@mui/icons-material/CalendarMonthOutlined';
import DarkModeOutlined from '@mui/icons-material/DarkModeOutlined';
import DashboardOutlined from '@mui/icons-material/DashboardOutlined';
import GroupsOutlined from '@mui/icons-material/GroupsOutlined';
import HandshakeOutlined from '@mui/icons-material/HandshakeOutlined';
import LightModeOutlined from '@mui/icons-material/LightModeOutlined';
import LogoutOutlined from '@mui/icons-material/LogoutOutlined';
import Menu from '@mui/icons-material/Menu';
import NotificationsNone from '@mui/icons-material/NotificationsNone';
import PersonOutline from '@mui/icons-material/PersonOutline';
import PrecisionManufacturingOutlined from '@mui/icons-material/PrecisionManufacturingOutlined';
import QrCodeScanner from '@mui/icons-material/QrCodeScanner';
import ShoppingBagOutlined from '@mui/icons-material/ShoppingBagOutlined';
import SyncOutlined from '@mui/icons-material/SyncOutlined';
import {
  Alert,
  AppBar,
  Avatar,
  Box,
  Chip,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Stack,
  Toolbar,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { human, resources, visible } from '../components/domain';
import ProfileDialog from '../features/users/ProfileDialog';
import { api, Entity, errorText } from '../services/api';
import { pending, synchronize } from '../services/offline';
import AssistantChat from '../features/assistant/AssistantChat';
import { useAuth } from './Auth';
import AppRoutes from './AppRoutes';
const groupIcons: Record<string, React.ReactNode> = {
  patrimoine: <PrecisionManufacturingOutlined />,
  equipes: <GroupsOutlined />,
  maintenance: <BuildOutlined />,
  achats: <ShoppingBagOutlined />,
  prestataires: <HandshakeOutlined />,
  pilotage: <NotificationsNone />,
};
export default function Shell({
  online,
  dark,
  toggle,
}: {
  online: boolean;
  dark: boolean;
  toggle: () => void;
}) {
  const { user, logout } = useAuth();
  const location = useLocation();
  const [open, setOpen] = useState(false);
  const [count, setCount] = useState(0);
  const [error, setError] = useState('');
  const [profile, setProfile] = useState(false);
  useEffect(() => {
    const update = () => void pending(user!.id).then((p) => setCount(p.length));
    update();
    const timer = setInterval(update, 5000);
    return () => clearInterval(timer);
  }, [user]);
  useEffect(() => {
    if (online)
      void pending(user!.id)
        .then((p) => {
          if (p.some((o) => o.state === 'PENDING')) return synchronize(user!.id);
        })
        .catch((e) => setError(errorText(e)));
  }, [online, user]);
  const { data: notifications } = useQuery({
    queryKey: ['unread', user!.id],
    queryFn: async () => (await api.get('/notifications?size=100')).data,
    refetchInterval: 60000,
    enabled: online,
  });
  const unread = notifications?.items.filter((n: Entity) => !n.readAt).length || 0;
  function nav(to: string, label: string, icon?: React.ReactNode) {
    return (
      <ListItemButton
        component={Link}
        to={to}
        selected={location.pathname === to}
        onClick={() => setOpen(false)}
        sx={{
          mx: 1.5,
          mb: 0.4,
          borderRadius: 2,
          '&.Mui-selected': {
            bgcolor: 'primary.main',
            color: 'primary.contrastText',
            '& .MuiListItemIcon-root': { color: 'inherit' },
          },
        }}
      >
        <ListItemIcon sx={{ minWidth: 36 }}>{icon}</ListItemIcon>
        <ListItemText
          primary={label}
          slotProps={{
            primary: { fontSize: 14, fontWeight: location.pathname === to ? 650 : 400 },
          }}
        />
      </ListItemButton>
    );
  }
  const sidebar = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <Stack direction="row" alignItems="center" gap={1.5} p={3}>
        <img src="/icon.svg" width="38" height="38" alt="" />
        <Box>
          <Typography fontWeight={750}>POLYCLINIQUE</Typography>
          <Typography variant="caption" color="text.secondary">
            Maintenance & patrimoine
          </Typography>
        </Box>
      </Stack>
      <Box sx={{ overflowY: 'auto', flex: 1 }}>
        <List>
          {nav('/', 'Dashboard', <DashboardOutlined />)}
          {nav('/planning', 'Planning des postes', <CalendarMonthOutlined />)}
          {nav('/scan', 'Scanner un QR code', <QrCodeScanner />)}
          {nav('/sync', `Synchronisation${count ? ` (${count})` : ''}`, <SyncOutlined />)}
        </List>
        {['maintenance', 'patrimoine', 'equipes', 'achats', 'prestataires', 'pilotage'].map(
          (group) => {
            const list = resources.filter((r) => r.group === group && visible(r, user!.role));
            return list.length ? (
              <Box key={group}>
                <Typography
                  variant="overline"
                  color="text.secondary"
                  sx={{ display: 'block', px: 3, mt: 2, mb: 1 }}
                >
                  {human(group)}
                </Typography>
                <List disablePadding>
                  {list.map((r) => (
                    <Box key={r.route}>{nav(`/m/${r.route}`, r.label, groupIcons[group])}</Box>
                  ))}
                </List>
              </Box>
            ) : null;
          },
        )}
        {['ADMIN', 'RESPONSABLE_TECHNIQUE'].includes(user!.role) && (
          <List>{nav('/users', 'Utilisateurs', <PersonOutline />)}</List>
        )}
      </Box>
      <Stack
        p={2}
        borderTop="1px solid"
        borderColor="divider"
        direction="row"
        alignItems="center"
        gap={1.5}
      >
        <Avatar sx={{ bgcolor: 'primary.main', width: 34, height: 34, fontSize: 14 }}>
          {user!.name.slice(0, 2).toUpperCase()}
        </Avatar>
        <Box flex={1}>
          <Typography variant="body2" fontWeight={600}>
            {user!.name}
          </Typography>
          <Typography variant="caption" color="text.secondary">
            {human(user!.role)}
          </Typography>
        </Box>
        <IconButton
          aria-label="Se déconnecter"
          onClick={() => logout().catch((e) => setError(errorText(e)))}
        >
          <LogoutOutlined fontSize="small" />
        </IconButton>
      </Stack>
    </Box>
  );
  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <Drawer
        variant="permanent"
        sx={{
          display: { xs: 'none', lg: 'block' },
          width: 264,
          flexShrink: 0,
          '& .MuiDrawer-paper': { width: 264, boxSizing: 'border-box' },
        }}
      >
        {sidebar}
      </Drawer>
      <Drawer
        open={open}
        onClose={() => setOpen(false)}
        sx={{ '& .MuiDrawer-paper': { width: 280 } }}
      >
        {sidebar}
      </Drawer>
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <AppBar
          position="sticky"
          color="inherit"
          elevation={0}
          sx={{ borderBottom: '1px solid', borderColor: 'divider' }}
        >
          <Toolbar sx={{ gap: 1.5 }}>
            <IconButton
              onClick={() => setOpen(true)}
              sx={{ display: { lg: 'none' } }}
              aria-label="Ouvrir le menu"
            >
              <Menu />
            </IconButton>
            <Typography variant="body2" color="text.secondary" sx={{ flex: 1 }}>
              Services techniques{' '}
              <Box component="span" sx={{ display: { xs: 'none', sm: 'inline' } }}>
                / Espace de travail
              </Box>
            </Typography>
            <Chip
              size="small"
              color={online ? 'success' : 'warning'}
              variant="outlined"
              label={online ? 'Connecté' : 'Hors connexion'}
            />
            <IconButton
              aria-label={dark ? 'Activer le mode clair' : 'Activer le mode sombre'}
              onClick={toggle}
            >
              {dark ? <LightModeOutlined /> : <DarkModeOutlined />}
            </IconButton>
            <IconButton
              component={Link}
              to="/m/notifications"
              aria-label={`${unread} notifications non lues`}
            >
              <NotificationsNone />
              {unread > 0 && <span className="notification-dot" />}
            </IconButton>
            <IconButton aria-label="Mon profil" onClick={() => setProfile(true)}>
              <Avatar sx={{ width: 32, height: 32, fontSize: 13, bgcolor: 'primary.main' }}>
                {user!.name.slice(0, 2).toUpperCase()}
              </Avatar>
            </IconButton>
          </Toolbar>
        </AppBar>
        <Box component="main" sx={{ p: { xs: 2, md: 4 }, maxWidth: 1800, mx: 'auto' }}>
          {error && (
            <Alert severity="warning" onClose={() => setError('')} sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}
          <AppRoutes />
        </Box>
      </Box>
      <ProfileDialog
        open={profile}
        onClose={() => setProfile(false)}
        online={online}
        onLogout={() => {
          void logout().catch((e) => setError(errorText(e)));
        }}
      />
      <AssistantChat />
    </Box>
  );
}
