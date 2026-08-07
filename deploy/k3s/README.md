# note-keeper on k3s

Manifests for single-node k3s (`192.168.1.103`). Deployed by Gitea Actions [`.gitea/workflows/deploy-k3s.yml`](../../.gitea/workflows/deploy-k3s.yml). Synology Docker deploy remains in [`.gitea/workflows/deploy.yml`](../../.gitea/workflows/deploy.yml).

## Access

`http://192.168.1.103:30081`

## Gitea repository secrets

| Secret | Required | Description |
|--------|----------|-------------|
| `K3S_SSH_KEY_B64` | yes* | Private SSH key, **base64** (`base64 -w0 < key`) — preferred |
| `K3S_SSH_KEY` | yes* | Private SSH key PEM (raw multiline) — alternative to `_B64` |
| `APP_ENCRYPTION_KEY` | yes | AES key for the app |
| `JWT_SECRET` | yes | JWT signing secret |
| `GOOGLE_CLIENT_ID` | no | Google OAuth |
| `GOOGLE_CLIENT_SECRET` | no | Google OAuth |
| `TELEGRAM_WEBHOOK_BASE_URL` | no | Public Telegram webhook base URL |

\* One of `K3S_SSH_KEY_B64` or `K3S_SSH_KEY` is required.

### SSH deploy key setup

On a machine that already has working SSH to the node:

```bash
# dedicated key (recommended)
ssh-keygen -t ed25519 -f ./gitea-k3s-deploy -N "" -C "gitea-note-keeper-k3s"

# install public key on the node
ssh-copy-id -i ./gitea-k3s-deploy.pub darvik@192.168.1.103

# put PRIVATE key into Gitea secret K3S_SSH_KEY_B64
base64 -w0 ./gitea-k3s-deploy | clip.exe   # or copy output into Gitea UI
```

On the k3s host, `darvik` must have passwordless sudo for `k3s` / `k3s kubectl` / `k3s ctr`.

If the workflow prints `Permission denied (publickey)`, the private key in the secret does not match any key in `~/.ssh/authorized_keys` on the node. Compare the fingerprint printed by the workflow with:

```bash
ssh-keygen -lf ~/.ssh/authorized_keys
```

## Flow

1. Build image `docker.io/library/note-keeper:latest`
2. SSH import via `k3s ctr images import`
3. Apply manifests + Secret
4. `rollout restart` and wait until ready

`imagePullPolicy: Never` (local import). SQLite data on PVC `note-keeper-data` (`local-path`, 5Gi). `replicas: 1`.

Init container creates `/app/var/{data,attachments,backups}` on the PVC before the app starts (SQLite needs `data/`).
