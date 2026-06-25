# Heat Trip Terraform

This Terraform tree is intentionally split by runtime role.

```text
infra/terraform/
  modules/
    ec2-docker-host/          # shared EC2 + Docker + optional SSM permission
  envs/
    staging/
      backend/                # backend EC2 running Docker Compose
      load-generator/         # loader EC2 running k6 through Docker
```

The backend and load-generator states should stay separate. The loader can be created before a test and destroyed immediately after, without touching the backend.

## Public Repo Secret Policy

Do not commit real values in Terraform files.

- Commit `terraform.tfvars.example`.
- Keep real `terraform.tfvars` local. It is ignored by `.gitignore`.
- Store application `.env` content in AWS SSM Parameter Store as `SecureString`.
- Store k6 environment values in SSM if they include tokens or test credentials.

## Suggested Flow

1. Create a SecureString parameter for the backend `.env`.
2. Apply `envs/staging/backend`.
3. Copy the backend private IP output.
4. Set `backend_base_url` in `envs/staging/load-generator/terraform.tfvars`.
5. Apply `envs/staging/load-generator`.
6. SSH to the loader and run `run-heattrip-k6`.

## State Backend

Each env includes `backend.tf.example`. Copy it to `backend.tf` locally if you want remote S3 state.

```bash
cp backend.tf.example backend.tf
```

Never commit `backend.tf` if it contains private bucket names or account-specific details.

