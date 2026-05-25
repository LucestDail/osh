function initializeNews() {
    const newsEventSource = new EventSource((window.CTX || '/') + 'api/news/stream');
    
    newsEventSource.onmessage = function(event) {
        const news = JSON.parse(event.data);
        updateNewsTable(news);
    };
    
    newsEventSource.onerror = function(error) {
        console.error('News SSE Error:', error);
        newsEventSource.close();
        setTimeout(initializeNews, 5000); // 5초 후 재연결 시도
    };
}

function updateNewsTable(news) {
    const tbody = document.querySelector('#newsTable tbody');
    tbody.innerHTML = '';
    
    news.forEach(item => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${item.newsCompany}</td>
            <td>${item.newsTitle}</td>
            <td>${formatDate(item.newsCreateDT)}</td>
        `;
        tbody.appendChild(row);
    });
}

function formatDate(dateString) {
    const date = new Date(dateString);
    return date.toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
} 