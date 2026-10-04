## Firebase & Local-First Architecture

Oistars Billings is designed as a local-first billing application for invoice management, client ledgers, recurring subscriptions, and offline financial records. The app can optionally integrate with Firebase for AI-assisted features, app attestation, and platform security checks.

If Firebase features are enabled, a valid `google-services.json` must be present and the project must be configured with the correct Firebase project identifiers. Release builds fail fast if Firebase configuration is missing and production signing credentials are not provided.
