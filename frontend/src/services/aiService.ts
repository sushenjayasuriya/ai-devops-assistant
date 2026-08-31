import { api } from './api';
import { FOIRResponse, ToolMetadata } from '../types/ai';

export const aiService = {
  async getTools(): Promise<ToolMetadata[]> {
    const res = await api.get('/ai/tools');
    return res.data.data;
  },

  async sendChat(prompt: string, conversationId?: string, environmentId?: string): Promise<FOIRResponse> {
    const res = await api.post('/ai/chat', { prompt, environmentId }, {
      params: { conversationId }
    });
    return res.data.data;
  },

  async getConversations(): Promise<any[]> {
    const res = await api.get('/ai/conversations');
    return res.data.data;
  },

  async getMessages(conversationId: string): Promise<any[]> {
    const res = await api.get(`/ai/conversations/${conversationId}/messages`);
    return res.data.data;
  }
};
