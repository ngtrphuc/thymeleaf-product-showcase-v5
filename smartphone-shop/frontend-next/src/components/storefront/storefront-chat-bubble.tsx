"use client";

import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { ArrowDown, ArrowUp, MessageSquare, SendHorizontal, X } from "lucide-react";
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

function compareMessagesByTimeline(left: ChatMessageResponse, right: ChatMessageResponse): number {
  const leftTime = new Date(left.createdAt).getTime();
  const rightTime = new Date(right.createdAt).getTime();
  const leftTimestamp = Number.isNaN(leftTime) ? 0 : leftTime;
  const rightTimestamp = Number.isNaN(rightTime) ? 0 : rightTime;
  if (leftTimestamp !== rightTimestamp) {
    return leftTimestamp - rightTimestamp;
  }
  return (left.id ?? 0) - (right.id ?? 0);
}

function upsertChatMessage(
  messages: ChatMessageResponse[],
  incoming: ChatMessageResponse,
): ChatMessageResponse[] {
  if (incoming.id == null) {
    return [...messages, incoming].sort(compareMessagesByTimeline).slice(-50);
  }
  const existingIndex = messages.findIndex((message) => message.id === incoming.id);
  if (existingIndex === -1) {
    return [...messages, incoming].sort(compareMessagesByTimeline).slice(-50);
  }
  const next = [...messages];
  next[existingIndex] = incoming;
  return next.sort(compareMessagesByTimeline).slice(-50);
}

