"use client";

import { FormEvent, useEffect, useRef, useState } from "react";
import { usePathname } from "next/navigation";
import { MessageSquare, X } from "lucide-react";
import {
  ApiError,
  fetchAuthMe,
  fetchChatHistory,
  fetchChatUnreadCount,
  markChatRead,
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

export function StorefrontChatBubble() {
  const pathname = usePathname();
  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const unreadCountRef = useRef(0);
  const pulseTimerRef = useRef<number | null>(null);
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

  useEffect(() => {
    let alive = true;

    async function resolveAuth() {
      try {
        const response = await fetchAuthMe();
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

  const shouldShow =
    authState.authenticated &&
    authState.role !== "ROLE_ADMIN" &&
    pathname !== "/chat" &&
    !pathname.startsWith("/chat/");

  useEffect(() => {
    if (!shouldShow) {
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
            setHasUnreadPulse(true);
            if (pulseTimerRef.current !== null) {
              window.clearTimeout(pulseTimerRef.current);
            }
            pulseTimerRef.current = window.setTimeout(() => {
              setHasUnreadPulse(false);
              pulseTimerRef.current = null;
            }, 2200);
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
    const timerId = window.setInterval(() => {
      void syncChat();
    }, 8000);

    return () => {
      alive = false;
      window.clearInterval(timerId);
    };
  }, [open, shouldShow]);

  useEffect(() => {
    if (!open || !messagesEndRef.current) {
      return;
    }
    messagesEndRef.current.scrollIntoView({ block: "end" });
  }, [messages, open]);

  useEffect(() => {
    return () => {
      if (pulseTimerRef.current !== null) {
        window.clearTimeout(pulseTimerRef.current);
      }
    };
  }, []);

  async function openChat() {
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
    try {
      await sendChatMessage(content);
      setDraft("");
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
        <section className="fixed bottom-5 right-5 z-40 w-[min(24rem,calc(100vw-1.5rem))] overflow-hidden rounded-[1.6rem] border border-white/10 bg-[var(--color-surface)] shadow-[0_22px_54px_rgba(0,0,0,0.48)] backdrop-blur-xl sm:bottom-6 sm:right-6">
          <header className="flex items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface-soft)] px-4 py-3">
            <div>
              <h2 className="text-sm font-semibold text-slate-900">Chat With Shop</h2>
              <p className="text-xs text-slate-600">Ask about products, orders, or shipping.</p>
            </div>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setOpen(false)}
                aria-label="Close chat"
                className="inline-flex h-8 w-8 items-center justify-center rounded-full border border-[var(--color-border)] bg-[var(--color-surface)] text-[var(--color-text-muted)] transition-[background-color,color,border-color,transform] duration-200 hover:-translate-y-0.5 hover:border-white/10 hover:bg-white hover:text-black"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
          </header>

          <div className="px-4 pb-4 pt-3">
            {error ? <p className="mb-3 text-sm text-red-700">{error}</p> : null}

            {loadingChat ? (
              <div className="flex h-72 items-center justify-center rounded-2xl border border-[var(--color-border)] bg-white text-sm text-slate-600">
                Loading chat...
              </div>
            ) : messages.length === 0 ? (
              <div className="flex h-72 items-center justify-center rounded-2xl border border-[var(--color-border)] bg-white text-sm text-slate-600">
                Start a conversation with the shop.
              </div>
            ) : (
              <div className="max-h-[20rem] min-h-[18rem] space-y-3 overflow-y-auto rounded-2xl border border-[var(--color-border)] bg-white p-3">
                {messages.map((message) => {
                  const isUser = message.senderRole === "USER";
                  const sideClass = isUser ? "justify-end" : "justify-start";
                  const toneClass = isUser
                    ? "bg-[var(--color-primary)] text-black"
                    : "bg-slate-100 text-slate-800";
                  const metaClass = isUser ? "text-black/60" : "text-slate-500";
                  return (
                    <div key={message.id} className={`flex ${sideClass}`}>
                      <div className={`flex max-w-[88%] flex-col gap-1 ${isUser ? "items-end" : "items-start"}`}>
                        <article className={`inline-block w-fit break-words rounded-xl px-3 py-2 text-sm ${toneClass}`}>
                          <p className="whitespace-pre-wrap leading-relaxed">{message.content}</p>
                        </article>
                        <p className={`px-1 text-[11px] ${metaClass}`}>
                          {isUser ? "You" : "Shop"} | {formatChatClock(message.createdAt)}
                        </p>
                      </div>
                    </div>
                  );
                })}
                <div ref={messagesEndRef} />
              </div>
            )}

            <form onSubmit={onSend} className="mt-3 flex gap-2">
              <input
                value={draft}
                onChange={(event) => setDraft(event.target.value)}
                placeholder="Write your message..."
                className="ui-input flex-1 px-3 py-2 text-sm"
              />
              <button
                type="submit"
                disabled={sending || !draft.trim()}
                className="ui-btn ui-btn-primary inline-flex items-center gap-2 px-4 py-2 text-sm"
              >
                {sending ? "Sending..." : "Send"}
              </button>
            </form>
          </div>
        </section>
      ) : null}

      {!open ? (
        <button
          type="button"
          onClick={() => void openChat()}
          aria-label="Open chat"
          title="Open chat"
          className={`fixed bottom-5 right-5 z-40 inline-flex min-h-14 items-center justify-center gap-2 rounded-full border border-white/12 bg-[var(--color-surface-soft)] px-4 text-[var(--color-text-muted)] shadow-[0_18px_40px_rgba(0,0,0,0.45)] transition-[transform,background-color,color,border-color,box-shadow] duration-200 hover:-translate-y-1 hover:border-white/10 hover:bg-white hover:text-black hover:shadow-[0_20px_44px_rgba(0,0,0,0.52)] sm:bottom-6 sm:right-6 ${
            hasUnreadPulse ? "animate-pulse" : ""
          }`}
        >
          <MessageSquare className="h-5 w-5" />
          {unreadCount > 0 ? (
            <span className="text-xs font-semibold leading-none text-slate-900">
              {unreadCount > 99 ? "99+" : unreadCount} unread
            </span>
          ) : null}
          {unreadCount > 0 ? (
            <span className="absolute -right-1 -top-1 inline-flex min-w-5 items-center justify-center rounded-full bg-white px-1.5 py-0.5 text-[10px] font-bold text-black shadow-[0_6px_16px_rgba(0,0,0,0.32)]">
              {unreadCount > 99 ? "99+" : unreadCount}
            </span>
          ) : null}
        </button>
      ) : null}
    </>
  );
}
