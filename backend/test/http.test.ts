import { afterEach, describe, expect, it, vi } from "vitest";
import { fetchWithTimeout } from "@/lib/http";

const realFetch = globalThis.fetch;

afterEach(() => {
  globalThis.fetch = realFetch;
  vi.restoreAllMocks();
});

describe("fetchWithTimeout", () => {
  it("aborts the request once the timeout elapses", async () => {
    // A fetch that never resolves on its own — only an abort signal can end it.
    globalThis.fetch = vi.fn((_input: string, init?: RequestInit) => {
      return new Promise<Response>((_resolve, reject) => {
        init?.signal?.addEventListener("abort", () =>
          reject(new DOMException("Aborted", "AbortError")),
        );
      });
    }) as unknown as typeof fetch;

    await expect(fetchWithTimeout("https://example.com", { timeoutMs: 20 })).rejects.toThrow();
  });

  it("returns the response when it resolves before the timeout", async () => {
    globalThis.fetch = vi.fn(async () => new Response("ok", { status: 200 })) as unknown as typeof fetch;

    const res = await fetchWithTimeout("https://example.com", { timeoutMs: 1_000 });
    expect(res.status).toBe(200);
    expect(await res.text()).toBe("ok");
  });

  it("passes an abort signal through to the underlying fetch", async () => {
    let receivedSignal: AbortSignal | undefined;
    globalThis.fetch = vi.fn(async (_input: string, init?: RequestInit) => {
      receivedSignal = init?.signal ?? undefined;
      return new Response("ok");
    }) as unknown as typeof fetch;

    await fetchWithTimeout("https://example.com", { timeoutMs: 1_000 });
    expect(receivedSignal).toBeInstanceOf(AbortSignal);
  });
});
