# note-keeper on k3s

Manifests for single-node k3s (`192.168.1.103`). Deployed by Gitea Actions [`.gitea/workflows/deploy-k3s.yml`](../../.gitea/workflows/deploy-k3s.yml). Synology Docker deploy remains in [`.gitea/workflows/deploy.yml`](../../.gitea/workflows/deploy.yml).

## Access

`http://192.168.1.103:30081`

## Gitea repository secrets

| Secret | Required | Description |
|--------|----------|-------------|
| `K3S_SSH_KEY` | yes | Private SSH key for `darvik@192.168.1.103` |
| `APP_ENCRYPTION_KEY` | yes | AES key for the app |
| `JWT_SECRET` | yes | JWT signing secret |
| `GOOGLE_CLIENT_ID` | no | Google OAuth |
| `GOOGLE_CLIENT_SECRET` | no | Google OAuth |
| `TELEGRAM_WEBHOOK_BASE_URL` | no | Public Telegram webhook base URL |

On the k3s host, `darvik` must have passwordless sudo for `k3s` / `k3s kubectl` / `k3s ctr`.

## Flow

1. Build image `docker.io/library/note-keeper:latest`
2. SSH import via `k3s ctr images import`
3. Apply manifests + Secret
4. `rollout restart` and wait until ready

`imagePullPolicy: Never` (local import). SQLite data on PVC `note-keeper-data` (`local-path`, 5Gi). `replicas: 1`.