export function StorefrontChatBubble() {
  const pathname = usePathname();
  const chatPanelRef = useRef<HTMLElement | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const messagesViewportRef = useRef<HTMLDivElement | null>(null);
  const unreadCountRef = useRef(0);
  const openRef = useRef(false);
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
    openRef.current = open;
  }, [open]);

  useEffect(() => {
    if (!shouldShow || !pageVisible) {
      return;
    }

    let alive = true;

    async function syncChat() {
      try {
        if (openRef.current) {
          const history = await fetchChatHistory();
          if (!alive) {
            return;
          }
          setMessages(history.reduce<ChatMessageResponse[]>((current, message) => upsertChatMessage(current, message), []));
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

      if (openRef.current) {
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
  }, [pageVisible, shouldShow, triggerUnreadPulse]);

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
    if (!shouldShow || !open) {
      return;
    }

    function onKeyDown(event: KeyboardEvent) {
      if (event.key !== "Escape") {
        return;
      }
      event.preventDefault();
      pendingOpenScrollRef.current = false;
      setOpen(false);
    }

    document.addEventListener("keydown", onKeyDown);
    return () => {
      document.removeEventListener("keydown", onKeyDown);
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
      setMessages(history.reduce<ChatMessageResponse[]>((current, message) => upsertChatMessage(current, message), []));
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
      const sent = await sendChatMessage(content);
      setMessages((current) => upsertChatMessage(current, sent));
      setDraft("");
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
          className="storefront-chat-panel fixed bottom-5 right-5 z-40 w-[min(22rem,calc(100vw-1.5rem))] overflow-hidden rounded-[1.4rem] sm:bottom-6 sm:right-6"
        >
          <header className="storefront-chat-header flex items-center justify-between px-4 py-3">
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
                className="storefront-chat-close inline-flex h-8 w-8 items-center justify-center rounded-full transition-[background-color,color,border-color,transform] duration-200 hover:-translate-y-px"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </header>

          <div className="px-4 pb-4 pt-3">
            {error ? <p className="mb-3 text-sm text-red-700">{error}</p> : null}

            {loadingChat ? (
              <div className="storefront-chat-state flex h-72 items-center justify-center rounded-2xl text-sm text-[var(--color-text-muted)]">
                Loading chat...
              </div>
            ) : messages.length === 0 ? (
              <div className="storefront-chat-state flex h-72 items-center justify-center rounded-2xl text-sm text-[var(--color-text-muted)]">
                Start a conversation with the shop.
              </div>
            ) : (
              <div className="relative">
                <div
                  ref={messagesViewportRef}
                  onScroll={syncScrollModeFromViewport}
                  className="storefront-chat-messages chat-grid-paper max-h-[20rem] min-h-[18rem] min-w-0 space-y-3 overflow-x-hidden overflow-y-auto rounded-2xl p-3.5"
                >
                  {messages.map((message, index) => {
                    const isUser = message.senderRole === "USER";
                    const sideClass = isUser ? "justify-end" : "justify-start";
                    const toneClass = isUser
                      ? "bg-[var(--chat-accent)] text-black shadow-[0_6px_14px_rgba(0,0,0,0.12)]"
                      : "storefront-chat-peer bg-[var(--chat-peer-bg)] text-[var(--color-text)]";
                    const metaClass = isUser ? "text-[var(--chat-meta)]" : "text-[var(--chat-meta)]";
                    return (
                      <div
                        key={message.id ?? `${message.userEmail}:${message.createdAt}:${message.senderRole}:${index}`}
                        className={`flex min-w-0 ${sideClass}`}
                      >
                        <div
                          className={`flex max-w-[82%] min-w-0 flex-col gap-1 ${isUser ? "items-end" : "items-start"}`}
                        >
                          <article
                            className={`inline-block max-w-full rounded-2xl px-3 py-2.5 text-sm ${toneClass}`}
                            style={{ overflowWrap: "anywhere", wordBreak: "break-word" }}
                          >
                            <p
                              className="whitespace-pre-wrap leading-relaxed"
                              style={{ overflowWrap: "anywhere", wordBreak: "break-word" }}
                            >
                              {message.content}
                            </p>
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
                    title="Latest"
                    className="storefront-chat-jump absolute bottom-3 left-1/2 inline-flex h-10 w-10 -translate-x-1/2 items-center justify-center rounded-full border border-cyan-300/45 bg-black/78 text-cyan-300 shadow-[0_10px_24px_rgba(0,0,0,0.45)] transition-[transform,background-color,border-color,color] duration-200 hover:-translate-x-1/2 hover:-translate-y-px hover:border-cyan-200 hover:bg-[#13181f] hover:text-cyan-200"
                  >
                    <ArrowDown className="h-4 w-4" />
                  </button>
                ) : null}
              </div>
            )}

            <form onSubmit={onSend} className="storefront-chat-form mt-3 flex gap-2 rounded-2xl p-2">
              <input
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="Write your message..."
                className="storefront-chat-input ui-input min-w-0 flex-1 px-3 py-2 text-sm"
              />
              <button
                type="submit"
                disabled={sending || !draft.trim()}
                className={`group inline-flex h-10 shrink-0 items-center justify-center overflow-hidden rounded-xl bg-[var(--chat-accent)] px-3 text-sm font-semibold text-black transition-[width,transform,filter,opacity] duration-700 hover:-translate-y-px hover:brightness-105 disabled:cursor-not-allowed disabled:opacity-60 ${
                  sending ? "w-28" : "w-10 hover:w-24"
                }`}
              >
                <SendHorizontal className="h-4 w-4 shrink-0 transition-transform duration-700 group-hover:translate-x-0.5" />
                <span
                  className={`overflow-hidden whitespace-nowrap text-xs transition-[max-width,opacity,margin] duration-700 ${
                    sending
                      ? "ml-1.5 max-w-16 opacity-100"
                      : "max-w-0 opacity-0 group-hover:ml-1.5 group-hover:max-w-16 group-hover:opacity-100"
                  }`}
                >
                  {sending ? "Sending..." : "Send"}
                </span>
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
            className="storefront-chat-scrolltop fixed bottom-3 right-5 z-30 inline-flex h-10 w-10 items-center justify-center rounded-full shadow-[0_10px_22px_rgba(0,0,0,0.34)] transition-[transform,background-color,color,border-color] duration-200 hover:-translate-y-px sm:bottom-3 sm:right-6"
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
          className={`storefront-chat-trigger fixed right-5 z-40 inline-flex min-h-14 items-center justify-center gap-2 rounded-full px-4 shadow-[0_12px_24px_rgba(0,0,0,0.36)] transition-[transform,background-color,color,border-color,opacity] duration-200 hover:-translate-y-px sm:right-6 ${
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
            <span className="storefront-chat-badge absolute -right-1 -top-1 inline-flex min-w-5 items-center justify-center rounded-full border border-white/20 bg-black px-1.5 py-0.5 text-[10px] font-bold text-white shadow-[0_6px_16px_rgba(0,0,0,0.32)]">
              {unreadCount > 99 ? "99+" : unreadCount}
            </span>
          ) : null}
        </button>
      ) : null}
    </>
  );
}
