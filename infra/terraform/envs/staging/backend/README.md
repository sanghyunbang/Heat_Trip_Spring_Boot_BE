# Staging Backend EC2

This env creates one EC2 host that runs:

- Spring Boot backend
- MySQL from `docker-compose.yml`
- optional Prometheus/Grafana from `docker-compose.observability.yml`

## Secrets

Create one SSM SecureString parameter containing the full backend `.env` content.

```bash
aws ssm put-parameter \
  --name /heat-trip/staging/backend/env \
  --type SecureString \
  --value file://.env \
  --overwrite
```

For load testing from another EC2, the `.env` stored in SSM should include:

```text
APP_PUBLISHED_HOST=0.0.0.0
APP_PUBLISHED_PORT=8080
APP_SECURITY_RATE_LIMIT_ENABLED=false
```

Keep `backend_app_allowed_cidr_blocks` restricted to the loader/VPC CIDR.

## Apply

```bash
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
terraform apply
```

Copy the `backend_base_url_for_loader` output into the load-generator env.

