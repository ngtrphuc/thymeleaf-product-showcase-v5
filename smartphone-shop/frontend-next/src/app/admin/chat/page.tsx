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

function upsertChatMessage(messages: ChatMessageResponse[], incoming: ChatMessageResponse): ChatMessageResponse[] {
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

export default function AdminChatPage() {
  const selectedEmailRef = useRef<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const messagesViewportRef = useRef<HTMLDivElement | null>(null);
  const shouldAutoScrollRef = useRef(true);
  const [conversations, setConversations] = useState<AdminConversationsResponse | null>(null);
  const [selectedEmail, setSelectedEmail] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showJumpToLatest, setShowJumpToLatest] = useState(false);

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

  async function loadConversations(options?: { initial?: boolean }) {
    if (options?.initial) {
      setLoading(true);
      setError(null);
    }
    try {
      const data = await fetchAdminConversations();
      setConversations(data);
      if (!selectedEmailRef.current && data.emails.length > 0) {
        setSelectedEmail(data.emails[0]);
      }
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
  }

  async function loadHistory(email: string, options?: { markRead?: boolean }) {
    setError(null);
    try {
      const history = await fetchAdminChatHistory(email);
      setMessages(history);
      if (options?.markRead !== false) {
        await markAdminConversationRead(email);
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load message history.");
      }
    }
  }

  useEffect(() => {
    selectedEmailRef.current = selectedEmail;
  }, [selectedEmail]);

  useEffect(() => {
    void loadConversations({ initial: true });
    const timer = window.setInterval(() => {
      void loadConversations();
    }, 15000);

    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!selectedEmail) {
      setMessages([]);
      setShowJumpToLatest(false);
      return;
    }
    shouldAutoScrollRef.current = true;
    void loadHistory(selectedEmail).finally(() => {
      void loadConversations();
    });
  }, [selectedEmail]);

  useEffect(() => {
    const eventSource = openAdminChatEventStream();
    eventSource.addEventListener("message", (event) => {
      const incoming = parseAdminChatEvent(event.data);
      if (!incoming) {
        return;
      }

      void loadConversations();
      if (selectedEmailRef.current !== incoming.userEmail) {
        return;
      }
      setMessages((current) => upsertChatMessage(current, incoming));
      if (incoming.senderRole === "USER") {
        void markAdminConversationRead(incoming.userEmail);
      }
    });

    return () => {
      eventSource.close();
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
    if (!selectedEmail || !draft.trim()) {
      return;
    }

    setSending(true);
    setError(null);
    try {
      shouldAutoScrollRef.current = true;
      await sendAdminChatMessage(selectedEmail, draft.trim());
      setDraft("");
      await loadHistory(selectedEmail);
      await loadConversations();
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

      <div className="grid gap-4 lg:grid-cols-[260px_1fr]">
        <aside className="glass-panel flex h-[min(72vh,760px)] min-h-[520px] flex-col rounded-3xl p-4">
          <h2 className="text-sm font-semibold text-slate-900">Conversations</h2>
          {!conversations || conversations.emails.length === 0 ? (
            <p className="mt-3 text-sm text-slate-600">No conversations yet.</p>
          ) : (
            <ul className="mt-3 flex-1 space-y-2 overflow-y-auto pr-1">
              {conversations.emails.map((email) => {
                const unread = conversations.unreadCounts[email] ?? 0;
                const active = selectedEmail === email;
                return (
                  <li key={email}>
                    <button
                      type="button"
                      onClick={() => setSelectedEmail(email)}
                      className={`w-full rounded-xl px-3 py-2 text-left text-sm ${
                        active ? "bg-[var(--chat-accent)] text-black" : "bg-[var(--chat-peer-bg)] text-[#f3f4f6]"
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="truncate">{email}</span>
                        {unread > 0 ? (
                          <span
                            className={`rounded-full px-2 py-0.5 text-xs ${
                              active ? "bg-black/10 text-black" : "bg-black/30 text-[#d1d5db]"
                            }`}
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

        <section className="flex h-[min(72vh,760px)] min-h-[520px] flex-col rounded-3xl border border-white/10 bg-black p-4 shadow-[0_12px_28px_rgba(0,0,0,0.35)]">
          {!selectedEmail ? (
            <div className="flex flex-1 items-center justify-center text-sm text-slate-600">
              Select a conversation.
            </div>
          ) : (
            <>
              <div className="mb-3 rounded-xl border border-white/10 bg-[var(--chat-peer-bg)] px-3 py-2 text-sm font-semibold text-[#f3f4f6]">
                {selectedEmail}
              </div>

              <div className="relative flex-1">
                <div
                  ref={messagesViewportRef}
                  onScroll={syncScrollModeFromViewport}
                  className="chat-grid-paper h-full space-y-2 overflow-y-auto rounded-xl border border-white/10 p-3"
                >
                  {messages.length === 0 ? (
                    <p className="text-sm text-[var(--chat-meta)]">No messages yet.</p>
                  ) : (
                    messages.map((message) => {
                      const isAdmin = message.senderRole === "ADMIN";
                      const sideClass = isAdmin ? "justify-end" : "justify-start";
                      const toneClass = isAdmin
                        ? "bg-[var(--chat-accent)] text-black shadow-[0_6px_14px_rgba(74,221,225,0.28)]"
                        : "bg-[var(--chat-peer-bg)] text-[#f3f4f6]";
                      const metaClass = "text-[var(--chat-meta)]";
                      return (
                        <div key={message.id} className={`flex ${sideClass}`}>
                          <div className={`flex max-w-[88%] flex-col gap-1 ${isAdmin ? "items-end" : "items-start"}`}>
                            <article className={`inline-block w-fit break-words rounded-2xl p-2 text-sm ${toneClass}`}>
                              <p className="whitespace-pre-wrap leading-relaxed">{message.content}</p>
                            </article>
                            <p className={`px-1 text-[11px] ${metaClass}`}>
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
                    className="absolute bottom-3 left-1/2 inline-flex h-10 w-10 -translate-x-1/2 items-center justify-center rounded-full border border-cyan-300/45 bg-black/78 text-cyan-300 shadow-[0_10px_24px_rgba(0,0,0,0.45)] transition-[transform,background-color,border-color,color] duration-200 hover:-translate-x-1/2 hover:-translate-y-px hover:border-cyan-200 hover:bg-[#13181f] hover:text-cyan-200"
                  >
                    <ArrowDown className="h-4 w-4" />
                  </button>
                ) : null}
              </div>

              <form onSubmit={onSend} className="mt-3 flex gap-2 rounded-2xl border border-white/10 bg-black/25 p-2">
                <input
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  placeholder="Type a reply..."
                  className="ui-input flex-1 border-white/12 bg-black/35 px-3 py-2 text-sm text-[var(--color-text)]"
                />
                <button
                  type="submit"
                  disabled={sending || !draft.trim()}
                  className={`group inline-flex h-10 items-center justify-center overflow-hidden rounded-xl bg-[var(--chat-accent)] px-3 text-sm font-semibold text-black transition-[width,transform,filter,opacity] duration-700 hover:-translate-y-px hover:brightness-105 disabled:cursor-not-allowed disabled:opacity-60 ${
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
