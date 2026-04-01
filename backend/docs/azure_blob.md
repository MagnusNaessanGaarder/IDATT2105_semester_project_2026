# Azure Blob Storage Integration

## Overview

This project uses Azure Blob Storage to store organization documents (policies, procedures, certificates, etc.). Each tenant (organization) gets its own blob container named `org-{orgNumber}`, ensuring logical data isolation.

## Configuration

Set the connection string in your `.env` or environment variables:

```
AZURE_STORAGE_CONNECTION_STRING=DefaultEndpointsProtocol=https;AccountName=<your-account>;AccountKey=<your-key>;EndpointSuffix=core.windows.net
```

You can find this in the Azure Portal under **Storage Account → Access keys → Connection string**.

## How It Works

- **Upload:** `POST /api/files/upload` — uploads a file to blob storage and creates metadata rows in `organization_document` and `organization_document_version`.
- **Download:** `GET /api/files/download/{documentId}` — retrieves the file from blob storage using the blob name stored in the version table.

Both endpoints require an `X-Org-Number` header to identify the tenant.

## Container Structure

```
org-123/
  ├── documents/report.pdf
  ├── documents/policy.docx
  └── certificates/food-hygiene.pdf
```

Containers are created automatically on the first upload for a given organization.

## Key Classes

| Class | Role |
|-------|------|
| `AzureBlobConfig` | Creates the `BlobServiceClient` bean from the connection string |
| `BlobStorageService` | Handles upload, download, delete, and SAS URL generation |
| `DocumentService` | Orchestrates blob upload + DB persistence in a transaction with compensation |
| `FileController` | REST endpoints for upload and download |

## Input Validation

File names are sanitized before upload to prevent path traversal. The directory parameter is also sanitized, and content type defaults to `application/octet-stream` if not provided.

## Testing

In tests, `BlobServiceClient` is mocked via `TestBlobConfig` and the `AzureBlobConfig` is excluded using `@Profile("!test")`. No Azure connection is needed to run the test suite.