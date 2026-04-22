import type { NextConfig } from "next";

const DEFAULT_BACKEND_ORIGIN = "http://localhost:8080";
const LOCAL_BACKEND_ORIGINS = [
  DEFAULT_BACKEND_ORIGIN,
  "http://127.0.0.1:8080",
];

type RemotePattern = NonNullable<NonNullable<NextConfig["images"]>["remotePatterns"]>[number];

function toRemotePattern(origin: string): RemotePattern | null {
  try {
    const url = new URL(origin);
    const protocol = url.protocol.replace(":", "");
    if (protocol !== "http" && protocol !== "https") {
      return null;
    }
    return {
      protocol,
      hostname: url.hostname,
      port: url.port,
      pathname: "/**",
    };
  } catch {
    return null;
  }
}

function uniqueRemotePatterns(origins: string[]): RemotePattern[] {
  const seen = new Set<string>();
  const patterns: RemotePattern[] = [];
  for (const origin of origins) {
    const pattern = toRemotePattern(origin);
    if (!pattern) {
      continue;
    }
    const key = `${pattern.protocol}://${pattern.hostname}:${pattern.port}${pattern.pathname}`;
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    patterns.push(pattern);
  }
  return patterns;
}

const configuredBackendOrigin =
  process.env.API_BASE_URL ?? process.env.NEXT_PUBLIC_API_BASE_URL ?? DEFAULT_BACKEND_ORIGIN;

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      ...uniqueRemotePatterns([configuredBackendOrigin, ...LOCAL_BACKEND_ORIGINS]),
      {
        protocol: "https",
        hostname: "placehold.co",
        port: "",
        pathname: "/**",
      },
    ],
  },
};

export default nextConfig;
