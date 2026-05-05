/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { ArrowDown, SendHorizontal } from "lucide-react";
import {
  ApiError,
  fetchAdminChatHistory,
  fetchAdminConversations,
  markAdminConversationRead,
  openAdminChatEventStream,
  sendAdminChatMessage,
  type AdminConversationsResponse,
  type ChatMessageResponse,
} from "@/lib/api";
import { formatDateTime } from "@/lib/format";

const MESSAGE_HISTORY_CAP = 50;
const CONVERSATION_REFRESH_THROTTLE_MS = 1500;
const SSE_RECONNECT_BACKOFF_MS = 5000;

type ConversationRefreshOptions = { initial?: boolean; force?: boolean };

function normalizeConversationEmail(email: string | null | undefined): string | null {
  if (!email) {
    return null;
  }
  const normalized = email.trim().toLowerCase();
  return normalized.length > 0 ? normalized : null;
}

function formatChatClock(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return formatDateTime(value);
  }
  const now = new Date();
  const isSameDay =
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth() &&
    date.getDate() === now.getDate();
  if (!isSameDay) {
    return date.toLocaleString("en-GB", {
      day: "2-digit",
      month: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  }
  return date.toLocaleTimeString("en-GB", {
    hour: "2-digit",
    minute: "2-digit",
  });
}

function parseAdminChatEvent(data: string): ChatMessageResponse | null {
  try {
    const parsed = JSON.parse(data) as Partial<ChatMessageResponse>;
    if (
      typeof parsed.userEmail !== "string" ||
      typeof parsed.content !== "string" ||
      typeof parsed.senderRole !== "string" ||
      typeof parsed.createdAt !== "string"
    ) {
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

function upsertChatMessage(messages: ChatMessageResponse[], incoming: ChatMessageResponse): ChatMessageResponse[] {
  if (incoming.id == null) {
    return [...messages, incoming].sort(compareMessagesByTimeline).slice(-MESSAGE_HISTORY_CAP);
  }
  const existingIndex = messages.findIndex((message) => message.id === incoming.id);
  if (existingIndex === -1) {
    return [...messages, incoming].sort(compareMessagesByTimeline).slice(-MESSAGE_HISTORY_CAP);
  }
  const next = [...messages];
  next[existingIndex] = incoming;
  return next.sort(compareMessagesByTimeline).slice(-MESSAGE_HISTORY_CAP);
}

export default function AdminChatPage() {
  const selectedEmailRef = useRef<string | null>(null);
  const historyRequestSequenceRef = useRef(0);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const messagesViewportRef = useRef<HTMLDivElement | null>(null);
  const shouldAutoScrollRef = useRef(true);
  const lastConversationsFetchRef = useRef(0);
  const conversationsFetchTimerRef = useRef<number | null>(null);
  const requestConversationsRefreshRef = useRef<((options?: ConversationRefreshOptions) => void) | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);
  const reconnectTimerRef = useRef<number | null>(null);
  const [conversations, setConversations] = useState<AdminConversationsResponse | null>(null);
  const [selectedEmail, setSelectedEmail] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showJumpToLatest, setShowJumpToLatest] = useState(false);
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
    const distanceToBottom = viewport.scrollHeight - viewport.scrollTop - viewport.clientHeight;
    const nearBottom = distanceToBottom <= 64;
    shouldAutoScrollRef.current = nearBottom;
    setShowJumpToLatest(!nearBottom);
  }, []);

  function scrollToLatest(behavior: ScrollBehavior = "auto") {
    shouldAutoScrollRef.current = true;
    setShowJumpToLatest(false);
    messagesEndRef.current?.scrollIntoView({ block: "end", behavior });
  }

  const markConversationReadLocally = useCallback((email: string) => {
    const normalizedEmail = normalizeConversationEmail(email);
    if (!normalizedEmail) {
      return;
    }

    setConversations((current) => {
      if (!current) {
        return current;
      }

      let changed = false;
      const unreadCounts = { ...current.unreadCounts };
      for (const key of Object.keys(unreadCounts)) {
        if (normalizeConversationEmail(key) === normalizedEmail && unreadCounts[key] !== 0) {
          unreadCounts[key] = 0;
          changed = true;
        }
      }

      if (!changed && unreadCounts[normalizedEmail] !== 0) {
        unreadCounts[normalizedEmail] = 0;
        changed = true;
      }

      return changed ? { ...current, unreadCounts } : current;
    });
  }, []);

  const requestConversationsRefresh = useCallback(async (options?: ConversationRefreshOptions) => {
    const now = Date.now();
    const elapsed = now - lastConversationsFetchRef.current;

    if (!options?.force && !options?.initial && elapsed < CONVERSATION_REFRESH_THROTTLE_MS) {
      if (conversationsFetchTimerRef.current === null) {
        conversationsFetchTimerRef.current = window.setTimeout(() => {
          conversationsFetchTimerRef.current = null;
          requestConversationsRefreshRef.current?.({ force: true });
        }, CONVERSATION_REFRESH_THROTTLE_MS - elapsed);
      }
      return;
    }

    if (conversationsFetchTimerRef.current !== null) {
      window.clearTimeout(conversationsFetchTimerRef.current);
      conversationsFetchTimerRef.current = null;
    }
    lastConversationsFetchRef.current = now;

    if (options?.initial) {
      setLoading(true);
      setError(null);
    }

    try {
      const data = await fetchAdminConversations();
      setConversations(data);
      setSelectedEmail((current) => {
        const normalizedCurrent = normalizeConversationEmail(current);
        const normalizedEmails = new Set<string>();
        let firstEmail: string | null = null;

        for (const email of data.emails) {
          const normalizedEmail = normalizeConversationEmail(email);
          if (!normalizedEmail) {
            continue;
          }
          normalizedEmails.add(normalizedEmail);
          firstEmail ??= normalizedEmail;
        }

        if (!firstEmail) {
          return null;
        }
        if (normalizedCurrent && normalizedEmails.has(normalizedCurrent)) {
          return normalizedCurrent;
        }
        return normalizedCurrent ? null : firstEmail;
      });
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load conversations.");
      }
    } finally {
      if (options?.initial) {
        setLoading(false);
      }
    }
  }, []);

  const loadHistory = useCallback(
    async (email: string) => {
      const normalizedEmail = normalizeConversationEmail(email);
      if (!normalizedEmail) {
        return;
      }

      const requestSequence = ++historyRequestSequenceRef.current;
      setError(null);
      try {
        const history = await fetchAdminChatHistory(normalizedEmail);
        if (
          requestSequence !== historyRequestSequenceRef.current ||
          normalizeConversationEmail(selectedEmailRef.current) !== normalizedEmail
        ) {
          return;
        }
        setMessages(
          history.reduce<ChatMessageResponse[]>((current, message) => upsertChatMessage(current, message), []),
        );
        await markAdminConversationRead(normalizedEmail);
        markConversationReadLocally(normalizedEmail);
      } catch (err) {
        if (err instanceof ApiError) {
          setError(err.message);
        } else {
          setError("Failed to load message history.");
        }
      }
    },
    [markConversationReadLocally],
  );

  useEffect(() => {
    selectedEmailRef.current = selectedEmail;
  }, [selectedEmail]);

  useEffect(() => {
    requestConversationsRefreshRef.current = requestConversationsRefresh;
  }, [requestConversationsRefresh]);

  useEffect(() => {
    function onVisibilityChange() {
      setPageVisible(document.visibilityState === "visible");
    }

    document.addEventListener("visibilitychange", onVisibilityChange);
    return () => {
      document.removeEventListener("visibilitychange", onVisibilityChange);
    };
  }, []);

  useEffect(() => {
    void requestConversationsRefresh({ force: true, initial: true });
  }, [requestConversationsRefresh]);

  useEffect(() => {
    if (!pageVisible) {
      return;
    }
    void requestConversationsRefresh({ force: true });
  }, [pageVisible, requestConversationsRefresh]);

  useEffect(() => {
    if (!selectedEmail) {
      historyRequestSequenceRef.current += 1;
      setMessages([]);
      setShowJumpToLatest(false);
      return;
    }
    shouldAutoScrollRef.current = true;
    void loadHistory(selectedEmail);
  }, [selectedEmail, loadHistory]);

  useEffect(() => {
    if (!pageVisible) {
      return;
    }

    let cancelled = false;

    function connect() {
      if (cancelled) {
        return;
      }

      const eventSource = openAdminChatEventStream();
      eventSourceRef.current = eventSource;

      eventSource.addEventListener("message", (event) => {
        const incoming = parseAdminChatEvent(event.data);
        if (!incoming) {
          return;
        }

        void requestConversationsRefresh();

        if (normalizeConversationEmail(selectedEmailRef.current) !== normalizeConversationEmail(incoming.userEmail)) {
          return;
        }

        setMessages((current) => upsertChatMessage(current, incoming));
        if (incoming.senderRole === "USER") {
          markConversationReadLocally(incoming.userEmail);
          void markAdminConversationRead(incoming.userEmail).catch(() => undefined);
        }
      });

      eventSource.addEventListener("error", () => {
        eventSource.close();
        eventSourceRef.current = null;
        if (cancelled) {
          return;
        }
        if (reconnectTimerRef.current !== null) {
          window.clearTimeout(reconnectTimerRef.current);
        }
        reconnectTimerRef.current = window.setTimeout(() => {
          reconnectTimerRef.current = null;
          connect();
        }, SSE_RECONNECT_BACKOFF_MS);
      });
    }

    connect();

    return () => {
      cancelled = true;
      if (reconnectTimerRef.current !== null) {
        window.clearTimeout(reconnectTimerRef.current);
        reconnectTimerRef.current = null;
      }
      eventSourceRef.current?.close();
      eventSourceRef.current = null;
    };
  }, [markConversationReadLocally, pageVisible, requestConversationsRefresh]);

  useEffect(() => {
    return () => {
      if (conversationsFetchTimerRef.current !== null) {
        window.clearTimeout(conversationsFetchTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (!messages.length || !shouldAutoScrollRef.current) {
      return;
    }
    const frame = window.requestAnimationFrame(() => {
      scrollToLatest("auto");
    });
    return () => {
      window.cancelAnimationFrame(frame);
    };
  }, [messages]);

  async function onSend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const content = draft.trim();
    if (!selectedEmail || !content) {
      return;
    }

    setSending(true);
    setError(null);
    try {
      shouldAutoScrollRef.current = true;
      const sent = await sendAdminChatMessage(selectedEmail, content);
      setMessages((current) => upsertChatMessage(current, sent));
      setDraft("");
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

  if (loading) {
    return <div className="glass-panel rounded-3xl p-8 text-center">Loading admin chat...</div>;
  }

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <h1 className="text-2xl font-bold text-slate-900">Admin Chat</h1>
        <p className="mt-2 text-sm text-slate-600">Customer support messages managed from Next.js admin.</p>
      </header>

      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      <div className="grid gap-4 lg:grid-cols-[260px_minmax(0,1fr)]">
        <aside className="glass-panel admin-chat-sidebar flex h-[clamp(420px,calc(100dvh-22rem),760px)] min-h-0 min-w-0 flex-col overflow-hidden rounded-3xl p-4">
          <h2 className="text-sm font-semibold text-slate-900">Conversations</h2>
          {!conversations || conversations.emails.length === 0 ? (
            <p className="mt-3 text-sm text-slate-600">No conversations yet.</p>
          ) : (
            <ul className="mt-3 flex-1 space-y-2 overflow-y-auto pr-1">
              {conversations.emails.map((email) => {
                const unread = conversations.unreadCounts[email] ?? 0;
                const normalizedEmail = normalizeConversationEmail(email);
                const active = selectedEmail === normalizedEmail;
                return (
                  <li key={email}>
                    <button
                      type="button"
                      onClick={() => setSelectedEmail(normalizedEmail)}
                      aria-current={active ? "true" : "false"}
                      className={`admin-chat-conversation ${active ? "is-active" : "is-inactive"} w-full rounded-xl px-3 py-2 text-left text-sm`}
                    >
                      <div className="flex min-w-0 items-center justify-between gap-2">
                        <span className="min-w-0 truncate">{email}</span>
                        {unread > 0 ? (
                          <span
                            className={`admin-chat-unread shrink-0 rounded-full px-2 py-0.5 text-xs ${active ? "is-active" : "is-inactive"}`}
                          >
                            {unread}
                          </span>
                        ) : null}
                      </div>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </aside>

        <section className="admin-chat-panel flex h-[clamp(420px,calc(100dvh-22rem),760px)] min-h-0 min-w-0 flex-col overflow-hidden rounded-3xl p-4">
          {!selectedEmail ? (
            <div className="flex flex-1 items-center justify-center text-sm text-slate-600">
              Select a conversation.
            </div>
          ) : (
            <>
              <div className="admin-chat-selected mb-3 truncate rounded-xl border border-white/10 bg-[var(--chat-peer-bg)] px-3 py-2 text-sm font-semibold text-[var(--color-text)]">
                {selectedEmail}
              </div>

              <div className="relative min-h-0 min-w-0 flex-1">
                <div
                  ref={messagesViewportRef}
                  onScroll={syncScrollModeFromViewport}
                  className="admin-chat-messages chat-grid-paper h-full min-w-0 space-y-2 overflow-x-hidden overflow-y-auto rounded-xl p-3"
                >
                  {messages.length === 0 ? (
                    <p className="text-sm text-[var(--chat-meta)]">No messages yet.</p>
                  ) : (
                    messages.map((message, index) => {
                      const isAdmin = message.senderRole === "ADMIN";
                      const sideClass = isAdmin ? "justify-end" : "justify-start";
                      const toneClass = isAdmin
                        ? "bg-[var(--chat-accent)] text-black shadow-[0_6px_14px_rgba(0,0,0,0.12)]"
                        : "admin-chat-peer border border-white/10 bg-[var(--chat-peer-bg)] text-[var(--color-text)]";
                      return (
                        <div
                          key={message.id ?? `${message.userEmail}:${message.createdAt}:${message.senderRole}:${index}`}
                          className={`flex w-full min-w-0 ${sideClass}`}
                        >
                          <div
                            className={`flex max-w-[82%] min-w-0 flex-col gap-1 sm:max-w-[75%] ${isAdmin ? "items-end" : "items-start"}`}
                          >
                            <article
                              className={`inline-block max-w-full rounded-2xl px-3 py-2 text-sm ${toneClass}`}
                              style={{ overflowWrap: "anywhere", wordBreak: "break-word" }}
                            >
                              <p
                                className="whitespace-pre-wrap leading-relaxed"
                                style={{ overflowWrap: "anywhere", wordBreak: "break-word" }}
                              >
                                {message.content}
                              </p>
                            </article>
                            <p className="px-1 text-[11px] text-[var(--chat-meta)]">
                              {message.senderRole} | {formatChatClock(message.createdAt)}
                            </p>
                          </div>
                        </div>
                      );
                    })
                  )}
                  <div ref={messagesEndRef} />
                </div>
                {showJumpToLatest ? (
                  <button
                    type="button"
                    onClick={() => scrollToLatest("smooth")}
                    aria-label="Jump to latest message"
                    title="Latest"
                    className="admin-chat-jump absolute bottom-3 left-1/2 inline-flex h-10 w-10 -translate-x-1/2 items-center justify-center rounded-full border border-cyan-300/45 bg-black/78 text-cyan-300 shadow-[0_10px_24px_rgba(0,0,0,0.45)] transition-[transform,background-color,border-color,color] duration-200 hover:-translate-x-1/2 hover:-translate-y-px hover:border-cyan-200 hover:bg-[#13181f] hover:text-cyan-200"
                  >
                    <ArrowDown className="h-4 w-4" />
                  </button>
                ) : null}
              </div>

              <form onSubmit={onSend} className="admin-chat-form mt-3 flex gap-2 rounded-2xl p-2">
                <input
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  placeholder="Type a reply..."
                  className="admin-chat-input ui-input min-w-0 flex-1 px-3 py-2 text-sm text-[var(--color-text)]"
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
            </>
          )}
        </section>
      </div>
    </div>
  );
}
