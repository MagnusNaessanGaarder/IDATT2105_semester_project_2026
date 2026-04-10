import { client } from '@/api/client'

export const DOCUMENT_TYPES = [
  { value: 'POLICY',            label: 'Retningslinje' },
  { value: 'PROCEDURE',         label: 'Prosedyre' },
  { value: 'TRAINING_MATERIAL', label: 'Opplæringsmateriell' },
  { value: 'CERTIFICATE',       label: 'Sertifikat' },
  { value: 'ATTACHMENT',        label: 'Vedlegg' },
  { value: 'REPORT_EXPORT',     label: 'Rapport' },
  { value: 'OTHER',             label: 'Annet' },
] as const

export type DocumentTypeValue = typeof DOCUMENT_TYPES[number]['value']

export interface OrganizationDocument {
  documentId: number
  orgNumber: number
  documentType: DocumentTypeValue
  title: string
  description: string | null
  currentVersion: number
  active: boolean
  createdByUserId: number | null
  createdAt: string
  updatedAt: string
}

export interface UploadNewDocumentPayload {
  file: File
  documentType: DocumentTypeValue
  title?: string
  description?: string
  directory?: string
}

export interface UploadNewVersionPayload {
  file: File
  title?: string
  description?: string
  directory?: string
}

export interface DocumentLinkResponse {
  url: string
}
// All endpoints except uploadNewVersion read orgNumber from X-Org-Number header.
// uploadNewVersion reads it as @RequestParam so it goes in FormData.
const orgHeader = (orgNumber: number) => ({ 'X-Org-Number': String(orgNumber) })
export const documentsApi = {

  listDocuments(orgNumber: number, category?: string): Promise<OrganizationDocument[]> {
    return client
        .get<OrganizationDocument[]>('/files', {
          headers: orgHeader(orgNumber),
          params: category ? { category } : undefined,
        })
        .then((r) => r.data)
  },

  uploadDocument(orgNumber: number, payload: UploadNewDocumentPayload): Promise<OrganizationDocument> {
    const form = new FormData()
    form.append('file', payload.file)
    form.append('documentType', payload.documentType)
    if (payload.title?.trim())       form.append('title', payload.title.trim())
    if (payload.description?.trim()) form.append('description', payload.description.trim())
    if (payload.directory)           form.append('directory', payload.directory)

    return client
        .post<OrganizationDocument>('/files/upload', form, {
          // Do NOT set Content-Type manually — axios sets it with the correct boundary
          headers: orgHeader(orgNumber),
        })
        .then((r) => r.data)
  },

  uploadNewVersion(
      orgNumber: number,
      documentId: number,
      payload: UploadNewVersionPayload,
  ): Promise<OrganizationDocument> {
    const form = new FormData()
    form.append('file', payload.file)
    // uploadNewVersion uses @RequestParam, not @RequestHeader
    form.append('orgNumber', String(orgNumber))
    if (payload.title?.trim())       form.append('title', payload.title.trim())
    if (payload.description?.trim()) form.append('description', payload.description.trim())
    if (payload.directory)           form.append('directory', payload.directory)

    return client
        .post<OrganizationDocument>(`/files/${documentId}/version`, form, {
          // orgNumber goes as @RequestParam in FormData for this endpoint
          // Do NOT set Content-Type manually — axios handles the multipart boundary
        })
        .then((r) => r.data)
  },

  async downloadDocument(orgNumber: number, documentId: number, filename: string): Promise<void> {
    const response = await client.get<Blob>(`/files/download/${documentId}`, {
      headers: orgHeader(orgNumber),
      responseType: 'blob',
    })
    const url = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = filename
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    setTimeout(() => URL.revokeObjectURL(url), 10_000)
  },

  async getPreviewUrl(orgNumber: number, documentId: number): Promise<string> {
    const response = await client.get<Blob>(`/files/download/${documentId}`, {
      headers: orgHeader(orgNumber),
      responseType: 'blob',
    })
    return URL.createObjectURL(response.data)
  },

  getDocumentLink(orgNumber: number, documentId: number): Promise<DocumentLinkResponse> {
    return client
      .get<DocumentLinkResponse>(`/files/${documentId}/link`, {
        headers: orgHeader(orgNumber),
      })
      .then((r) => r.data)
  },
}
