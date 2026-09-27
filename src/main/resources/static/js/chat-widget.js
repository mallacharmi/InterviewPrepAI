(function() {
    let contextType = 'PREP';
    let contextId = null;
    let targetRole = null;
    let conversationHistory = [];

    document.addEventListener('DOMContentLoaded', () => {
        const root = document.getElementById('ai-chat-widget-root');
        if (!root) return;

        contextType = root.getAttribute('data-context-type') || 'PREP';
        const rawId = root.getAttribute('data-context-id');
        contextId = (rawId && rawId !== 'null' && rawId !== '') ? parseInt(rawId) : null;
        targetRole = root.getAttribute('data-target-role') || null;

        setupChatWidgetEventListeners();
    });

    function setupChatWidgetEventListeners() {
        const toggleBtn = document.getElementById('chat-widget-toggle');
        const closeBtn = document.getElementById('chat-widget-close');
        const card = document.getElementById('chat-widget-card');

        if (toggleBtn) {
            toggleBtn.addEventListener('click', () => {
                if (card) {
                    const isHidden = card.style.display === 'none' || !card.style.display;
                    card.style.display = isHidden ? 'flex' : 'none';
                    if (isHidden) {
                        loadChatHistory();
                        const input = document.getElementById('chat-widget-input');
                        if (input) input.focus();
                    }
                }
            });
        }

        if (closeBtn) {
            closeBtn.addEventListener('click', () => {
                if (card) card.style.display = 'none';
            });
        }

        const clearBtn = document.getElementById('chat-widget-clear');
        if (clearBtn) {
            clearBtn.addEventListener('click', async () => {
                if (contextType === 'REVIEW') return;
                if (!confirm('Clear chat history for this mode?')) return;
                try {
                    let url = `/api/chat/history?contextType=${contextType}`;
                    if (contextId) url += `&contextId=${contextId}`;
                    const fetchFn = typeof window.apiCall === 'function' ? window.apiCall : apiCall;
                    await fetchFn(url, 'DELETE');

                    conversationHistory = [];
                    const body = document.getElementById('chat-widget-body');
                    if (body) body.innerHTML = '';
                    appendWelcomeMessage();
                } catch (err) {
                    console.error('Failed to clear chat history:', err);
                }
            });
        }
    }

    async function loadChatHistory() {
        const body = document.getElementById('chat-widget-body');
        if (!body) return;

        try {
            let url = `/api/chat/history?contextType=${contextType}`;
            if (contextId) {
                url += `&contextId=${contextId}`;
            }
            
            const fetchFn = typeof window.apiCall === 'function' ? window.apiCall : apiCall;
            const history = await fetchFn(url);

            body.innerHTML = '';
            conversationHistory = history || [];

            if (conversationHistory.length === 0) {
                appendWelcomeMessage();
            } else {
                conversationHistory.forEach(msg => {
                    appendMessageBubble(msg.role, msg.content);
                });
            }
            scrollToBottom();
        } catch (e) {
            console.warn('Could not load chat history:', e);
            body.innerHTML = '';
            appendWelcomeMessage();
        }
    }

    function appendWelcomeMessage() {
        let text = '';
        if (contextType === 'PREP') {
            text = `👋 Hi! I'm your AI Interview Coach. Ask me anything about preparing for your upcoming target role!`;
        } else if (contextType === 'REVIEW') {
            text = `📊 Hello! I am your Session Tutor. Ask me why marks were deducted or for ideal answers to your session's questions!`;
        } else if (contextType === 'RESUME') {
            text = `📝 Welcome! I am your ATS Resume Advisor. Ask me for specific bullet point rewording to integrate missing keywords!`;
        }
        appendMessageBubble('assistant', text);
    }

    function appendMessageBubble(role, text) {
        const body = document.getElementById('chat-widget-body');
        if (!body) return;

        const bubble = document.createElement('div');
        bubble.className = `chat-bubble chat-bubble-${role}`;

        const avatar = role === 'user' ? '👤' : '🤖';
        const formattedText = escapeHtml(text).replace(/\n/g, '<br>');

        bubble.innerHTML = `
            <span class="chat-bubble-avatar">${avatar}</span>
            <div class="chat-bubble-text">${formattedText}</div>
        `;

        body.appendChild(bubble);
        scrollToBottom();
    }

    function showTypingIndicator() {
        const body = document.getElementById('chat-widget-body');
        if (!body) return;

        const indicator = document.createElement('div');
        indicator.id = 'chat-typing-indicator';
        indicator.className = 'chat-bubble chat-bubble-assistant typing';
        indicator.innerHTML = `
            <span class="chat-bubble-avatar">🤖</span>
            <div class="chat-bubble-text">
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
                <span class="typing-dot"></span>
            </div>
        `;
        body.appendChild(indicator);
        scrollToBottom();
    }

    function hideTypingIndicator() {
        const el = document.getElementById('chat-typing-indicator');
        if (el) el.remove();
    }

    function scrollToBottom() {
        const body = document.getElementById('chat-widget-body');
        if (body) {
            body.scrollTop = body.scrollHeight;
        }
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    window.sendChatMessage = async function() {
        const input = document.getElementById('chat-widget-input');
        const sendBtn = document.getElementById('chat-widget-send');
        if (!input) return;

        const messageText = input.value.trim();
        if (!messageText) return;

        input.value = '';
        if (sendBtn) sendBtn.disabled = true;

        appendMessageBubble('user', messageText);
        showTypingIndicator();

        // Gather context metadata from page elements if available
        let roleVal = targetRole || (document.getElementById('targetRole')?.value) || 'Java Developer';
        let diffVal = (document.querySelector('input[name="difficulty"]:checked')?.value) || 'MEDIUM';
        let topicsVal = [];
        const topicInput = document.getElementById('selectedTopicsInput');
        if (topicInput && topicInput.value) {
            topicsVal = topicInput.value.split(',').map(t => t.trim()).filter(Boolean);
        }

        let resumeTextVal = document.getElementById('resumeText')?.value || '';
        let jdVal = document.getElementById('jobDescription')?.value || '';

        let missingKeywordsVal = [];
        let atsScoreVal = null;

        if (window.lastAtsResult && Array.isArray(window.lastAtsResult.missingSkills)) {
            missingKeywordsVal = window.lastAtsResult.missingSkills;
        } else {
            const missingChips = document.querySelectorAll('.ats-chip-missing');
            if (missingChips && missingChips.length > 0) {
                missingChips.forEach(chip => {
                    const text = chip.textContent.replace('✕', '').trim();
                    if (text && !text.includes('No critical missing skills')) {
                        missingKeywordsVal.push(text);
                    }
                });
            }
        }

        const scoreEl = document.getElementById('scoreDisplay');
        if (scoreEl && scoreEl.textContent) {
            const parsedScore = parseFloat(scoreEl.textContent.replace('%', ''));
            if (!isNaN(parsedScore)) atsScoreVal = parsedScore;
        }

        const payload = {
            contextType: contextType,
            contextId: contextId,
            message: messageText,
            targetRole: roleVal,
            difficulty: diffVal,
            topics: topicsVal,
            resumeText: resumeTextVal,
            jobDescription: jdVal,
            atsScore: atsScoreVal,
            missingKeywords: missingKeywordsVal,
            conversationHistory: conversationHistory.slice(-10) // Trimmed last 10 turns
        };

        try {
            const fetchFn = typeof window.apiCall === 'function' ? window.apiCall : apiCall;
            const res = await fetchFn('/api/chat', 'POST', payload);

            hideTypingIndicator();
            if (res && res.reply) {
                appendMessageBubble('assistant', res.reply);
                conversationHistory.push({ role: 'user', content: messageText });
                conversationHistory.push({ role: 'assistant', content: res.reply });
            } else {
                appendMessageBubble('assistant', 'Sorry, I could not generate a response at this moment.');
            }
        } catch (err) {
            hideTypingIndicator();
            console.error('Chat error:', err);
            appendMessageBubble('assistant', '⚠️ Error: ' + (err.message || 'Failed to connect to AI Chat service.'));
        } finally {
            if (sendBtn) sendBtn.disabled = false;
        }
    };
})();
