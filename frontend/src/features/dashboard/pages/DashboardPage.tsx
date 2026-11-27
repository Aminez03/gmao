import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Alert,
  Autocomplete,
  Avatar,
  Box,
  Button,
  Chip,
  Divider,
  LinearProgress,
  MenuItem,
  Paper,
  Skeleton,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { alpha, useTheme } from '@mui/material/styles';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  CartesianGrid,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
} from 'recharts';
import BuildOutlined from '@mui/icons-material/BuildOutlined';
import WarningAmber from '@mui/icons-material/WarningAmber';
import EventAvailable from '@mui/icons-material/EventAvailable';
import CheckCircleOutline from '@mui/icons-material/CheckCircleOutline';
import ArrowForwardRounded from '@mui/icons-material/ArrowForwardRounded';
import DownloadOutlined from '@mui/icons-material/DownloadOutlined';
import FilterAltOutlined from '@mui/icons-material/FilterAltOutlined';
import RestartAltRounded from '@mui/icons-material/RestartAltRounded';
import CalendarMonthOutlined from '@mui/icons-material/CalendarMonthOutlined';
import Inventory2Outlined from '@mui/icons-material/Inventory2Outlined';
import SwapHorizRounded from '@mui/icons-material/SwapHorizRounded';
import AddRounded from '@mui/icons-material/AddRounded';
import InsightsOutlined from '@mui/icons-material/InsightsOutlined';
import { api, download, errorText, Entity } from '../../../services/api';
import { useAuth } from '../../../app/Auth';
import { Status, display } from '../../../components/EntityDisplay';
import { canCreate, getResource, visible } from '../../../components/domain';
export default function DashboardPage() {
  const { user } = useAuth();
  const theme = useTheme();
  const [from, setFrom] = useState('');
  const [to, setTo] = useState('');
  const [team, setTeam] = useState('');
  const [asset, setAsset] = useState('');
  const [assetChoice, setAssetChoice] = useState<Entity | null>(null);
  const [assetSearch, setAssetSearch] = useState('');
  const [error, setError] = useState('');
  const params = {
    ...(from ? { from: new Date(from).toISOString() } : {}),
    ...(to ? { to: new Date(to).toISOString() } : {}),
    ...(team ? { teamId: team } : {}),
    ...(asset ? { assetId: asset } : {}),
  };
  const query = new URLSearchParams(params).toString();
  const {
    data,
    isLoading,
    error: loadError,
  } = useQuery({
    queryKey: ['dashboard', query, user!.id],
    queryFn: async () => (await api.get(`/reports/dashboard?${query}`)).data,
    enabled: navigator.onLine,
  });
  const {
    data: orders,
    isLoading: ordersLoading,
    error: ordersError,
  } = useQuery({
    queryKey: ['recent', user!.id],
    queryFn: async () => (await api.get('/work-orders?size=5')).data,
    enabled: navigator.onLine,
  });
  const { data: teams } = useQuery({
    queryKey: ['dashboard-teams', user!.id],
    queryFn: async () => (await api.get('/teams?size=100')).data,
    enabled: navigator.onLine,
  });
  const { data: assets } = useQuery({
    queryKey: ['dashboard-assets', assetSearch, user!.id],
    queryFn: async () =>
      (await api.get('/assets', { params: { size: 25, search: assetSearch } })).data,
    enabled: navigator.onLine,
  });
  const tiles = [
    {
      label: 'Interventions actives',
      value: data?.active,
      icon: <BuildOutlined />,
      color: '#087f8c',
      note: 'Sur votre périmètre',
    },
    {
      label: 'Échéances dépassées',
      value: data?.overdue,
      icon: <EventAvailable />,
      color: '#c47712',
      note: 'À replanifier en priorité',
    },
    {
      label: 'Interventions critiques',
      value: data?.critical,
      icon: <WarningAmber />,
      color: '#c64048',
      note: 'Priorité P1',
    },
    {
      label: 'Interventions validées',
      value: data?.completed,
      icon: <CheckCircleOutline />,
      color: '#36866c',
      note: 'Sur la période sélectionnée',
    },
  ];
  const hasFilters = !!(from || to || team || asset);
  const canExport = ['ADMIN', 'RESPONSABLE_TECHNIQUE'].includes(user!.role);
  const quickLinks = [
    {
      route: 'assets',
      title: 'Patrimoine',
      description: 'Retrouver un équipement',
      icon: <Inventory2Outlined />,
    },
    {
      route: 'work-orders',
      title: 'Interventions',
      description: 'Suivre les opérations',
      icon: <BuildOutlined />,
    },
    {
      route: 'handovers',
      title: 'Passations',
      description: 'Transmettre les consignes',
      icon: <SwapHorizRounded />,
    },
  ].filter((item) => visible(getResource(item.route), user!.role));
  const chartHasData = data?.series?.some(
    (day: Entity) => Number(day.preventive) > 0 || Number(day.corrective) > 0,
  );

  return (
    <Stack spacing={3} sx={{ pb: 3 }}>
      <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
        <Box>
          <Typography variant="overline" color="text.secondary">
            Vue d’ensemble · Maintenance
          </Typography>
          <Typography variant="h4">Tableau de bord</Typography>
          <Typography color="text.secondary" mt={1}>
            Bonjour {user!.name.split(' ')[0]}, voici l’activité de votre polyclinique.
          </Typography>
        </Box>
        {canCreate(getResource('failure-reports'), user!.role) && (
          <Button
            component={Link}
            to="/m/failure-reports"
            variant="contained"
            startIcon={<AddRounded />}
            sx={{ alignSelf: { xs: 'flex-start', sm: 'center' }, px: 2.5, borderRadius: 2.5 }}
          >
            Déclarer une panne
          </Button>
        )}
      </Stack>
      <Paper
        elevation={0}
        sx={{
          position: 'relative',
          overflow: 'hidden',
          p: { xs: 3, md: 4 },
          borderRadius: 4,
          color: 'white',
          background: 'linear-gradient(115deg, #12394b 0%, #076b78 65%, #0b8991 100%)',
          '&::after': {
            content: '""',
            position: 'absolute',
            width: 300,
            height: 300,
            borderRadius: '50%',
            border: '45px solid rgba(255,255,255,.04)',
            right: -100,
            top: -110,
            pointerEvents: 'none',
          },
        }}
      >
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          gap={3}
          alignItems={{ md: 'center' }}
          justifyContent="space-between"
          sx={{ position: 'relative', zIndex: 1 }}
        >
          <Box sx={{ maxWidth: 600 }}>
            <Chip
              label="Continuité des soins"
              size="small"
              sx={{
                bgcolor: '#ffffff18',
                color: 'white',
                mb: 2,
                border: '1px solid #ffffff25',
                fontWeight: 600,
              }}
            />
            <Typography
              variant="h5"
              color="white"
              sx={{
                fontSize: { xs: '1.5rem', md: '1.85rem' },
                fontWeight: 700,
                lineHeight: 1.3,
                letterSpacing: '-.025em',
              }}
            >
              Vos équipes coordonnées.
              <br />
              Vos équipements suivis.
            </Typography>
            <Typography sx={{ color: '#cce7eb', mt: 1.5, maxWidth: 480, lineHeight: 1.7 }}>
              Suivez les équipements, les interventions et les transmissions de vos équipes.
            </Typography>
          </Box>
          <Box
            sx={{
              p: 2.5,
              borderRadius: 3,
              bgcolor: '#ffffff0d',
              border: '1px solid #ffffff26',
              minWidth: { md: 240 },
            }}
          >
            <Stack direction="row" gap={1.5} alignItems="center" mb={2}>
              <CalendarMonthOutlined sx={{ color: '#a6ebdf' }} />
              <Box>
                <Typography fontWeight={700}>Planning des équipes</Typography>
                <Typography variant="caption" sx={{ color: '#cce7eb' }}>
                  Postes et affectations
                </Typography>
              </Box>
            </Stack>
            <Button
              component={Link}
              to="/planning"
              endIcon={<ArrowForwardRounded />}
              fullWidth
              sx={{
                color: 'white',
                borderColor: '#ffffff50',
                borderRadius: 2,
                '&:hover': { borderColor: 'white', bgcolor: '#ffffff12' },
              }}
              variant="outlined"
            >
              Voir le planning
            </Button>
          </Box>
        </Stack>
      </Paper>
      <Paper variant="outlined" sx={{ p: { xs: 2, md: 2.5 }, borderRadius: 3 }}>
        <Stack direction="row" gap={1} alignItems="center" mb={2} flexWrap="wrap">
          <FilterAltOutlined fontSize="small" color="primary" />
          <Typography variant="subtitle2" fontWeight={700}>
            Filtrer les indicateurs
          </Typography>
          {hasFilters && (
            <Button
              size="small"
              startIcon={<RestartAltRounded />}
              onClick={() => {
                setFrom('');
                setTo('');
                setTeam('');
                setAsset('');
                setAssetChoice(null);
                setAssetSearch('');
              }}
            >
              Réinitialiser
            </Button>
          )}
          {canExport && (
            <Stack direction="row" gap={1} sx={{ ml: 'auto' }}>
              {['pdf', 'xlsx'].map((format) => (
                <Button
                  key={format}
                  size="small"
                  variant="outlined"
                  startIcon={<DownloadOutlined />}
                  disabled={!navigator.onLine || isLoading || !!loadError}
                  onClick={() =>
                    download(
                      `/reports/export?format=${format}&${query}`,
                      `maintenance.${format}`,
                    ).catch((e) => setError(errorText(e)))
                  }
                >
                  {format.toUpperCase()}
                </Button>
              ))}
            </Stack>
          )}
        </Stack>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: {
              xs: 'minmax(0, 1fr)',
              sm: 'repeat(2, minmax(0, 1fr))',
              xl: 'repeat(4, minmax(0, 1fr))',
            },
            gap: 2,
          }}
        >
          <TextField
            type="datetime-local"
            label="Début de période"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
            size="small"
          />
          <TextField
            type="datetime-local"
            label="Fin de période (exclue)"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            slotProps={{ inputLabel: { shrink: true } }}
            size="small"
          />
          <TextField
            select
            label="Équipe"
            value={team}
            onChange={(e) => setTeam(e.target.value)}
            size="small"
            sx={{ minWidth: 0 }}
          >
            <MenuItem value="">Toutes les équipes</MenuItem>
            {teams?.items?.map((item: Entity) => (
              <MenuItem key={item.id} value={item.id}>
                {item.code} · {item.name}
              </MenuItem>
            ))}
          </TextField>
          <Autocomplete
            options={assets?.items || []}
            value={assetChoice}
            getOptionLabel={(item: Entity) => `${item.code} · ${item.name}`}
            isOptionEqualToValue={(option, value) => option.id === value.id}
            onChange={(_, value) => {
              setAssetChoice(value);
              setAsset(value?.id || '');
            }}
            onInputChange={(_, value, reason) => {
              if (reason === 'input' || reason === 'clear') setAssetSearch(value);
            }}
            filterOptions={(options) => options}
            noOptionsText="Aucun équipement"
            sx={{ minWidth: 0 }}
            renderInput={(params) => (
              <TextField
                {...params}
                label="Équipement"
                size="small"
                placeholder="Rechercher par nom ou code"
              />
            )}
          />
        </Box>
      </Paper>
      {(error || loadError) && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error || errorText(loadError)}
        </Alert>
      )}
      {!navigator.onLine && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Le tableau de bord actualisé nécessite une connexion. Vos interventions téléchargées
          restent accessibles.
        </Alert>
      )}
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: 'minmax(0, 1fr)',
            sm: 'repeat(2, minmax(0, 1fr))',
            xl: 'repeat(4, minmax(0, 1fr))',
          },
          gap: 2,
        }}
      >
        {tiles.map((tile) => (
          <Paper
            key={tile.label}
            variant="outlined"
            sx={{
              p: 2.5,
              borderRadius: 3,
              position: 'relative',
              overflow: 'hidden',
              transition: 'border-color .2s ease',
              '&:hover': { borderColor: tile.color },
              '&::before': {
                content: '""',
                position: 'absolute',
                left: 0,
                top: 22,
                bottom: 22,
                width: 3,
                borderRadius: 3,
                bgcolor: tile.color,
              },
            }}
          >
            <Stack direction="row" justifyContent="space-between" alignItems="center">
              <Typography
                color="text.secondary"
                variant="body2"
                fontWeight={600}
                sx={{ maxWidth: '70%' }}
              >
                {tile.label}
              </Typography>
              <Box
                sx={{
                  color: tile.color,
                  bgcolor: alpha(tile.color, theme.palette.mode === 'dark' ? 0.2 : 0.09),
                  p: 1.25,
                  borderRadius: 2.5,
                  display: 'flex',
                }}
              >
                {tile.icon}
              </Box>
            </Stack>
            <Typography component="div" variant="h3" my={1.5} sx={{ letterSpacing: '-.04em' }}>
              {isLoading ? <Skeleton width={75} /> : (tile.value ?? '—')}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              {tile.note}
            </Typography>
          </Paper>
        ))}
      </Box>
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: 'minmax(0, 1fr)', lg: 'minmax(0, 1.5fr) minmax(0, 1fr)' },
          gap: 3,
        }}
      >
        <Paper variant="outlined" sx={{ p: { xs: 2, sm: 3 }, minWidth: 0, borderRadius: 3 }}>
          <Stack direction="row" gap={1.5} alignItems="center" mb={1}>
            <InsightsOutlined color="primary" />
            <Typography variant="h6">Activité de maintenance</Typography>
          </Stack>
          <Typography color="text.secondary" variant="body2" mb={3}>
            Interventions créées · toute la période · par{' '}
            {data?.activityGrouping === 'month'
              ? 'mois'
              : data?.activityGrouping === 'week'
                ? 'semaine'
                : 'jour'}
          </Typography>
          <Box sx={{ height: 290, minWidth: 0 }}>
            {isLoading ? (
              <Skeleton variant="rounded" height="100%" />
            ) : !chartHasData ? (
              <Stack
                alignItems="center"
                justifyContent="center"
                spacing={1}
                sx={{
                  height: '100%',
                  border: '1px dashed',
                  borderColor: 'divider',
                  borderRadius: 2,
                }}
              >
                <InsightsOutlined sx={{ fontSize: 36, color: 'text.disabled' }} />
                <Typography color="text.secondary" textAlign="center">
                  {loadError || !navigator.onLine
                    ? 'Activité indisponible'
                    : 'Aucune activité sur cette période'}
                </Typography>
              </Stack>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart
                  data={data?.series || []}
                  barGap={5}
                  barCategoryGap="25%"
                  margin={{ top: 5, right: 5, left: -25, bottom: 0 }}
                >
                  <CartesianGrid
                    strokeDasharray="3 5"
                    vertical={false}
                    stroke={theme.palette.divider}
                  />
                  <XAxis
                    dataKey="day"
                    tickFormatter={(v: string) =>
                      data?.activityGrouping === 'month'
                        ? `${v.slice(5, 7)}/${v.slice(0, 4)}`
                        : `${v.slice(8, 10)}/${v.slice(5, 7)}`
                    }
                    tick={{ fill: theme.palette.text.secondary, fontSize: 12 }}
                    tickLine={false}
                    axisLine={false}
                    dy={8}
                  />
                  <YAxis
                    allowDecimals={false}
                    tick={{ fill: theme.palette.text.secondary, fontSize: 12 }}
                    tickLine={false}
                    axisLine={false}
                  />
                  <Tooltip
                    cursor={{ fill: alpha(theme.palette.primary.main, 0.05) }}
                    contentStyle={{
                      backgroundColor: theme.palette.background.paper,
                      border: `1px solid ${theme.palette.divider}`,
                      borderRadius: 12,
                      color: theme.palette.text.primary,
                    }}
                    labelFormatter={(value) => {
                      const bucket = data?.series?.find((item: Entity) => item.day === value);
                      const dateLabel = (date: string) =>
                        new Date(`${date}T12:00:00`).toLocaleDateString('fr-FR');
                      return bucket && bucket.lastDay !== value
                        ? `Du ${dateLabel(String(value))} au ${dateLabel(bucket.lastDay)}`
                        : dateLabel(String(value));
                    }}
                  />
                  <Legend />
                  <Bar
                    dataKey="preventive"
                    name="Préventive"
                    fill={theme.palette.primary.main}
                    radius={[5, 5, 0, 0]}
                    maxBarSize={28}
                  />
                  <Bar
                    dataKey="corrective"
                    name="Corrective"
                    fill="#9cb9cd"
                    radius={[4, 4, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            )}
          </Box>
        </Paper>
        <Paper variant="outlined" sx={{ p: { xs: 2, sm: 3 }, borderRadius: 3, minWidth: 0 }}>
          <Typography variant="h6" mb={2}>
            Fiabilité du patrimoine
          </Typography>
          {[
            { label: 'Disponibilité observée', value: data?.availability, unit: '%' },
            { label: 'Préventif réalisé à échéance', value: data?.preventiveRate, unit: '%' },
            { label: 'Temps moyen de réparation', value: data?.mttrMinutes, unit: 'min' },
            { label: 'Temps moyen entre pannes', value: data?.mtbfHours, unit: 'h' },
          ].map((item) => (
            <Box key={item.label} py={1.5}>
              <Stack direction="row" justifyContent="space-between" gap={2} alignItems="baseline">
                <Typography variant="body2" color="text.secondary">
                  {item.label}
                </Typography>
                <Typography component="div" fontWeight={700} sx={{ whiteSpace: 'nowrap' }}>
                  {isLoading ? (
                    <Skeleton width={60} />
                  ) : item.value == null ? (
                    '—'
                  ) : (
                    `${Number(item.value).toFixed(1)} ${item.unit}`
                  )}
                </Typography>
              </Stack>
              {item.unit === '%' && item.value != null ? (
                <LinearProgress
                  variant="determinate"
                  value={Math.min(100, Math.max(0, Number(item.value)))}
                  aria-label={item.label}
                  sx={{
                    mt: 1.25,
                    height: 6,
                    borderRadius: 3,
                    bgcolor: (theme) => alpha(theme.palette.primary.main, 0.08),
                    '& .MuiLinearProgress-bar': { borderRadius: 3 },
                  }}
                />
              ) : (
                <Divider sx={{ mt: 1.5 }} />
              )}
            </Box>
          ))}
          <Typography color="text.secondary" variant="caption" display="block" mt={2}>
            Indicateurs calculés sur les événements enregistrés. Une absence de données ne constitue
            pas une mesure de fiabilité.
          </Typography>
        </Paper>
      </Box>
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: { xs: 'minmax(0, 1fr)', lg: 'minmax(0, 1.5fr) minmax(0, 1fr)' },
          gap: 3,
        }}
      >
        <Paper variant="outlined" sx={{ p: { xs: 2, sm: 3 }, borderRadius: 3, minWidth: 0 }}>
          <Stack
            direction={{ xs: 'column', sm: 'row' }}
            justifyContent="space-between"
            gap={1}
            mb={2}
          >
            <Typography variant="h6">Dernières interventions</Typography>
            <Button
              component={Link}
              to="/m/work-orders"
              endIcon={<ArrowForwardRounded />}
              size="small"
              sx={{ alignSelf: 'flex-start' }}
            >
              Tout consulter
            </Button>
          </Stack>
          {ordersError ? (
            <Alert severity="error">{errorText(ordersError)}</Alert>
          ) : ordersLoading ? (
            <Stack spacing={2}>
              {[0, 1, 2].map((key) => (
                <Skeleton key={key} variant="rounded" height={60} />
              ))}
            </Stack>
          ) : !orders?.items?.length ? (
            <Typography color="text.secondary" py={3}>
              Aucune intervention enregistrée. Commencez par créer votre patrimoine et vos équipes.
            </Typography>
          ) : (
            orders.items.map((w: Entity) => (
              <Box
                key={w.id}
                component={Link}
                to={`/m/work-orders?id=${w.id}`}
                sx={{
                  display: 'flex',
                  flexWrap: { xs: 'wrap', sm: 'nowrap' },
                  gap: 1.5,
                  alignItems: 'center',
                  p: 1.5,
                  my: 0.5,
                  color: 'text.primary',
                  borderRadius: 2,
                  border: '1px solid',
                  borderColor: 'transparent',
                  '&:hover': {
                    bgcolor: (theme) => alpha(theme.palette.primary.main, 0.04),
                    borderColor: 'divider',
                  },
                  '&:focus-visible': { outline: '2px solid', outlineColor: 'primary.main' },
                }}
              >
                <Avatar
                  variant="rounded"
                  sx={{
                    bgcolor: (theme) => alpha(theme.palette.primary.main, 0.08),
                    color: 'primary.main',
                    width: 38,
                    height: 38,
                  }}
                >
                  <BuildOutlined fontSize="small" />
                </Avatar>
                <Box sx={{ flex: 1, minWidth: 0 }}>
                  <Typography fontWeight={600} variant="body2" sx={{ overflowWrap: 'anywhere' }}>
                    {w.title}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {w.number} · {display(w.createdAt)}
                  </Typography>
                </Box>
                <Stack
                  direction="row"
                  gap={1}
                  flexWrap="wrap"
                  sx={{ width: { xs: '100%', sm: 'auto' }, pl: { xs: '50px', sm: 0 } }}
                >
                  {w.priority && <Status value={w.priority} />}
                  {w.status && <Status value={w.status} />}
                </Stack>
              </Box>
            ))
          )}
        </Paper>
        <Paper variant="outlined" sx={{ p: { xs: 2, sm: 3 }, borderRadius: 3 }}>
          <Typography variant="h6" mb={0.5}>
            Accès rapides
          </Typography>
          <Typography variant="body2" color="text.secondary" mb={2}>
            L’essentiel de votre espace de travail.
          </Typography>
          <Stack spacing={1.25}>
            {quickLinks.map((item) => (
              <Box
                key={item.route}
                component={Link}
                to={`/m/${item.route}`}
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 1.5,
                  p: 1.75,
                  borderRadius: 2.5,
                  border: '1px solid',
                  borderColor: 'divider',
                  color: 'text.primary',
                  '&:hover': {
                    bgcolor: (theme) => alpha(theme.palette.primary.main, 0.04),
                    borderColor: 'primary.main',
                  },
                }}
              >
                <Box sx={{ display: 'flex', color: 'primary.main' }}>{item.icon}</Box>
                <Box flex={1}>
                  <Typography variant="body2" fontWeight={700}>
                    {item.title}
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {item.description}
                  </Typography>
                </Box>
                <ArrowForwardRounded fontSize="small" sx={{ color: 'text.secondary' }} />
              </Box>
            ))}
          </Stack>
          {data?.partsCost !== undefined && (
            <Box
              sx={{
                mt: 2.5,
                p: 2,
                borderRadius: 2.5,
                bgcolor: (theme) => alpha(theme.palette.primary.main, 0.06),
              }}
            >
              <Typography variant="caption" color="text.secondary">
                Coût des pièces consommées
              </Typography>
              <Typography variant="h6" sx={{ mt: 0.5 }}>
                {Number(data.partsCost).toLocaleString('fr-TN', {
                  minimumFractionDigits: 3,
                  maximumFractionDigits: 3,
                })}{' '}
                <Typography component="span" variant="body2" color="text.secondary">
                  TND
                </Typography>
              </Typography>
              {data.costScope && (
                <Typography variant="caption" color="text.secondary">
                  {data.costScope}
                </Typography>
              )}
            </Box>
          )}
        </Paper>
      </Box>
    </Stack>
  );
}
