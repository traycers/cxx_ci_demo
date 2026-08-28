# Build with context = repo root (see the `bootstrap` service in docker-compose.yml): this image
# needs both these scripts and the repos/ seed content in its build context. See ADR 0008.
FROM python:3.14.7-slim

# git: GitPython shells out to the real binary, it isn't a pure-Python git implementation.
# ca-certificates: needed for any future https:// remote, cheap to have even though every
# target this script talks to today is a plain http:// service on the cxxci network.
RUN apt-get update \
    && apt-get install -y --no-install-recommends git ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY scripts/bootstrap/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY scripts/bootstrap/*.py ./
# Baked in at build time, not bind-mounted: see ADR 0008 — adding a seed branch means rebuilding
# this image, traded deliberately for a simpler, self-contained container.
COPY repos/ ./repos/

ENTRYPOINT ["python", "main.py"]
