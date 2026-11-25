import { Box, CircularProgress, CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import { useEffect, useMemo, useState } from 'react';
import { useAuth } from './app/Auth';
import Shell from './app/Shell';
import Login from './features/auth/pages/LoginPage';
export default function App() {
  const [dark, setDark] = useState(localStorage.getItem('gmao-theme') === 'dark');
  const [online, setOnline] = useState(navigator.onLine);
  const theme = useMemo(
    () =>
      createTheme({
        palette: {
          mode: dark ? 'dark' : 'light',
          primary: { main: dark ? '#5fd0d0' : '#087f8c', dark: dark ? '#3aa9b0' : '#066a75' },
          secondary: { main: dark ? '#f0a477' : '#cb7045' },
          success: { main: dark ? '#69c7a2' : '#278666' },
          warning: { main: dark ? '#f2b65d' : '#bd7713' },
          error: { main: dark ? '#ff8585' : '#c34e55' },
          background: {
            default: dark ? '#101b29' : '#f4f7f8',
            paper: dark ? '#192738' : '#ffffff',
          },
          text: { primary: dark ? '#edf4fc' : '#193246', secondary: dark ? '#a6b9cb' : '#617686' },
        },
        typography: {
          fontFamily: 'Inter, Segoe UI, Arial, sans-serif',
          fontSize: 14,
          h3: { fontWeight: 750, fontSize: '2.25rem', letterSpacing: '-.04em' },
          h4: { fontWeight: 750, fontSize: '1.85rem', letterSpacing: '-.04em' },
          h5: { fontWeight: 650 },
          h6: { fontWeight: 650, fontSize: '1.1rem' },
          button: { textTransform: 'none', fontWeight: 600 },
        },
        shape: { borderRadius: 14 },
        components: {
          MuiPaper: {
            styleOverrides: {
              root: { backgroundImage: 'none' },
              outlined: {
                borderColor: dark ? '#2c3d50' : '#e2e9ec',
                boxShadow: dark ? 'none' : '0 2px 10px rgba(27, 58, 75, .035)',
              },
            },
          },
          MuiButton: {
            styleOverrides: {
              root: { minHeight: 40, borderRadius: 10, fontWeight: 650, boxShadow: 'none' },
              contained: { '&:hover': { boxShadow: '0 7px 18px rgba(8, 127, 140, .18)' } },
            },
          },
          MuiIconButton: {
            styleOverrides: {
              root: { borderRadius: 11, transition: 'background-color .18s ease, color .18s ease' },
            },
          },
          MuiOutlinedInput: {
            styleOverrides: {
              root: {
                borderRadius: 11,
                backgroundColor: dark ? '#142231' : '#fff',
                '&:hover .MuiOutlinedInput-notchedOutline': {
                  borderColor: dark ? '#5fd0d0' : '#76aeb1',
                },
              },
            },
          },
          MuiChip: { styleOverrides: { root: { fontWeight: 600, borderRadius: 9 } } },
          MuiDrawer: {
            styleOverrides: {
              paper: {
                backgroundColor: dark ? '#142231' : '#fbfcfc',
                borderColor: dark ? '#2c3d50' : '#e4eaed',
              },
            },
          },
          MuiAppBar: {
            styleOverrides: {
              root: {
                backgroundColor: dark ? 'rgba(20, 34, 49, .9)' : 'rgba(255, 255, 255, .9)',
                backdropFilter: 'blur(14px)',
              },
            },
          },
          MuiTableCell: {
            styleOverrides: {
              head: {
                backgroundColor: dark ? '#1d2f43' : '#f5f8f9',
                fontWeight: 600,
                color: dark ? '#a6b9cb' : '#627587',
                whiteSpace: 'nowrap',
              },
              root: { padding: '14px 18px', borderColor: dark ? '#2c3d50' : '#edf1f2' },
            },
          },
        },
      }),
    [dark],
  );
  useEffect(() => {
    const change = () => setOnline(navigator.onLine);
    window.addEventListener('online', change);
    window.addEventListener('offline', change);
    return () => {
      window.removeEventListener('online', change);
      window.removeEventListener('offline', change);
    };
  }, []);
  const { user, loading } = useAuth();
  if (loading)
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <Box sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center' }}>
          <CircularProgress aria-label="Chargement de votre session" />
        </Box>
      </ThemeProvider>
    );
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      {user ? (
        <Shell
          online={online}
          dark={dark}
          toggle={() => {
            localStorage.setItem('gmao-theme', dark ? 'light' : 'dark');
            setDark(!dark);
          }}
        />
      ) : (
        <Login />
      )}
    </ThemeProvider>
  );
}
