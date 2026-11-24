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
          primary: { main: dark ? '#58bac3' : '#087f8c' },
          background: {
            default: dark ? '#101b29' : '#f3f7f9',
            paper: dark ? '#192738' : '#ffffff',
          },
          text: { primary: dark ? '#edf4fc' : '#1c3046', secondary: dark ? '#a6b9cb' : '#627587' },
        },
        typography: {
          fontFamily: 'Inter, Segoe UI, Arial, sans-serif',
          fontSize: 14,
          h3: { fontWeight: 700, fontSize: '2.25rem' },
          h4: { fontWeight: 700, fontSize: '1.85rem', letterSpacing: '-.035em' },
          h5: { fontWeight: 650 },
          h6: { fontWeight: 650, fontSize: '1.1rem' },
          button: { textTransform: 'none', fontWeight: 600 },
        },
        shape: { borderRadius: 12 },
        components: {
          MuiPaper: { styleOverrides: { outlined: { borderColor: dark ? '#2c3d50' : '#e1e9ef' } } },
          MuiButton: { styleOverrides: { root: { minHeight: 40, boxShadow: 'none' } } },
          MuiTableCell: {
            styleOverrides: {
              head: {
                backgroundColor: dark ? '#1d2f43' : '#f8fafc',
                fontWeight: 600,
                color: dark ? '#a6b9cb' : '#627587',
              },
              root: { padding: '16px 20px' },
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
