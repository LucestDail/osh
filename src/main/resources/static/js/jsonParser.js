/**
 * 지정된 json key의 문자열을 가져온다.
 * @param {} jsonName 
 * @param {*} strJson 
 * @returns 
 */
function getTargetJson(jsonName, strJson){
    var targetJsonObject = JSON.parse(strJson);
    return targetJsonObject[jsonName];
}
/**
 * JSON 파서(교통)
 * @param {*} strJson 
 * @returns 
 */
function trafficJsonParser(strJson){
    var trafficJson = JSON.parse(getTargetJson("trafficJson",strJson));
    var trafficArray = trafficJson['body']['items'];
    if(trafficArray === null){
        return;
    }
    trafficArray.sort((a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime());
    var trafficTbody = document.querySelector('#trafficTbody');
    var trElement = "";
    var trafficLength = trafficArray.length > 20 ? 20 : trafficArray.length;
    for(var i = 0; i < trafficLength; i++) {
        var targetJson = trafficArray[i];
        trElement += "<tr>";
        trElement   += "<td>" + strToDate(targetJson.startDate) + "</td>";
        trElement   += "<td>" + targetJson.roadName + "</td>";
        trElement   += "<td>" + targetJson.roadDrcType + "</td>";
        trElement   += "<td>" + targetJson.message + "</td>";
        trElement   += "<td>" + targetJson.eventType + "</td>";
        trElement   += "<td>" + targetJson.eventDetailType + "</td>"; 
        trElement+= "</tr>";
    }
    trafficTbody.innerHTML = trElement;
}

/**
 * 긴급재난문자 정보 파싱
 * @param {string} jsonString JSON 문자열
 */
function emergencyJsonParser(jsonString) {
    try {
        const emergencyTbody = document.getElementById('emergencyTbody');
        if (!emergencyTbody) {
            console.error('긴급재난문자 테이블 본문을 찾을 수 없습니다.');
            return;
        }

        const jsonData = JSON.parse(getTargetJson("emergencyJson",jsonString));
        if (!jsonData.emergencyJson) {
            // console.error('유효하지 않은 긴급재난문자 데이터 형식입니다.');
            return;
        }

        const emergencyData = JSON.parse(jsonData.emergencyJson);
        if (!emergencyData.data || !emergencyData.data.items) {
            console.error('긴급재난문자 데이터에 items가 없습니다.');
            return;
        }

        const emergencyItems = emergencyData.data.items.slice(0, 10); // 최대 10개 항목만 표시
        emergencyTbody.innerHTML = '';

        emergencyItems.forEach(item => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${formatDate(item.createDT)}</td>
                <td>${item.area || '-'}</td>
                <td>${item.content || '-'}</td>
                <td>${item.type || '-'}</td>
                <td>${item.detail || '-'}</td>
            `;
            emergencyTbody.appendChild(row);
        });
    } catch (error) {
        console.error('긴급재난문자 데이터 파싱 중 오류 발생:', error);
    }
}

/**
 * 날짜 포맷팅 함수
 * @param {string} dateStr 날짜 문자열 (YYYYMMDDHHmmss 형식)
 * @returns {string} 포맷팅된 날짜 문자열
 */
function formatDate(dateStr) {
    try {
        const date = new Date(dateStr);
        return date.toLocaleString('ko-KR', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch (error) {
        console.error('날짜 포맷팅 중 오류 발생:', error);
        return dateStr;
    }
}

/**
 * 뉴스 정보 파싱
 * @param {string} jsonString JSON 문자열
 */
function newsJsonParser(data) {
    try {
        const newsTbody = document.getElementById('newsTbody');
        if (!newsTbody) {
            console.error('뉴스 테이블 본문을 찾을 수 없습니다.');
            return;
        }

        const jsonData = JSON.parse(data);
        if (!jsonData.data || !jsonData.data.items) {
            // console.error('유효하지 않은 뉴스 데이터 형식입니다.');
            return;
        }

        const newsItems = jsonData.data.items.slice(0, 10); // 최대 10개 항목만 표시
        newsTbody.innerHTML = '';

        newsItems.forEach(item => {
            const row = document.createElement('tr');
            // 내용이 너무 길 경우 자동으로 줄임
            const truncatedContent = item.content && item.content.length > 100 
                ? item.content.substring(0, 100) + '...' 
                : item.content;
            
            row.innerHTML = `
                <td>${formatDate(item.createDT)}</td>
                <td>${item.company || '-'}</td>
                <td>${item.title || '-'}</td>
                <td>${truncatedContent || '-'}</td>
            `;
            newsTbody.appendChild(row);
        });
    } catch (error) {
        console.error('뉴스 데이터 파싱 중 오류 발생:', error);
    }
}

/**
 * JSON 파서(날씨)
 * @param {*} strJson 
 */
function weatherJsonParser(strJson) {
    const weatherJson = JSON.parse(getTargetJson("weatherJson",strJson));
    try {
        const weatherTbody = document.querySelector('#weatherTbody');
        if (!weatherTbody) {
            console.error('Weather table body not found');
            return;
        }

        let trElement = "";
        
         // 10개의 도시 정보를 순차적으로 처리
         for (let i = 1; i <= 20; i++) {
            const weatherJsonKey = `weatherJson${i}`;
            if (!weatherJson[weatherJsonKey]) {
                console.error(`Weather data not found for city ${i}`);
                continue;
            }

            // 중첩된 JSON 문자열을 파싱
            const weatherData = JSON.parse(weatherJson[weatherJsonKey]);
            if (!weatherData || !weatherData.weather || !weatherData.weather.length) {
                console.error(`Invalid weather data for city ${i}:`, weatherData);
                continue;
            }

            // 온도를 섭씨로 변환 (켈빈에서)
            const tempCelsius = (weatherData.main.temp - 273.15).toFixed(1);

            trElement += "<tr>";
            trElement += "<td>" + getCityName(i) + "</td>";
            trElement += "<td>" + weatherData.weather[0].main + "</td>";
            trElement += "<td>" + tempCelsius + "℃</td>";
            trElement += "<td>" + weatherData.clouds.all + "%</td>";
            trElement += "<td>" + weatherData.main.humidity + "%</td>";
            trElement += "<td>" + weatherData.wind.speed + "m/s</td>";
            trElement += "<td>" + convertDate(weatherData.sys.sunrise) + "</td>";
            trElement += "<td>" + convertDate(weatherData.sys.sunset) + "</td>";
            trElement += "</tr>";
        }
        weatherTbody.innerHTML = trElement;
    } catch (error) {
        console.error('Error parsing weather data:', error);
    }
}

/**
 * 도시 번호에 따른 도시 이름 반환
 * @param {number} cityNumber 
 * @returns {string} 도시 이름
 */
function getCityName(cityNumber) {
    const cities = {
        1: "창원",
        2: "서울",
        3: "부산",
        4: "인천",
        5: "대구",
        6: "대전",
        7: "광주",
        8: "수원",
        9: "울산",
        10: "고양",
        11: "용인",
        12: "포항",
        13: "창원",
        14: "김해",
        15: "김천",
        16: "제주",
        17: "춘천",
        18: "원주",
        19: "강릉",
        20: "속초"
    };
    return cities[cityNumber] || "알 수 없음";
}

/**
 * Unix timestamp를 시간 형식으로 변환
 * @param {number} timestamp 
 * @returns {string} HH:mm 형식의 시간
 */
function convertDate(timestamp) {
    try {
        const date = new Date(timestamp * 1000);
        return date.getHours().toString().padStart(2, '0') + ':' + 
               date.getMinutes().toString().padStart(2, '0');
    } catch (error) {
        console.error('시간 변환 중 오류 발생:', error);
        return '00:00';
    }
}

let memoryChart = null;

/**
 * JSON 파서(서버정보)
 * @param {*} strJson 
 */
function applicationJsonParser(strJson){
    var applicationJson = JSON.parse(getTargetJson("applicationJson",strJson));
    document.querySelector('#currentTime').innerHTML = applicationJson.currentTime;
    
    // 서버 정보 업데이트
    document.querySelector('#systemArchitecture').textContent = applicationJson.systemArchitecture;
    document.querySelector('#systemName').textContent = applicationJson.systemName;
    document.querySelector('#systemLoad').textContent = applicationJson.systemLoadAverage + '%';
    document.querySelector('#totalMemory').textContent = applicationJson.memory;
    
    // 메모리 사용량 차트 업데이트
    updateMemoryChart(applicationJson);
}

/**
 * 메모리 사용량 차트 업데이트
 * @param {Object} data 서버 정보 데이터
 */
function updateMemoryChart(data) {
    const ctx = document.getElementById('memoryChart').getContext('2d');
    
    // 메모리 사용량 계산
    const usedMemory = parseFloat(data.useMemory);
    const freeMemory = parseFloat(data.freeMemory);
    const totalMemory = usedMemory + freeMemory;
    const usedPercentage = (usedMemory / totalMemory) * 100;
    
    // 차트 데이터
    const chartData = {
        labels: ['사용 중', '여유'],
        datasets: [{
            data: [usedMemory, freeMemory],
            backgroundColor: [
                '#28a745', // 초록색 (사용 중)
                '#6c757d'  // 회색 (여유)
            ],
            borderWidth: 0
        }]
    };
    
    // 차트 옵션
    const options = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                position: 'bottom',
                labels: {
                    padding: 20,
                    font: {
                        size: 12
                    }
                }
            },
            tooltip: {
                callbacks: {
                    label: function(context) {
                        const value = context.raw;
                        const percentage = (value / (usedMemory + freeMemory) * 100).toFixed(1);
                        return `${context.label}: ${value}GB (${percentage}%)`;
                    }
                }
            }
        },
        cutout: '70%',
        animation: {
            duration: 1000 // 애니메이션 지속 시간 (밀리초)
        }
    };
    
    // 기존 차트가 있다면 데이터만 업데이트
    if (memoryChart) {
        memoryChart.data = chartData;
        memoryChart.update('none'); // 애니메이션 없이 업데이트
    } else {
        // 첫 생성시에만 새 차트 생성
        memoryChart = new Chart(ctx, {
            type: 'doughnut',
            data: chartData,
            options: options
        });
    }
}

/**
 * 연합뉴스 정보 파싱
 * @param {string} jsonString JSON 문자열
 */
function yeonhapJsonParser(jsonString) {
    const newsData = JSON.parse(getTargetJson("yeonhapJson",jsonString));
    try {
        const newsTbody = document.getElementById('newsTbody');
        if (!newsTbody) {
            console.error('뉴스 테이블 본문을 찾을 수 없습니다.');
            return;
        }

        if (!newsData.data || !newsData.data.items) {
            // console.error('유효하지 않은 뉴스 데이터 형식입니다.');
            return;
        }

        const newsItems = newsData.data.items.slice(0, 10); // 최대 10개 항목만 표시
        newsTbody.innerHTML = '';

        newsItems.forEach(item => {
            const row = document.createElement('tr');
            // 내용이 너무 길 경우 자동으로 줄임
            const truncatedContent = item.content && item.content.length > 100 
                ? item.content.substring(0, 100) + '...' 
                : item.content;
            
            row.innerHTML = `
                <td>${formatDate(item.createDT)}</td>
                <td>${item.company || '-'}</td>
                <td>${item.title || '-'}</td>
                <td>${truncatedContent || '-'}</td>
            `;
            newsTbody.appendChild(row);
        });
    } catch (error) {
        console.error('뉴스 데이터 파싱 중 오류 발생:', error);
    }
}
