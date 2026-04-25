"use client";

import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { ArrowUp, ChevronsDown, MessageSquare, X } from "lucide-react";
import {
  ApiError,
  fetchAuthMeCached,
  fetchChatHistory,
  fetchChatUnreadCount,
  markChatRead,
  openChatEventStream,
  sendChatMessage,
  type AuthMeResponse,
  type ChatMessageResponse,
} from "@/lib/api";
import { formatDateTime } from "@/lib/format";

function formatChatClock(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return formatDateTime(value);
  }
  return date.toLocaleTimeString("en-GB", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function isAdminRole(role: string | null | undefined): boolean {
  return role === "ROLE_ADMIN" || role === "ADMIN";
}

function parseChatEvent(data: string): ChatMessageResponse | null {
  try {
    const parsed = JSON.parse(data) as Partial<ChatMessageResponse>;
    if (typeof parsed.content !== "string" || typeof parsed.senderRole !== "string") {
      return null;
    }
    return parsed as ChatMessageResponse;
  } catch {
    return null;
  }
}

function upsertChatMessage(
  messages: ChatMessageResponse[],
  incoming: ChatMessageResponse,
): ChatMessageResponse[] {
  if (incoming.id == null) {
    return [...messages, incoming].slice(-50);
  }
  const existingIndex = messages.findIndex((message) => message.id === incoming.id);
  if (existingIndex === -1) {
    return [...messages, incoming].slice(-50);
  }
  const next = [...messages];
  next[existingIndex] = incoming;
  return next;
}

export function StorefrontChatBubble() {
  const pathname = usePathname();
  const chatPanelRef = useRef<HTMLElement | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const messagesViewportRef = useRef<HTMLDivElement | null>(null);
  const unreadCountRef = useRef(0);
  const pulseTimerRef = useRef<number | null>(null);
  const shouldAutoScrollRef = useRef(true);
  const pendingOpenScrollRef = useRef(false);
  const [authState, setAuthState] = useState<AuthMeResponse>({
    authenticated: false,
    email: null,
    role: null,
    fullName: null,
  });
  const [open, setOpen] = useState(false);
  const [loadingChat, setLoadingChat] = useState(false);
  const [sending, setSending] = useState(false);
  const [draft, setDraft] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [hasUnreadPulse, setHasUnreadPulse] = useState(false);
  const [showJumpToLatest, setShowJumpToLatest] = useState(false);
  const [showScrollTop, setShowScrollTop] = useState(false);
  const [pageVisible, setPageVisible] = useState(
    () => typeof document === "undefined" || document.visibilityState === "visible",
  );

  const syncScrollModeFromViewport = useCallback(() => {
    const viewport = messagesViewportRef.current;
    if (!viewport) {
      shouldAutoScrollRef.current = true;
      setShowJumpToLatest(false);
      return;
    }
    const remaining = viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight;
    const nearBottom = remaining <= 36;
    shouldAutoScrollRef.current = nearBottom;
    setShowJumpToLatest(!nearBottom);
  }, []);

  function scrollToLatest(behavior: ScrollBehavior = "smooth") {
    shouldAutoScrollRef.current = true;
    setShowJumpToLatest(false);
    messagesEndRef.current?.scrollIntoView({ block: "end", behavior });
  }

  useEffect(() => {
    let alive = true;

    async function resolveAuth() {
      try {
        const response = await fetchAuthMeCached();
        if (alive) {
          setAuthState(response);
        }
      } catch {
        if (alive) {
          setAuthState({
            authenticated: false,
            email: null,
            role: null,
            fullName: null,
          });
        }
      }
    }

    void resolveAuth();
    return () => {
      alive = false;
    };
  }, []);

  useEffect(() => {
    function onVisibilityChange() {
      setPageVisible(document.visibilityState === "visible");
    }

    document.addEventListener("visibilitychange", onVisibilityChange);
    return () => {
      document.removeEventListener("visibilitychange", onVisibilityChange);
    };
  }, []);

  const shouldShow =
    authState.authenticated &&
    !isAdminRole(authState.role) &&
    pathname !== "/chat" &&
    !pathname.startsWith("/chat/");

  const triggerUnreadPulse = useCallback(() => {
    setHasUnreadPulse(true);
    if (pulseTimerRef.current !== null) {
      window.clearTimeout(pulseTimerRef.current);
    }
    pulseTimerRef.current = window.setTimeout(() => {
      setHasUnreadPulse(false);
      pulseTimerRef.current = null;
    }, 2200);
  }, []);

  useEffect(() => {
    if (!shouldShow || !pageVisible) {
      return;
    }

    let alive = true;

    async function syncChat() {
      try {
        if (open) {
          const history = await fetchChatHistory();
          if (!alive) {
            return;
          }
          setMessages(history);
          await markChatRead();
          if (!alive) {
            return;
          }
          setUnreadCount(0);
        } else {
          const nextUnreadCount = await fetchChatUnreadCount();
          if (!alive) {
            return;
          }
          if (nextUnreadCount > unreadCountRef.current) {
            triggerUnreadPulse();
          }
          unreadCountRef.current = nextUnreadCount;
          setUnreadCount(nextUnreadCount);
        }
      } catch (err) {
        if (!alive) {
          return;
        }
        if (err instanceof ApiError) {
          setError(err.message);
        } else {
          setError("Failed to sync chat.");
        }
      }
    }

    void syncChat();
    const eventSource = openChatEventStream();
    eventSource.addEventListener("message", (event) => {
      if (!alive) {
        return;
      }
      const incoming = parseChatEvent(event.data);
      if (!incoming) {
        return;
      }

      if (open) {
        setMessages((current) => upsertChatMessage(current, incoming));
        unreadCountRef.current = 0;
        setUnreadCount(0);
        void markChatRead().catch(() => undefined);
        return;
      }

      const nextUnreadCount = unreadCountRef.current + 1;
      unreadCountRef.current = nextUnreadCount;
      setUnreadCount(nextUnreadCount);
      triggerUnreadPulse();
    });

    return () => {
      alive = false;
      eventSource.close();
    };
  }, [open, pageVisible, shouldShow, triggerUnreadPulse]);

  useEffect(() => {
    if (!shouldShow || !open) {
      return;
    }

    function onPointerDown(event: MouseEvent) {
      const targetNode = event.target as Node | null;
      if (!targetNode || !chatPanelRef.current) {
        return;
      }
      if (!chatPanelRef.current.contains(targetNode)) {
        pendingOpenScrollRef.current = false;
        setOpen(false);
      }
    }

    document.addEventListener("mousedown", onPointerDown);
    return () => {
      document.removeEventListener("mousedown", onPointerDown);
    };
  }, [open, shouldShow]);

  useEffect(() => {
    if (!open || !messagesEndRef.current || !shouldAutoScrollRef.current) {
      return;
    }
    messagesEndRef.current.scrollIntoView({ block: "end", behavior: "auto" });
  }, [messages, open]);

  useEffect(() => {
    if (!open || loadingChat || !pendingOpenScrollRef.current) {
      return;
    }

    const frame = window.requestAnimationFrame(() => {
      scrollToLatest("auto");
      syncScrollModeFromViewport();
      pendingOpenScrollRef.current = false;
    });

    return () => {
      window.cancelAnimationFrame(frame);
    };
  }, [loadingChat, messages.length, open, syncScrollModeFromViewport]);

  useEffect(() => {
    if (!open) {
      shouldAutoScrollRef.current = true;
      pendingOpenScrollRef.current = false;
      return;
    }
    const frame = window.requestAnimationFrame(() => {
      syncScrollModeFromViewport();
    });
    return () => {
      window.cancelAnimationFrame(frame);
    };
  }, [messages.length, open, syncScrollModeFromViewport]);

  useEffect(() => {
    return () => {
      if (pulseTimerRef.current !== null) {
        window.clearTimeout(pulseTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (!shouldShow) {
      return;
    }

    const onWindowScroll = () => {
      if (open) {
        setShowScrollTop(false);
        return;
      }
      setShowScrollTop(window.scrollY > 420);
    };

    onWindowScroll();
    window.addEventListener("scroll", onWindowScroll, { passive: true });
    return () => {
      window.removeEventListener("scroll", onWindowScroll);
    };
  }, [open, shouldShow]);

  async function openChat() {
    shouldAutoScrollRef.current = true;
    pendingOpenScrollRef.current = true;
    setShowJumpToLatest(false);
    setOpen(true);
    setLoadingChat(true);
    setError(null);

    try {
      const history = await fetchChatHistory();
      setMessages(history);
      await markChatRead();
      unreadCountRef.current = 0;
      setUnreadCount(0);
      setHasUnreadPulse(false);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load chat history.");
      }
    } finally {
      setLoadingChat(false);
    }
  }

  async function onSend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const content = draft.trim();
    if (!content) {
      return;
    }

    setSending(true);
    setError(null);
    shouldAutoScrollRef.current = true;
    setShowJumpToLatest(false);
    try {
      await sendChatMessage(content);
      setDraft("");
      const history = await fetchChatHistory();
      setMessages(history);
      await markChatRead();
      unreadCountRef.current = 0;
      setUnreadCount(0);
      setHasUnreadPulse(false);
      scrollToLatest("auto");
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to send message.");
      }
    } finally {
      setSending(false);
    }
  }

  if (!shouldShow) {
    return null;
  }

  return (
    <>
      {open ? (
        <section
          ref={chatPanelRef}
          className="fixed bottom-5 right-5 z-40 w-[min(22rem,calc(100vw-1.5rem))] overflow-hidden rounded-[1.4rem] border border-white/12 bg-[linear-gradient(180deg,rgba(18,18,18,0.98),rgba(5,5,5,0.98))] shadow-[0_14px_30px_rgba(0,0,0,0.38)] sm:bottom-6 sm:right-6"
        >
          <header className="flex items-center justify-between border-b border-white/10 bg-black/25 px-4 py-3">
            <div>
              <h2 className="text-sm font-semibold text-[var(--color-text)]">Chat With Shop</h2>
              <p className="text-xs text-[var(--color-text-muted)]">Ask about products, orders, or shipping.</p>
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => {
                  pendingOpenScrollRef.current = false;
                  setOpen(false);
                }}
                aria-label="Close chat"
                className="inline-flex h-8 w-8 items-center justify-center rounded-full border border-white/14 bg-black/35 text-[var(--color-text-muted)] transition-[background-color,color,border-color,transform] duration-200 hover:-translate-y-px hover:border-white/18 hover:bg-white hover:text-black"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </header>

          <div className="px-4 pb-4 pt-3">
            {error ? <p className="mb-3 text-sm text-red-700">{error}</p> : null}

            {loadingChat ? (
              <div className="flex h-72 items-center justify-center rounded-2xl border border-white/10 bg-black/30 text-sm text-[var(--color-text-muted)]">
                Loading chat...
              </div>
            ) : messages.length === 0 ? (
              <div className="flex h-72 items-center justify-center rounded-2xl border border-white/10 bg-black/30 text-sm text-[var(--color-text-muted)]">
                Start a conversation with the shop.
              </div>
            ) : (
              <div className="relative">
                <div
                  ref={messagesViewportRef}
                  onScroll={syncScrollModeFromViewport}
                  className="chat-grid-paper max-h-[20rem] min-h-[18rem] space-y-3 overflow-y-auto rounded-2xl border border-white/10 p-3.5"
                >
                  {messages.map((message) => {
                    const isUser = message.senderRole === "USER";
                    const sideClass = isUser ? "justify-end" : "justify-start";
                    const toneClass = isUser
                      ? "bg-[var(--color-primary)] text-black shadow-[0_6px_14px_rgba(255,255,255,0.16)]"
                      : "border border-white/12 bg-white/6 text-[var(--color-text)]";
                    const metaClass = isUser ? "text-white/68" : "text-[var(--color-text-muted)]";
                    return (
                      <div key={message.id} className={`flex ${sideClass}`}>
                        <div className={`flex max-w-[88%] flex-col gap-1 ${isUser ? "items-end" : "items-start"}`}>
                          <article className={`inline-block w-fit break-words rounded-2xl px-3 py-2.5 text-sm ${toneClass}`}>
                            <p className="whitespace-pre-wrap leading-relaxed">{message.content}</p>
                          </article>
                          <p className={`px-1 text-[11px] ${metaClass}`}>
                            {isUser ? "You" : "Shop"} - {formatChatClock(message.createdAt)}
                          </p>
                        </div>
                      </div>
                    );
                  })}
                  <div ref={messagesEndRef} />
                </div>
                {showJumpToLatest ? (
                  <button
                    type="button"
                    onClick={() => scrollToLatest("smooth")}
                    aria-label="Jump to latest message"
                    title="Jump to latest"
                    className="absolute bottom-3 right-3 inline-flex items-center gap-1.5 rounded-full border border-white/18 bg-black/65 px-3 py-1.5 text-xs font-semibold text-[var(--color-text)] shadow-[0_10px_22px_rgba(0,0,0,0.35)] transition-[transform,background-color,color,border-color] duration-200 hover:-translate-y-px hover:border-white/24 hover:bg-white hover:text-black"
                  >
                    Latest
                    <ChevronsDown className="h-3.5 w-3.5" />
                  </button>
                ) : null}
              </div>
            )}

            <form onSubmit={onSend} className="mt-3 flex gap-2 rounded-2xl border border-white/10 bg-black/25 p-2">
              <input
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="Write your message..."
                className="ui-input flex-1 border-white/12 bg-black/35 px-3 py-2 text-sm"
              />
              <button
                type="submit"
                disabled={sending || !draft.trim()}
                className="ui-btn ui-btn-primary inline-flex min-w-[92px] items-center justify-center gap-2 px-4 py-2 text-sm"
              >
                {sending ? "Sending..." : "Send"}
              </button>
            </form>
          </div>
        </section>
      ) : null}

      {!open ? (
        showScrollTop ? (
          <button
            type="button"
            onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}
            aria-label="Scroll to top"
            title="Scroll to top"
            className="fixed bottom-3 right-5 z-30 inline-flex h-10 w-10 items-center justify-center rounded-full border border-white/14 bg-[var(--color-surface-soft)] text-[var(--color-text)] shadow-[0_10px_22px_rgba(0,0,0,0.34)] transition-[transform,background-color,color,border-color] duration-200 hover:-translate-y-px hover:border-white/10 hover:bg-white hover:text-black sm:bottom-3 sm:right-6"
          >
            <ArrowUp className="h-4 w-4" />
          </button>
        ) : null
      ) : null}

      {!open ? (
        <button
          type="button"
          onClick={() => void openChat()}
          aria-label="Open chat"
          title="Open chat"
          className={`fixed right-5 z-40 inline-flex min-h-14 items-center justify-center gap-2 rounded-full border border-white/16 bg-[var(--color-surface-soft)] px-4 text-[var(--color-text)] shadow-[0_12px_24px_rgba(0,0,0,0.36)] transition-[transform,background-color,color,border-color,opacity] duration-200 hover:-translate-y-px hover:border-white/10 hover:bg-white hover:text-black sm:right-6 ${
            showScrollTop ? "bottom-16 sm:bottom-16" : "bottom-5 sm:bottom-6"
          } ${
            hasUnreadPulse ? "ui-notify-pulse" : ""
          }`}
        >
          <MessageSquare className="h-5 w-5" />
          {unreadCount > 0 ? (
            <span className="text-xs font-semibold leading-none text-current">
              {unreadCount > 99 ? "99+" : unreadCount} unread
            </span>
          ) : null}
          {unreadCount > 0 ? (
            <span className="absolute -right-1 -top-1 inline-flex min-w-5 items-center justify-center rounded-full border border-white/20 bg-black px-1.5 py-0.5 text-[10px] font-bold text-white shadow-[0_6px_16px_rgba(0,0,0,0.32)]">
              {unreadCount > 99 ? "99+" : unreadCount}
            </span>
          ) : null}
        </button>
      ) : null}
    </>
  );
}
