# Sample Corpora — Sources and Provenance

This file records the provenance and licensing of the YAML sample corpora used by
MetaYAML for metamodel inference and for the multi-domain evaluation in the
dissertation *"Leverage JSON and YAML with Model-Driven Engineering Approaches"*.

**Collected:** 2026-05-31. All collected files are **unmodified** copies of the
originals; only the file names were changed to guarantee uniqueness.

---

## Docker Compose (`docker-compose/`) — 41 files

| Source | Detail |
|--------|--------|
| Repository | [docker/awesome-compose](https://github.com/docker/awesome-compose) |
| Commit | `18f59bd` |
| Licence | **CC0 1.0 Universal** (public domain dedication — no attribution required) |
| Collected | 39 files — every `compose.yaml` / `docker-compose.yml` across the example directories |
| Naming | named after the source example directory, e.g. `angular/compose.yaml` → `angular.yml` |

Plus **2** pre-existing hand-authored examples (`monitoring.yml`, `webapp.yml`).

## Ansible (`ansible/`) — 58 files

| Source | Detail |
|--------|--------|
| Repository | [geerlingguy/ansible-for-devops](https://github.com/geerlingguy/ansible-for-devops) |
| Commit | `86e1bf8` |
| Licence | **MIT** (© 2014 Jeff Geerling) |
| Collected | 56 files — every `.yml` / `.yaml` containing a top-level `- hosts:` play (playbook heuristic) |
| Naming | source-relative path with separators replaced by `-`, e.g. `deployments/playbooks/deploy.yml` → `deployments-playbooks-deploy.yml` |

Plus **2** pre-existing hand-authored examples (`backup-database.yml`, `setup-webserver.yml`).

## GitHub Actions (`github-actions/`) — 179 files

| Source | Detail |
|--------|--------|
| Repository | [actions/starter-workflows](https://github.com/actions/starter-workflows) |
| Licence | Official starter workflow templates published by GitHub for public use (see repository). |
| Collected | 179 official starter workflow templates (as described in the dissertation evaluation). |

---

## Licensing notes
- **CC0 1.0** imposes no attribution requirement; the source is credited here as good practice.
- **MIT** requires retention of the copyright notice — recorded above (© 2014 Jeff Geerling).
- Re-running `./gradlew run` over these corpora regenerates the `.ecore` and `.puml`
  outputs in `output/` for all three domains.
