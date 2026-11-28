# Doc Generators

This folder contains reusable generators for architecture DOCX deliverables.

## Files

- `generate_frontend_architecture_docs.py`
- `generate_fullstack_e2e_docs.py`
- `generate_backend_architecture_docs.py`
- `generate_updation_traceability_doc.py`
- `generate_release_checklist_v1_1.py`
- `validate_doc_artifacts.py`
- `install_pre_commit_hook.ps1`
- `build_docx_documentation.py`
- `build_user_diagrams_docx.py`
- `generate_diagrams.py`
- `requirements.txt`

## Run

```powershell
cd C:\Users\raman\eclipse-workspace\docmind-backend\docs\scripts\doc_generators
python -m pip install -r requirements.txt
python generate_frontend_architecture_docs.py
python generate_fullstack_e2e_docs.py
python generate_backend_architecture_docs.py
python generate_updation_traceability_doc.py
python generate_release_checklist_v1_1.py
python validate_doc_artifacts.py
```

## Local Git Hook (recommended)

Enable automatic docs validation before each commit:

```powershell
cd C:\Users\raman\eclipse-workspace\docmind-backend\docs\scripts\doc_generators
powershell -ExecutionPolicy Bypass -File .\install_pre_commit_hook.ps1
```

This configures `core.hooksPath` to use repo hooks in `.githooks/`.

## Output Locations

- Frontend: `docs/docx/frontend/` and `docs/docx/frontend/images/`
- Full stack: `docs/docx/fullstack/` and `docs/docx/fullstack/images/`
- Backend: `docs/docx/backend/` and `docs/docx/backend/images/`
- Updation traceability: `docs/docx/fullstack/DocMind_Updation_Traceability_v1_1.docx`

Current baseline outputs are generated as `v1_1` for frontend, backend, and fullstack architecture artifacts.
Superseded artifacts are archived under `docs/docx/archive/`.
