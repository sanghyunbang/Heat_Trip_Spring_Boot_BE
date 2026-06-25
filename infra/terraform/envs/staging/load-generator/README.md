# Staging Load Generator EC2

This env creates one EC2 host that runs k6 through Docker.

It does not need k6 installed on the host. The helper script uses the `grafana/k6` container image.

## Apply

```bash
cp terraform.tfvars.example terraform.tfvars
terraform init
terraform plan
terraform apply
```

Set `backend_base_url` from the backend env output:

```text
backend_base_url = "http://<backend-private-ip>:8080"
```

## Run k6

SSH to the loader and run one of the repo scenarios.

```bash
run-heattrip-k6 domains/explore/explore-read-baseline.js
run-heattrip-k6 domains/journey/journey-crud.js
run-heattrip-k6 domains/curation/curation-non-llm-baseline.js
run-heattrip-k6 operations/mysql-ddl/journey-body-ddl-traffic.js
```

If the scenario needs secrets, put the k6 env file in SSM as SecureString and set `k6_env_ssm_parameter_name`.

```text
BASE_URL=http://<backend-private-ip>:8080
ACCESS_TOKEN=<jwt>
VUS=20
DURATION=10m
```

