import { NextRequest } from "next/server";

const DEFAULT_BACKEND_ORIGIN = "http://localhost:8080";
const PASSTHROUGH_HEADERS = [
  "content-type",
  "cache-control",
  "etag",
  "last-modified",
  "content-length",
];

function getBackendOrigin(): string {
  const configured =
    process.env.API_BASE_URL ??
    process.env.NEXT_PUBLIC_API_BASE_URL ??
    DEFAULT_BACKEND_ORIGIN;
  return configured.replace(/\/+$/, "");
}

function buildBackendAssetUrl(pathSegments: string[], search: string): URL {
  const backendUrl = new URL(getBackendOrigin());
  const safePath = pathSegments
    .filter((segment) => segment.length > 0)
    .map((segment) => encodeURIComponent(segment))
    .join("/");

  backendUrl.pathname = `/${safePath}`;
  backendUrl.search = search;
  return backendUrl;
}

export async function GET(
  request: NextRequest,
  context: { params: Promise<{ path: string[] }> },
): Promise<Response> {
  const { path } = await context.params;
  if (!path || path.length === 0) {
    return new Response("Asset path is required.", { status: 400 });
  }

  const assetUrl = buildBackendAssetUrl(path, request.nextUrl.search);
  const upstream = await fetch(assetUrl, {
    method: "GET",
    headers: {
      Accept: request.headers.get("accept") ?? "*/*",
    },
    cache: "force-cache",
  });

  if (!upstream.ok) {
    return new Response(upstream.body, {
      status: upstream.status,
      statusText: upstream.statusText,
    });
  }

  const headers = new Headers();
  for (const headerName of PASSTHROUGH_HEADERS) {
    const value = upstream.headers.get(headerName);
    if (value) {
      headers.set(headerName, value);
    }
  }
  if (!headers.has("cache-control")) {
    headers.set("cache-control", "public, max-age=300, stale-while-revalidate=86400");
  }

  return new Response(upstream.body, {
    status: upstream.status,
    headers,
  });
}
