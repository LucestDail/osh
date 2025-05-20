// 대시보드 요약 데이터를 가져오는 함수
function fetchDashboardSummary() {
    showLoading();
    fetch('/api/gemini/dashboard-summary')
        .then(response => {
            if (!response.ok) {
                throw new Error('대시보드 요약을 불러오는데 실패했습니다.');
            }
            return response.json();
        })
        .then(summary => {
            updateDashboardUI(summary);
        })
        .catch(error => {
            showError(error.message);
            console.error('Error:', error);
        });
}

// 대시보드 UI 업데이트 함수
function updateDashboardUI(summary) {
    document.getElementById('news-summary').innerHTML = formatText(summary.newsSummary);
    document.getElementById('market-trends').innerHTML = formatText(summary.marketTrends);
    document.getElementById('key-highlights').innerHTML = formatText(summary.keyHighlights);
    document.getElementById('recommendations').innerHTML = formatText(summary.recommendations);
    document.getElementById('comprehensive-analysis').innerHTML = formatText(summary.comprehensiveAnalysis);
    
    const now = new Date();
    document.getElementById('last-update').textContent = `마지막 업데이트: ${now.toLocaleString()}`;
}

// 텍스트 포맷팅 함수 (줄바꿈을 <br> 태그로 변환)
function formatText(text) {
    if (!text) return '<div class="text-muted">데이터를 불러오는 중입니다...</div>';
    return text.split('\n').map(line => `<p>${line}</p>`).join('');
}

// 에러 메시지 표시 함수
function showError(message) {
    const errorDiv = document.getElementById('error-message');
    errorDiv.textContent = message;
    errorDiv.style.display = 'block';
    setTimeout(() => {
        errorDiv.style.display = 'none';
    }, 5000);
}

// 로딩 상태 표시 함수
function showLoading() {
    const sections = ['news-summary', 'market-trends', 'key-highlights', 'recommendations', 'comprehensive-analysis'];
    sections.forEach(section => {
        document.getElementById(section).innerHTML = '<div class="text-center"><div class="spinner-border text-primary" role="status"><span class="visually-hidden">Loading...</span></div></div>';
    });
}

// 페이지 로드 시 대시보드 요약 데이터 가져오기
document.addEventListener('DOMContentLoaded', function() {
    fetchDashboardSummary();
    // 5분마다 데이터 갱신
    setInterval(fetchDashboardSummary, 300000);
}); 