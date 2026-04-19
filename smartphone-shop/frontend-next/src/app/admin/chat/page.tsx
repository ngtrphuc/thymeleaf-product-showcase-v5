/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  ApiError,
  fetchAdminChatHistory,
  fetchAdminConversations,
  markAdminConversationRead,
  sendAdminChatMessage,
  type AdminConversationsResponse,
  type ChatMessageResponse,
} from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import { GriddyIcon } from "@/components/ui/griddy-icon";

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

export default function AdminChatPage() {
  const [conversations, setConversations] = useState<AdminConversationsResponse | null>(null);
  const [selectedEmail, setSelectedEmail] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function loadConversations() {
    setLoading(true);
    setError(null);
    try {
      const data = await fetchAdminConversations();
      setConversations(data);
      if (!selectedEmail && data.emails.length > 0) {
        setSelectedEmail(data.emails[0]);
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load conversations.");
      }
    } finally {
      setLoading(false);
    }
  }

  async function loadHistory(email: string) {
    setError(null);
    try {
      const history = await fetchAdminChatHistory(email);
      setMessages(history);
      await markAdminConversationRead(email);
      const refreshed = await fetchAdminConversations();
      setConversations(refreshed);
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load message history.");
      }
    }
  }

  useEffect(() => {
    void loadConversations();
    const timer = setInterval(() => {
      void loadConversations();
      if (selectedEmail) {
        void loadHistory(selectedEmail);
      }
    }, 8000);

    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if (!selectedEmail) {
      setMessages([]);
      return;
    }
    void loadHistory(selectedEmail);
  }, [selectedEmail]);

  async function onSend(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selectedEmail || !draft.trim()) {
      return;
    }

    setSending(true);
    setError(null);
    try {
      await sendAdminChatMessage(selectedEmail, draft.trim());
      setDraft("");
      await loadHistory(selectedEmail);
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
        <aside className="glass-panel rounded-3xl p-4">
          <h2 className="text-sm font-semibold text-slate-900">Conversations</h2>
          {!conversations || conversations.emails.length === 0 ? (
            <p className="mt-3 text-sm text-slate-600">No conversations yet.</p>
          ) : (
            <ul className="mt-3 space-y-2">
              {conversations.emails.map((email) => {
                const unread = conversations.unreadCounts[email] ?? 0;
                const active = selectedEmail === email;
                return (
                  <li key={email}>
                    <button
                      type="button"
                      onClick={() => setSelectedEmail(email)}
                      className={`w-full rounded-xl px-3 py-2 text-left text-sm ${
                        active ? "bg-[var(--color-primary)] text-black" : "bg-white text-slate-800"
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="truncate">{email}</span>
                        {unread > 0 ? (
                          <span className={`rounded-full px-2 py-0.5 text-xs ${active ? "bg-black/10" : "bg-slate-100"}`}>
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

        <section className="glass-panel flex min-h-[460px] flex-col rounded-3xl p-4">
          {!selectedEmail ? (
            <div className="flex flex-1 items-center justify-center text-sm text-slate-600">
              Select a conversation.
            </div>
          ) : (
            <>
              <div className="mb-3 rounded-xl bg-white px-3 py-2 text-sm font-semibold text-slate-900">
                {selectedEmail}
              </div>

              <div className="flex-1 space-y-2 overflow-y-auto rounded-xl border border-[var(--color-border)] bg-white p-3">
                {messages.length === 0 ? (
                  <p className="text-sm text-slate-600">No messages yet.</p>
                ) : (
                  messages.map((message) => {
                    const isAdmin = message.senderRole === "ADMIN";
                    const sideClass = isAdmin ? "justify-end" : "justify-start";
                    const toneClass = isAdmin
                      ? "bg-[var(--color-primary)] text-black"
                      : "bg-slate-50 text-slate-800";
                    const metaClass = isAdmin ? "text-black/60" : "text-slate-500";
                    return (
                      <div key={message.id} className={`flex ${sideClass}`}>
                        <div className={`flex max-w-[88%] flex-col gap-1 ${isAdmin ? "items-end" : "items-start"}`}>
                          <article className={`inline-block w-fit break-words rounded-lg p-2 text-sm ${toneClass}`}>
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
              </div>

              <form onSubmit={onSend} className="mt-3 flex gap-2">
                <input
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                  placeholder="Type a reply..."
                  className="ui-input flex-1 px-3 py-2 text-sm"
                />
                <button
                  type="submit"
                  disabled={sending || !draft.trim()}
                  className="ui-btn ui-btn-primary inline-flex items-center gap-2 px-4 py-2 text-sm"
                >
                  <GriddyIcon name="chat" />
                  {sending ? "Sending..." : "Send"}
                </button>
              </form>
            </>
          )}
        </section>
      </div>
    </div>
  );
}

