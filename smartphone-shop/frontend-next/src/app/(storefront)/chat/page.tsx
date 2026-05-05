/* eslint-disable react-hooks/set-state-in-effect */
"use client";

import { FormEvent, useCallback, useEffect, useState } from "react";
import { ApiError, fetchChatHistory, markChatRead, sendChatMessage, type ChatMessageResponse } from "@/lib/api";
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
      void loadHistory(document.hasFocus());
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

      <section className="storefront-chat-page-panel rounded-3xl p-4">
        {messages.length === 0 ? (
          <div className="storefront-chat-state chat-grid-paper flex h-44 items-center justify-center rounded-2xl text-sm text-[var(--chat-meta)]">
            Start a conversation with the shop.
          </div>
        ) : (
          <div className="storefront-chat-messages chat-grid-paper max-h-[360px] min-h-[120px] space-y-3 overflow-y-auto rounded-2xl p-3">
            {messages.map((message) => {
              const isUser = message.senderRole === "USER";
              const sideClass = isUser ? "justify-end" : "justify-start";
              const toneClass = isUser
                ? "bg-[var(--chat-accent)] text-black shadow-[0_6px_14px_rgba(0,0,0,0.12)]"
                : "storefront-chat-peer bg-[var(--chat-peer-bg)] text-[var(--color-text)]";
              const metaClass = "text-[var(--chat-meta)]";
              return (
                <div key={message.id} className={`flex ${sideClass}`}>
                  <div className={`flex max-w-[88%] flex-col gap-1 ${isUser ? "items-end" : "items-start"}`}>
                    <article className={`inline-block w-fit break-words rounded-2xl px-3 py-2 text-sm ${toneClass}`}>
                      <p className="whitespace-pre-wrap leading-relaxed">{message.content}</p>
                    </article>
                    <p className={`px-1 text-[11px] ${metaClass}`}>
                      {isUser ? "You" : "Shop"} | {formatChatClock(message.createdAt)}
                    </p>
                  </div>
                </div>
              );
            })}
          </div>
        )}

        <form onSubmit={onSend} className="storefront-chat-form mt-3 flex gap-2 rounded-2xl p-2">
          <input
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="Write your message..."
            className="storefront-chat-input ui-input flex-1 px-3 py-2 text-sm text-[var(--color-text)]"
          />
          <button
            type="submit"
            disabled={sending || !draft.trim()}
            className="inline-flex items-center gap-2 rounded-xl bg-[var(--chat-accent)] px-4 py-2 text-sm font-semibold text-black transition-[transform,filter] duration-200 hover:-translate-y-px hover:brightness-105 disabled:cursor-not-allowed disabled:opacity-60"
          >
            <GriddyIcon name="chat" />
            {sending ? "Sending..." : "Send"}
          </button>
        </form>
      </section>
    </div>
  );
}
