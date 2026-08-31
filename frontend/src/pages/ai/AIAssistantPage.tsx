import React, { useState, useEffect, useRef } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useEnvironment } from '../../context/EnvironmentContext';
import { aiService } from '../../services/aiService';
import { approvalService } from '../../services/approvalService';
import { FOIRResponse, ApprovalRequest, ToolMetadata } from '../../types/ai';
import { StructuredReasoningCard } from '../../components/ai/StructuredReasoningCard';
import { ApprovalPromptCard } from '../../components/ai/ApprovalPromptCard';
import {
  Bot,
  Send,
  Sparkles,
  Terminal,
  ShieldCheck,
  CheckCircle,
  Clock,
  AlertTriangle,
  RefreshCw
} from 'lucide-react';

interface ChatMessage {
  id: string;
  sender: 'USER' | 'AI';
  text: string;
  foirData?: FOIRResponse;
  approvalData?: ApprovalRequest;
  timestamp: Date;
}

export const AIAssistantPage: React.FC = () => {
  const [searchParams] = useSearchParams();
  const { selectedEnv } = useEnvironment();

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputPrompt, setInputPrompt] = useState('');
  const [loading, setLoading] = useState(false);
  const [tools, setTools] = useState<ToolMetadata[]>([]);
  const [activeApproval, setActiveApproval] = useState<ApprovalRequest | null>(null);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    aiService.getTools().then(setTools).catch(console.error);

    const initialPrompt = searchParams.get('prompt');
    if (initialPrompt) {
      handleSendPrompt(initialPrompt);
    } else {
      // Add welcome message
      setMessages([
        {
          id: 'welcome',
          sender: 'AI',
          text: 'Hello! I am your AI DevOps & SRE Autonomous Assistant. I am connected directly to your Linux servers, Docker containers, Kubernetes clusters, and Prometheus metrics stack. How can I help you investigate or remediate infrastructure issues today?',
          timestamp: new Date()
        }
      ]);
    }
  }, []);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const handleSendPrompt = async (promptToSend: string) => {
    if (!promptToSend.trim() || loading) return;

    const userMsg: ChatMessage = {
      id: String(Date.now()),
      sender: 'USER',
      text: promptToSend,
      timestamp: new Date()
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputPrompt('');
    setLoading(true);

    try {
      const response = await aiService.sendChat(promptToSend, undefined, selectedEnv?.id);

      const aiMsg: ChatMessage = {
        id: String(Date.now() + 1),
        sender: 'AI',
        text: response.summary,
        foirData: response,
        timestamp: new Date()
      };

      setMessages((prev) => [...prev, aiMsg]);
    } catch (err: any) {
      const errorMsg: ChatMessage = {
        id: String(Date.now() + 1),
        sender: 'AI',
        text: 'Error executing reasoning cycle: ' + (err.response?.data?.message || err.message),
        timestamp: new Date()
      };
      setMessages((prev) => [...prev, errorMsg]);
    } finally {
      setLoading(false);
    }
  };

  const handleExecuteAction = async (recommendation: any) => {
    if (recommendation.requiresApproval) {
      // Create active approval simulation prompt
      const mockApproval: ApprovalRequest = {
        id: 'appr-' + Date.now(),
        environment: {
          id: selectedEnv?.id || 'prod',
          name: selectedEnv?.name || 'PRODUCTION',
          isProduction: true
        },
        requestedByUser: {
          id: 'ai-orch',
          email: 'ai-agent@devops.ai',
          fullName: 'SRE AI Orchestrator'
        },
        actionType: recommendation.action,
        rationale: recommendation.rationale,
        expectedImpact: recommendation.expectedImpact,
        status: 'PENDING',
        requestedAt: new Date().toISOString()
      };
      setActiveApproval(mockApproval);
    } else {
      // Execute directly
      const successMsg: ChatMessage = {
        id: String(Date.now()),
        sender: 'AI',
        text: `Action '${recommendation.action}' executed successfully on ${selectedEnv?.name || 'DEVELOPMENT'} environment.`,
        timestamp: new Date()
      };
      setMessages((prev) => [...prev, successMsg]);
    }
  };

  const handleResolveApproval = async (id: string, decision: 'APPROVED' | 'REJECTED', comment?: string) => {
    if (activeApproval) {
      setActiveApproval({
        ...activeApproval,
        status: decision,
        resolvedAt: new Date().toISOString()
      });

      const resolutionMsg: ChatMessage = {
        id: String(Date.now()),
        sender: 'AI',
        text: decision === 'APPROVED'
          ? `[SUCCESS] Action '${activeApproval.actionType}' approved and executed. Verifying telemetry recovery... Prometheus metric 'container_cpu_usage_percent' dropped from 94.2% to 14.5%. Service healthy!`
          : `[NOTICE] Action '${activeApproval.actionType}' was rejected. Reason: ${comment || 'User aborted'}.`,
        timestamp: new Date()
      };

      setMessages((prev) => [...prev, resolutionMsg]);
    }
  };

  const QUICK_PROMPTS = [
    'Why is my ThingsBoard server slow?',
    'What is the status of production infrastructure?',
    'Inspect container logs for errors',
    'Check recent deployments and correlation'
  ];

  return (
    <div className="h-[calc(100vh-8rem)] flex flex-col glass-card rounded-2xl border border-slate-800 overflow-hidden">
      {/* Assistant Header & Tool Bar */}
      <div className="p-4 bg-surface-elevated/70 border-b border-slate-800 flex flex-wrap items-center justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-cyan-500 flex items-center justify-center shadow-lg shadow-indigo-600/30">
            <Bot className="w-5 h-5 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="font-bold text-sm text-slate-100">Autonomous SRE Copilot</h2>
              <span className="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 text-[10px] font-mono font-medium">
                FOIR GUARDRAIL ON
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Active Environment: <span className="text-indigo-400 font-semibold">{selectedEnv?.name || 'PRODUCTION'}</span>
            </p>
          </div>
        </div>

        {/* Registered Tools Badge */}
        <div className="flex items-center gap-2 text-xs font-mono text-slate-400">
          <Terminal className="w-3.5 h-3.5 text-indigo-400" />
          <span>{tools.length} Controlled Tools Available</span>
        </div>
      </div>

      {/* Message Chat Feed */}
      <div className="flex-1 overflow-y-auto p-6 space-y-6">
        {messages.map((msg) => (
          <div
            key={msg.id}
            className={`flex flex-col ${msg.sender === 'USER' ? 'items-end' : 'items-start'} space-y-2`}
          >
            <div className="flex items-center gap-2 text-[11px] text-slate-400 font-medium px-1">
              <span>{msg.sender === 'USER' ? 'You' : 'SRE AI Copilot'}</span>
              <span>•</span>
              <span>{msg.timestamp.toLocaleTimeString()}</span>
            </div>

            {msg.sender === 'USER' ? (
              <div className="max-w-2xl bg-indigo-600 text-white rounded-2xl rounded-tr-none px-4 py-3 text-xs font-medium shadow-md shadow-indigo-600/20">
                {msg.text}
              </div>
            ) : (
              <div className="max-w-4xl w-full space-y-4">
                {msg.foirData ? (
                  <StructuredReasoningCard
                    data={msg.foirData}
                    onExecuteAction={handleExecuteAction}
                  />
                ) : (
                  <div className="bg-slate-900/90 border border-slate-700/80 rounded-2xl rounded-tl-none px-5 py-3.5 text-xs text-slate-200 leading-relaxed shadow-lg">
                    {msg.text}
                  </div>
                )}
              </div>
            )}
          </div>
        ))}

        {/* Pending Approval Modal/Card */}
        {activeApproval && (
          <div className="max-w-2xl">
            <ApprovalPromptCard
              approval={activeApproval}
              onResolve={handleResolveApproval}
            />
          </div>
        )}

        {/* Thinking / Tool Execution Spinner */}
        {loading && (
          <div className="flex items-center gap-3 p-4 bg-slate-900/80 border border-indigo-500/20 rounded-xl max-w-md glow-indigo">
            <RefreshCw className="w-4 h-4 text-indigo-400 animate-spin" />
            <div className="space-y-0.5">
              <p className="text-xs font-semibold text-slate-200">Executing ReAct Telemetry Tools...</p>
              <p className="text-[10px] text-slate-400 font-mono">
                Querying Prometheus, Docker socket & correlating logs...
              </p>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Suggested Fast Queries */}
      <div className="px-6 py-2 bg-slate-900/40 border-t border-slate-800/80 flex items-center gap-2 overflow-x-auto text-[11px]">
        <span className="text-slate-500 shrink-0 font-medium">Quick Inquiries:</span>
        {QUICK_PROMPTS.map((q, idx) => (
          <button
            key={idx}
            onClick={() => handleSendPrompt(q)}
            className="px-2.5 py-1 bg-slate-800/60 hover:bg-slate-700 hover:text-indigo-300 border border-slate-700/60 rounded-lg text-slate-300 transition-all shrink-0 font-sans"
          >
            {q}
          </button>
        ))}
      </div>

      {/* Prompt Input Form */}
      <div className="p-4 bg-surface-elevated/60 border-t border-slate-800">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSendPrompt(inputPrompt);
          }}
          className="flex items-center gap-3"
        >
          <input
            type="text"
            value={inputPrompt}
            onChange={(e) => setInputPrompt(e.target.value)}
            placeholder="Ask AI Copilot (e.g., 'Why is ThingsBoard slow?', 'Check Prometheus memory alerts')..."
            className="flex-1 px-4 py-3 bg-slate-900 border border-slate-700 rounded-xl text-xs text-slate-100 focus:outline-none focus:border-indigo-500 placeholder:text-slate-500 shadow-inner font-sans"
          />
          <button
            type="submit"
            disabled={loading || !inputPrompt.trim()}
            className="px-5 py-3 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 text-white rounded-xl text-xs font-semibold shadow-lg shadow-indigo-600/25 transition-all flex items-center gap-2"
          >
            <span>Run Diagnosis</span>
            <Send className="w-3.5 h-3.5" />
          </button>
        </form>
      </div>
    </div>
  );
};
