import type { Components, Theme } from '@mui/material/styles';

export function alertTheme(dark: boolean): Components<Theme>['MuiAlert'] {
  const tones = {
    error: {
      color: dark ? '#ffe1e5' : '#881d2a',
      backgroundColor: dark ? '#3b202b' : '#fff0f2',
      borderColor: dark ? '#b46979' : '#d88996',
    },
    warning: {
      color: dark ? '#ffe6b3' : '#774500',
      backgroundColor: dark ? '#352b18' : '#fff7e4',
      borderColor: dark ? '#b78c43' : '#cea04f',
    },
    success: {
      color: dark ? '#c9f5e1' : '#17563e',
      backgroundColor: dark ? '#18372e' : '#edf9f2',
      borderColor: dark ? '#569d7e' : '#7eb99c',
    },
    info: {
      color: dark ? '#d1eaff' : '#164f76',
      backgroundColor: dark ? '#1a3046' : '#eef7ff',
      borderColor: dark ? '#5e94c2' : '#82b3d4',
    },
  };
  return {
    styleOverrides: {
      root: {
        alignItems: 'flex-start',
        gap: 10,
        padding: '12px 16px',
        border: '1px solid',
        borderLeftWidth: 4,
        borderRadius: 12,
        fontSize: '0.9375rem',
        fontWeight: 600,
        '& .MuiAlert-icon': { color: 'inherit' },
      },
      message: {
        minWidth: 0,
        flex: 1,
        padding: 0,
        lineHeight: 1.65,
        whiteSpace: 'pre-wrap',
        overflowWrap: 'anywhere',
      },
      icon: { padding: '1px 0', marginRight: 0, flexShrink: 0, opacity: 1 },
      action: { paddingTop: 0, alignItems: 'flex-start' },
      standardError: tones.error,
      standardWarning: tones.warning,
      standardSuccess: tones.success,
      standardInfo: tones.info,
      outlinedError: tones.error,
      outlinedWarning: tones.warning,
      outlinedSuccess: tones.success,
      outlinedInfo: tones.info,
    },
  };
}
