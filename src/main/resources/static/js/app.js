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
    document.getElementById('preferredCategories').value = p.preferredCategories || '';
    document.getElementById('avoidKeywords').value = p.avoidKeywords || '';
    document.getElementById('payFloor').value = p.payFloor || '';
    document.getElementById('payTarget').value = p.payTarget || '';
}

// 프로필 저장
async function saveProfile() {
    const body = {
        jobTitle: document.getElementById('jobTitle').value,
        resumeContent: document.getElementById('resumeContent').value,
        preferredCategories: document.getElementById('preferredCategories').value,
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
            <td>${p.company || '-'}</td>
            <td>${p.title || '-'}</td>
            <td>${p.fetchedAt ? p.fetchedAt.substring(0, 16).replace('T', ' ') : '-'}</td>
            <td><a href="${p.url}" target="_blank">보기</a></td>
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
                    <span class="result-title">${r.company || '-'}</span>
                    <span class="result-job"> · ${r.jobTitle || '-'}</span>
                </div>
                <span class="score">${r.score != null ? r.score + '점' : '-'}</span>
            </div>
            ${r.analysisReason ? `<div class="reason-badge">🔄 ${r.analysisReason}</div>` : ''}

            <!-- 항목별 점수 -->
            <div class="score-breakdown">
                <span>⚙️ 기술 ${r.techScore != null ? r.techScore + '점' : '-'}</span>
                <span>📅 경력 ${r.experienceScore != null ? r.experienceScore + '점' : '-'}</span>
                <span>❤️ 선호 ${r.preferenceScore != null ? r.preferenceScore + '점' : '-'}</span>
            </div>

            <div class="result-keywords">🏷️ ${r.matchedKeywords || '-'}</div>
            <div class="result-analysis">📊 ${r.requirementAnalysis || '-'}</div>

            <!-- 우려사항 -->
            ${r.riskFactors ? `<div class="result-risk">⚠️ ${r.riskFactors}</div>` : ''}

            <!-- 자소서 키워드 -->
            ${r.coverLetterKeywords ? `<div class="result-cover">✏️ 자소서 키워드: ${r.coverLetterKeywords}</div>` : ''}

            <div class="result-summary">📋 ${r.summary || '-'}</div>
            <div class="feedback-buttons">
                <button onclick="saveFeedback(${r.id}, 'INTERESTED')" class="btn-interested">👍 관심있음</button>
                <button onclick="saveFeedback(${r.id}, 'NOT_INTERESTED')" class="btn-not-interested">👎 관심없음</button>
                <button onclick="saveFeedback(${r.id}, 'APPLIED')" class="btn-applied">✉️ 지원함</button>
            </div>
            <a href="${r.jobUrl}" target="_blank" class="job-link">공고 보기 →</a>
        </div>
    `).join('');
}

// 상태 메시지
function setStatus(msg) {
    document.getElementById('action-status').textContent = msg;
}

async function saveFeedback(matchResultId, feedbackType) {
    const res = await fetch(`/api/feedback/${matchResultId}?feedbackType=${feedbackType}`, { method: 'POST' });
    if (res.ok) {
        alert(`피드백 저장 완료: ${feedbackType}`);
    }
}

// 초기 로드
loadProfile();