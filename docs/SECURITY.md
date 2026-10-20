# Security Architecture & Threat Model

MySpace Personal Hub is designed from the ground up as a **privacy-first, local-first, zero-knowledge personal information manager**. This document outlines the cryptographic standards, key management mechanisms, authentication flows, and data protections implemented across the application.

---

## 1. Threat Model & Principles

### Core Principles
1. **Zero Tracking**: Zero third-party telemetry, advertising SDKs, behavioral analytics, or opaque tracking libraries.
2. **Local-First Single Source of Truth**: All personal data (notes, contacts, finances, tasks, passwords) resides in a local Room database and encrypted local app storage.
3. **Defense in Depth**: Sensitive records in the Private Vault are encrypted at rest with hardware-backed keys (`AndroidKeyStore`) using authenticated encryption (AES-256-GCM).
4. **Resilient Offline Fallback**: The app operates seamlessly with or without internet connectivity. If Firebase sync is optionally configured, encrypted local state is the authoritative truth.

### Threat Scenarios Mitigated
| Threat Vector | Mitigation Strategy |
| :--- | :--- |
| **Physical Device Theft / Inspection** | Android Keystore hardware-backed keys (`AES_256_GCM`). Biometric authentication (Class 3 strong biometrics or device PIN/pattern) guards the Private Vault. |
| **Database File Extraction / Root Access** | Private Vault items (`VaultItemEntity`) store their payloads (`secretPayload`) encrypted via AES-256-GCM. Unlocking requires dynamic Keystore decryption. |
| **Backup Snooping & Cloud Interception** | Full database backup archives are encrypted using PBKDF2-HMAC-SHA256 (600,000 iterations) and AES-256-GCM before export or sharing. |
| **Replay & Modification Attacks** | AES-GCM provides authenticated encryption with associated data (AEAD). Any bit flips or payload tampering result in an `AEADBadTagException` and immediate rejection. |
| **Sync Eavesdropping** | Realtime Database connections mandate TLS 1.3 in transit and Firebase Security Rules requiring client-level authentication. |

---

## 2. Hardware-Backed Encryption (`CryptoManager`)

The Private Vault utilizes `AndroidKeyStore` to generate, manage, and isolate cryptographic keys within the device's Secure Element (SE) or Trusted Execution Environment (TEE).

### Key Specifications
- **Alias**: `myspace_vault_master_key`
- **Key Store**: `AndroidKeyStore`
- **Algorithm**: `KeyProperties.KEY_ALGORITHM_AES` (AES)
- **Block Size**: 256 bits (`AES-256`)
- **Block Mode**: `KeyProperties.BLOCK_MODE_GCM` (Galois/Counter Mode)
- **Padding Scheme**: `KeyProperties.ENCRYPTION_PADDING_NONE`
- **Initialization Vector (IV)**: Securely generated 12-byte random IV per encryption operation via `SecureRandom`.
- **Authentication Tag**: 128-bit authentication tag (`GCMParameterSpec(128, iv)`).

### Encryption Wire Format
Ciphertexts are serialized as:
```
[ 12 bytes IV ] + [ Encrypted Ciphertext + 16 bytes Authentication Tag ]
```
Stored as Base64 strings. Upon decryption, the initial 12 bytes are extracted as the IV and passed to the AES-GCM cipher initialized with `Cipher.DECRYPT_MODE`.

---

## 3. Encrypted Backup Architecture (`BackupCrypto`)

Portable backup archives exported to external storage or shared via Android `FileProvider` must protect all user data against offline brute-force attacks.

### Key Derivation Function (KDF)
- **Algorithm**: `PBKDF2WithHmacSHA256`
- **Iterations**: **600,000 rounds** (aligned with OWASP 2024+ recommendations for PBKDF2-HMAC-SHA256)
- **Salt**: 16 cryptographically secure random bytes generated via `SecureRandom`
- **Derived Key Length**: 256 bits (`AES-256`)

### Symmetric Cipher
- **Cipher**: `AES/GCM/NoPadding`
- **IV**: 12 cryptographically secure random bytes per backup archive
- **Authentication Tag**: 128 bits

### Backup Wire Protocol (`.myspace` Archive)
```
+-------------------+-------------------+-------------------+------------------------------+
| Salt (16 bytes)   | IV (12 bytes)     | Tag (16 bytes)    | Ciphertext (N bytes)         |
+-------------------+-------------------+-------------------+------------------------------+
```
Decryption failure (wrong passphrase, corrupted archive, or tampered byte stream) triggers an immediate checksum error and prevents incomplete database writes.

---

## 4. Biometric Authentication & Session Management

The Private Vault is guarded by AndroidX `BiometricPrompt`:
- **Allowed Authenticators**:
  - `BiometricManager.Authenticators.BIOMETRIC_STRONG` (Fingerprint, 3D Face Unlock)
  - `BiometricManager.Authenticators.DEVICE_CREDENTIAL` (PIN, Pattern, System Password fallback)
- **Session Rules**:
  - The Vault does not keep plaintext records in memory indefinitely.
  - Backgrounding the app or navigating away automatically locks the Vault session.
  - Decrypted passwords and private documents are held only in local Composable UI state and cleared when leaving the Vault view.

---

## 5. Network & Sync Security

- **Cleartext Traffic**: Disabled by default in Android manifest (`android:usesCleartextTraffic="false"`).
- **Transport Security**: All external synchronization with Firebase Realtime Database operates over TLS 1.3 HTTPS/WSS.
- **Firebase Security Rules**:
  ```json
  {
    "rules": {
      "users": {
        "$uid": {
          ".read": "$uid === auth.uid",
          ".write": "$uid === auth.uid"
        }
      }
    }
  }
  ```
- **Local Fallback Safety**: If Firebase credentials (`google-services.json`) are absent or sync is turned off in Settings, the app operates entirely in air-gapped local mode without network requests or crashes.

---

## 6. Permissions Audit

MySpace requests only the minimal set of Android permissions necessary to provide its offline-first productivity features:

| Permission | Purpose |
| :--- | :--- |
| `POST_NOTIFICATIONS` | Delivering user-scheduled task reminders and habit check-in alarms (Android 13+). |
| `SCHEDULE_EXACT_ALARM` | Ensuring exact-timed task alerts fire precisely when requested. |
| `RECEIVE_BOOT_COMPLETED` | Rescheduling pending task alarms following a device restart. |
| `INTERNET` | Optional cloud synchronization with Firebase Realtime Database. |
| `ACCESS_NETWORK_STATE` | WorkManager network constraint verification for background sync. |
| `USE_BIOMETRIC` | Biometric fingerprint/face unlock for the Private Vault. |

No location, SMS, microphone, camera, or third-party telephony permissions are requested.
