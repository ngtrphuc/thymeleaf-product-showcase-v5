/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { ApiError, fetchChatHistory, markChatRead, sendChatMessage, type ChatMessageResponse } from "@/lib/api";
import { formatDateTime } from "@/lib/format";
import { GriddyIcon } from "@/components/ui/griddy-icon";

export default function StorefrontChatPage() {
  const [messages, setMessages] = useState<ChatMessageResponse[]>([]);
  const [draft, setDraft] = useState("");
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadHistory = useCallback(async (markReadAfterLoad: boolean) => {
    setError(null);
    try {
      const history = await fetchChatHistory();
      setMessages(history);
      if (markReadAfterLoad) {
        await markChatRead();
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setError(err.message);
      } else {
        setError("Failed to load chat history.");
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadHistory(true);
    const timer = setInterval(() => {
      void loadHistory(false);
    }, 8000);
    return () => clearInterval(timer);
  }, [loadHistory]);

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
      await loadHistory(true);
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
    return <div className="glass-panel rounded-3xl p-8 text-center">Loading chat...</div>;
  }

  return (
    <div className="space-y-6">
      <header className="glass-panel rounded-3xl p-6">
        <h1 className="text-2xl font-bold text-slate-900">Chat With Shop</h1>
        <p className="mt-2 text-sm text-slate-600">Ask product, stock, shipping, or order questions in real time.</p>
      </header>

      {error ? <p className="text-sm text-red-700">{error}</p> : null}

      <section className="glass-panel rounded-3xl p-4">
        {messages.length === 0 ? (
          <div className="flex h-44 items-center justify-center rounded-2xl border border-[var(--color-border)] bg-white text-sm text-slate-600">
            Start a conversation with the shop.
          </div>
        ) : (
          <div className="max-h-[360px] min-h-[120px] space-y-3 overflow-y-auto rounded-2xl border border-[var(--color-border)] bg-white p-3">
            {messages.map((message) => {
              const isUser = message.senderRole === "USER";
              return (
                <article
                  key={message.id}
                  className={`max-w-[88%] rounded-xl px-3 py-2 text-sm ${
                    isUser ? "ml-auto bg-[var(--color-primary)] text-white" : "bg-slate-100 text-slate-800"
                  }`}
                >
                  <p>{message.content}</p>
                  <p className={`mt-1 text-[11px] ${isUser ? "text-white/80" : "text-slate-500"}`}>
                    {isUser ? "You" : "Shop"} | {formatDateTime(message.createdAt)}
                  </p>
                </article>
              );
            })}
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
            <GriddyIcon name="chat" />
            {sending ? "Sending..." : "Send"}
          </button>
        </form>
      </section>
    </div>
  );
}
