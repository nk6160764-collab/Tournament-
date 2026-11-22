// GamerZone PK Web Configuration
// Connects directly to the same free Firebase Spark backend used by the Android APK.
export const GAMERZONE_CONFIG = {
  brandName: "GAMERZONE PK",
  tagline: "Pakistan's Premier Free Fire Esports Arena",
  apkDownloadUrl: "./downloads/gamerzone-pk.apk",
  apkFileName: "GamerZonePK-v1.0.apk",
  apkVersion: "v1.0",
  apkSizeLabel: "27.1 MB",
  minAndroid: "Android 7.0+",
  whatsappNumber: "923001234567",
  supportEmail: "support@gamerzonepk.com",
  paymentMethods: {
    easypaisa: {
      raw: "easypaisa",
      displayName: "EasyPaisa",
      accountNumber: "03489674834",
      accountTitle: "GamerZone PK Official"
    },
    jazzcash: {
      raw: "jazzcash",
      displayName: "JazzCash",
      accountNumber: "03406690336",
      accountTitle: "GamerZone PK Official"
    }
  },
  firebase: {
    apiKey: "AIzaSyDyppvzgglCU-gqutFcuZNU61ug1LzwMfs",
    authDomain: "gen-lang-client-0582973628.firebaseapp.com",
    projectId: "gen-lang-client-0582973628",
    storageBucket: "gen-lang-client-0582973628.firebasestorage.app",
    messagingSenderId: "629589170124",
    appId: "1:629589170124:android:deb39ad793357e4d12af69"
  },
  firestoreDatabaseId: "ai-studio-android-ffposter-c25512ad-21aa-40be-872b-2126a42fdc0e",
  bootstrappedAdminEmail: "nk6160764@gmail.com"
};
