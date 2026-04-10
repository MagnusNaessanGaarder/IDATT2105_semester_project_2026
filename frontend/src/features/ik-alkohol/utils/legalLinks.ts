const KNOWN_LAW_LINKS: Array<{ pattern: RegExp; url: string }> = [
  {
    pattern: /\balkohollov/i,
    url: 'https://lovdata.no/dokument/NL/lov/1989-06-02-27',
  },
  {
    pattern: /\balkoholforskrift\b|\bskjenkeforskrift\b/i,
    url: 'https://lovdata.no/dokument/SF/forskrift/2005-06-08-538',
  },
  {
    pattern: /\bserveringslov/i,
    url: 'https://lovdata.no/dokument/NL/lov/1997-06-13-55',
  },
  {
    pattern: /\binternkontrollforskrift\b/i,
    url: 'https://lovdata.no/dokument/SF/forskrift/1996-12-06-1127',
  },
  {
    pattern: /\bmatlov/i,
    url: 'https://lovdata.no/dokument/NL/lov/2003-12-19-124',
  },
]

export const getKnownLegalSourceUrl = (title: string, description?: string | null): string | null => {
  const lookupText = `${title} ${description ?? ''}`.trim()

  const knownLink = KNOWN_LAW_LINKS.find(({ pattern }) => pattern.test(lookupText))
  if (knownLink) {
    return knownLink.url
  }

  return null
}
