// HTML 이스케이프 — 크롤한 회사명·LLM 출력 등 외부 문자열을 innerHTML에 넣기 전 처리(XSS 방지)
function esc(v) {
    if (v == null) return '';
    return String(v)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

// 탭 전환
function showTab(name) {
    document.querySelectorAll('.tab-content').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(el => el.classList.remove('active'));
    document.getElementById('tab-' + name).classList.add('active');
    event.target.classList.add('active');

    if (name === 'postings') loadPostings();
    if (name === 'results') loadResults();
}

// 프로필 로드
async function loadProfile() {
    const res = await fetch('/api/profile');
    const p = await res.json();
    document.getElementById('jobTitle').value = p.jobTitle || '';
    document.getElementById('resumeContent').value = p.resumeContent || '';
    document.getElementById('searchKeywords').value = p.searchKeywords || '';
    document.getElementById('avoidKeywords').value = p.avoidKeywords || '';
    document.getElementById('payFloor').value = p.payFloor || '';
    document.getElementById('payTarget').value = p.payTarget || '';
}

// 프로필 저장
async function saveProfile() {
    const body = {
        jobTitle: document.getElementById('jobTitle').value,
        resumeContent: document.getElementById('resumeContent').value,
        searchKeywords: document.getElementById('searchKeywords').value,
        avoidKeywords: document.getElementById('avoidKeywords').value,
        payFloor: parseInt(document.getElementById('payFloor').value) || 0,
        payTarget: parseInt(document.getElementById('payTarget').value) || 0
    };
    await fetch('/api/profile', { method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(body) });
    setStatus('프로필 저장 완료 ✅');
}

// 공고 수집
async function runCrawl() {
    setStatus('공고 수집 중...');
    const res = await fetch('/api/crawl', { method: 'POST' });
    const msg = await res.text();
    setStatus(msg + ' ✅');
}

// AI 분석
async function runAnalyze() {
    setStatus('AI 분석 중... (시간이 걸릴 수 있어요)');
    const res = await fetch('/api/analyze', { method: 'POST' });
    const msg = await res.text();
    setStatus(msg + ' ✅');
}

// 공고 목록
async function loadPostings() {
    const res = await fetch('/api/postings');
    const postings = await res.json();
    document.getElementById('posting-count').textContent = postings.length;
    const tbody = document.getElementById('postings-body');
    tbody.innerHTML = postings.map(p => `
        <tr>
            <td>${esc(p.company) || '-'}</td>
            <td>${esc(p.title) || '-'}</td>
            <td>${p.fetchedAt ? esc(p.fetchedAt.substring(0, 16).replace('T', ' ')) : '-'}</td>
            <td><a href="${encodeURI(p.url || '')}" target="_blank">보기</a></td>
        </tr>
    `).join('');
}

// 분석 결과
async function loadResults() {
    const res = await fetch('/api/results');
    const results = await res.json();
    document.getElementById('result-count').textContent = results.length;
    document.getElementById('results-list').innerHTML = results.map(r => `
        <div class="result-card">
            <div class="result-header">
                <div>
                    <span class="result-title">${esc(r.company) || '-'}</span>
                    <span class="result-job"> · ${esc(r.jobTitle) || '-'}</span>
                </div>
                <span class="score">${r.score != null ? r.score + '점' : '-'}</span>
            </div>
            ${r.analysisReason ? `<div class="reason-badge">🔄 ${esc(r.analysisReason)}</div>` : ''}

            <!-- 항목별 점수 -->
            <div class="score-breakdown">
                <span>⚙️ 기술 ${r.techScore != null ? r.techScore + '점' : '-'}</span>
                <span>📅 경력 ${r.experienceScore != null ? r.experienceScore + '점' : '-'}</span>
                <span>❤️ 선호 ${r.preferenceScore != null ? r.preferenceScore + '점' : '-'}</span>
            </div>

            <div class="result-keywords">🏷️ ${esc(r.matchedKeywords) || '-'}</div>
            <div class="result-analysis">📊 ${esc(r.requirementAnalysis) || '-'}</div>

            <!-- 우려사항 -->
            ${r.riskFactors ? `<div class="result-risk">⚠️ ${esc(r.riskFactors)}</div>` : ''}

            <!-- 자소서 키워드 -->
            ${r.coverLetterKeywords ? `<div class="result-cover">✏️ 자소서 키워드: ${esc(r.coverLetterKeywords)}</div>` : ''}

            <div class="result-summary">📋 ${esc(r.summary) || '-'}</div>
            <div class="feedback-buttons" id="feedback-${r.id}">
                ${feedbackButtons(r.id, r.feedbackType)}
            </div>
            <a href="${encodeURI(r.jobUrl || '')}" target="_blank" class="job-link">공고 보기 →</a>
        </div>
    `).join('');
}

// 피드백 버튼 HTML. 현재 선택된 종류에는 selected 클래스를 붙여 표시한다.
function feedbackButtons(resultId, selected) {
    const types = [
        { type: 'INTERESTED', cls: 'btn-interested', label: '👍 관심있음' },
        { type: 'NOT_INTERESTED', cls: 'btn-not-interested', label: '👎 관심없음' },
        { type: 'APPLIED', cls: 'btn-applied', label: '✉️ 지원함' }
    ];
    return types.map(t => {
        const isOn = selected === t.type;
        return `<button onclick="saveFeedback(${resultId}, '${t.type}')"
                    class="${t.cls}${isOn ? ' selected' : ''}">
                    ${isOn ? '✓ ' : ''}${t.label}
                </button>`;
    }).join('');
}

// 상태 메시지
function setStatus(msg) {
    document.getElementById('action-status').textContent = msg;
}

async function saveFeedback(matchResultId, feedbackType) {
    const res = await fetch(`/api/feedback/${matchResultId}?feedbackType=${feedbackType}`, { method: 'POST' });
    if (res.ok) {
        // 저장 후 해당 카드의 버튼만 다시 그려 선택 상태를 즉시 표시한다 (새로고침해도 유지됨)
        const box = document.getElementById('feedback-' + matchResultId);
        if (box) box.innerHTML = feedbackButtons(matchResultId, feedbackType);
        setStatus('피드백 저장 완료 ✅');
    } else {
        setStatus('피드백 저장 실패 ❌');
    }
}

// 초기 로드
loadProfile();