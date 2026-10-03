import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'app.coil.focus',
  appName: 'Coil',
  webDir: 'dist',
  backgroundColor: '#f5ead8',
  android: {
    // Users can't usefully inspect the WebView in a release build.
    webContentsDebuggingEnabled: false,
  },
  plugins: {
    SystemBars: {
      insetsHandling: 'css',
      initialViewportFitValueHint: 'cover',
    },
    LocalNotifications: {
      smallIcon: 'ic_stat_coil',
      iconColor: '#c67139',
    },
  },
};

export default config;
