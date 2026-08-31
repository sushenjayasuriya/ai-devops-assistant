# Database Schema & Flyway Migrations

## Overview
The application uses **PostgreSQL 16** with **Flyway** for database versioning and migrations.

## Migration Scripts
- `V1__init_schema.sql`: Core relational tables for organizations, users, environments, servers, containers, clusters, nodes, deployments, incidents, events, AI conversations/actions, approvals, and audit logs.
- `V2__seed_initial_data.sql`: Seed data with default roles (`ADMIN`, `DEVOPS_ENGINEER`, `VIEWER`), default demo organization, standard environments (`DEVELOPMENT`, `STAGING`, `PRODUCTION`), Linux host nodes, Docker containers (including the ThingsBoard incident reproduction scenario), and active integrations.
- `V3__create_indexes.sql`: High-performance composite indexes on environment, status, correlation IDs, timestamps, and resource identifiers.

## Key Relational Entities
```
organizations
  ├── users (roles: ADMIN, DEVOPS_ENGINEER, VIEWER)
  ├── environments (DEVELOPMENT, STAGING, PRODUCTION)
  │     ├── servers
  │     ├── containers
  │     ├── deployments
  │     ├── integrations (PROMETHEUS, DOCKER, LINUX_SSH, K8S)
  │     ├── incidents
  │     │     └── incident_events
  │     └── approval_requests
  └── audit_logs (immutable)
```
