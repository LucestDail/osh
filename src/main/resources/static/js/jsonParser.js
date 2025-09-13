/**
 * 지정된 json key의 문자열을 가져온다.
 * @param {string} jsonName 
 * @param {string} strJson 
 * @returns {string|null}
 */
function getTargetJson(jsonName, strJson){
    try {
        if (!strJson || strJson.trim() === '') {
            console.warn('getTargetJson: JSON 문자열이 비어있습니다.');
            return null;
        }
        
        if (!jsonName) {
            console.warn('getTargetJson: JSON 키가 지정되지 않았습니다.');
            return null;
        }

        var targetJsonObject = JSON.parse(strJson);
        if (!targetJsonObject || typeof targetJsonObject !== 'object') {
            console.warn('getTargetJson: 유효하지 않은 JSON 객체입니다.');
            return null;
        }

        const result = targetJsonObject[jsonName];
        if (result === undefined) {
            console.warn(`getTargetJson: 키 '${jsonName}'를 찾을 수 없습니다.`);
            return null;
        }

        return result;
    } catch (error) {
        console.error('getTargetJson: JSON 파싱 중 오류 발생:', error);
        return null;
    }
}
/**
 * JSON 파서(교통)
 * @param {*} strJson 
 * @returns 
 */
function trafficJsonParser(strJson){
    try {
        if (!strJson || strJson.trim() === '') {
            console.warn('교통정보 JSON이 비어있습니다.');
            return;
        }

        var trafficJson = JSON.parse(getTargetJson("trafficJson",strJson));
        if (!trafficJson || !trafficJson.body || !trafficJson.body.items) {
            console.warn('교통정보 데이터 구조가 올바르지 않습니다.');
            return;
        }

        var trafficArray = trafficJson['body']['items'];
        if(trafficArray === null || trafficArray.length === 0){
            console.warn('교통정보 배열이 비어있습니다.');
            return;
        }

        trafficArray.sort((a, b) => new Date(b.startDate).getTime() - new Date(a.startDate).getTime());
        var trafficTbody = document.querySelector('#trafficTbody');
        if (!trafficTbody) {
            console.error('교통정보 테이블 본문을 찾을 수 없습니다.');
            return;
        }

        var trElement = "";
        var trafficLength = trafficArray.length > 20 ? 20 : trafficArray.length;
        for(var i = 0; i < trafficLength; i++) {
            var targetJson = trafficArray[i];
            if (!targetJson) continue;
            
            trElement += "<tr>";
            trElement   += "<td>" + (targetJson.startDate ? strToDate(targetJson.startDate) : '-') + "</td>";
            trElement   += "<td>" + (targetJson.roadName || '-') + "</td>";
            trElement   += "<td>" + (targetJson.roadDrcType || '-') + "</td>";
            trElement   += "<td>" + (targetJson.message || '-') + "</td>";
            trElement   += "<td>" + (targetJson.eventType || '-') + "</td>";
            trElement   += "<td>" + (targetJson.eventDetailType || '-') + "</td>"; 
            trElement+= "</tr>";
        }
        trafficTbody.innerHTML = trElement;
    } catch (error) {
        console.error('교통정보 파싱 중 오류 발생:', error);
        // 에러 발생 시에도 테이블에 메시지 표시
        const trafficTbody = document.querySelector('#trafficTbody');
        if (trafficTbody) {
            trafficTbody.innerHTML = `
                <tr>
                    <td colspan="6" class="text-center text-danger">
                        <i class="mdi mdi-alert-circle-outline"></i>
                        교통정보 로드 중 오류가 발생했습니다
                        <br><small>잠시 후 다시 시도해주세요.</small>
                    </td>
                </tr>
            `;
        }
    }
}

/**
 * 긴급재난문자 정보 파싱
 * @param {string} jsonString JSON 문자열
 */
function emergencyJsonParser(jsonString) {
    try {
        if (!jsonString || jsonString.trim() === '') {
            console.warn('긴급재난문자 JSON이 비어있습니다.');
            return;
        }

        const emergencyTbody = document.getElementById('emergencyTbody');
        if (!emergencyTbody) {
            console.error('긴급재난문자 테이블 본문을 찾을 수 없습니다.');
            return;
        }

        const jsonData = JSON.parse(getTargetJson("emergencyJson",jsonString));
        if (!jsonData || !jsonData.items) {
            console.warn('긴급재난문자 데이터 구조가 올바르지 않습니다.');
            emergencyTbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted">
                        <i class="mdi mdi-information-outline"></i>
                        긴급재난문자 데이터를 불러오는 중입니다...
                    </td>
                </tr>
            `;
            return;
        }

        const emergencyItems = jsonData.items.slice(0, 10); // 최대 10개 항목만 표시
        emergencyTbody.innerHTML = '';

        if (emergencyItems.length === 0) {
            emergencyTbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-muted">
                        <i class="mdi mdi-information-outline"></i>
                        현재 긴급재난문자가 없습니다
                    </td>
                </tr>
            `;
            return;
        }

        emergencyItems.forEach(item => {
            if (!item) return;
            
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${formatDate(item.CRT_DT)}</td>
                <td>${item.RCPTN_RGN_NM || '-'}</td>
                <td>${item.MSG_CN || '-'}</td>
                <td>${item.EMRG_STEP_NM || '-'}</td>
                <td>${item.DST_SE_NM || '-'}</td>
            `;
            emergencyTbody.appendChild(row);
        });
    } catch (error) {
        console.error('긴급재난문자 데이터 파싱 중 오류 발생:', error);
        // 에러 발생 시에도 테이블에 메시지 표시
        const emergencyTbody = document.getElementById('emergencyTbody');
        if (emergencyTbody) {
            emergencyTbody.innerHTML = `
                <tr>
                    <td colspan="5" class="text-center text-danger">
                        <i class="mdi mdi-alert-circle-outline"></i>
                        긴급재난문자 로드 중 오류가 발생했습니다
                        <br><small>잠시 후 다시 시도해주세요.</small>
                    </td>
                </tr>
            `;
        }
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
            // 데이터가 없을 때 메시지 표시
            newsTbody.innerHTML = `
                <tr>
                    <td colspan="3" class="text-center text-muted">
                        <i class="mdi mdi-information-outline"></i>
                        뉴스 데이터를 불러오는 중입니다...
                    </td>
                </tr>
            `;
            return;
        }

        const newsItems = jsonData.data.items.slice(0, 20); // 최대 20개 항목만 표시
        newsTbody.innerHTML = '';

        // 데이터가 비어있거나 에러 상태인지 확인
        if (newsItems.length === 0 || (newsItems.length === 1 && 
            (newsItems[0].title === "데이터를 불러오는 중입니다..." || 
             newsItems[0].title === "데이터 로드 중 오류가 발생했습니다" ||
             newsItems[0].title === "서비스 초기화 중입니다..."))) {
            
            const message = newsItems.length > 0 ? newsItems[0].title : "뉴스 데이터를 불러오는 중입니다...";
            const subMessage = newsItems.length > 0 ? newsItems[0].content : "잠시 후 다시 시도해주세요.";
            
            newsTbody.innerHTML = `
                <tr>
                    <td colspan="3" class="text-center text-muted">
                        <i class="mdi mdi-information-outline"></i>
                        <div>${message}</div>
                        <small>${subMessage}</small>
                    </td>
                </tr>
            `;
            return;
        }

        newsItems.forEach(item => {
            const row = document.createElement('tr');
            // 내용이 너무 길 경우 자동으로 줄임
            const truncatedContent = item.content && item.content.length > 200 
                ? item.content.substring(0, 200) + '...' 
                : item.content;
            
            row.innerHTML = `
                <td>${formatDate(item.createDT)}</td>
                <td>${item.title || '-'}</td>
                <td>${truncatedContent || '-'}</td>
            `;
            newsTbody.appendChild(row);
        });
    } catch (error) {
        console.error('뉴스 데이터 파싱 중 오류 발생:', error);
        // 에러 발생 시에도 사용자에게 메시지 표시
        const newsTbody = document.getElementById('newsTbody');
        if (newsTbody) {
            newsTbody.innerHTML = `
                <tr>
                    <td colspan="3" class="text-center text-danger">
                        <i class="mdi mdi-alert-circle-outline"></i>
                        뉴스 데이터 로드 중 오류가 발생했습니다
                        <br><small>잠시 후 다시 시도해주세요.</small>
                    </td>
                </tr>
            `;
        }
    }
}

/**
 * JSON 파서(날씨)
 * @param {*} strJson 
 */
function weatherJsonParser(strJson) {
    try {
        if (!strJson || strJson.trim() === '') {
            console.warn('날씨정보 JSON이 비어있습니다.');
            return;
        }

        const weatherJson = JSON.parse(getTargetJson("weatherJson",strJson));
        if (!weatherJson) {
            console.warn('날씨정보 데이터 구조가 올바르지 않습니다.');
            return;
        }

        const weatherTbody = document.querySelector('#weatherTbody');
        if (!weatherTbody) {
            console.error('Weather table body not found');
            return;
        }

        let trElement = "";
        let validDataCount = 0;
        
        // 20개의 도시 정보를 순차적으로 처리
        for (let i = 1; i <= 20; i++) {
            try {
                const weatherJsonKey = `weatherJson${i}`;
                if (!weatherJson[weatherJsonKey]) {
                    console.warn(`Weather data not found for city ${i}`);
                    continue;
                }

                // 중첩된 JSON 문자열을 파싱
                const weatherData = JSON.parse(weatherJson[weatherJsonKey]);
                if (!weatherData || !weatherData.weather || !weatherData.weather.length || !weatherData.main) {
                    console.warn(`Invalid weather data for city ${i}:`, weatherData);
                    continue;
                }

                // 온도를 섭씨로 변환 (켈빈에서)
                const tempCelsius = weatherData.main.temp ? (weatherData.main.temp - 273.15).toFixed(1) : '-';

                trElement += "<tr>";
                trElement += "<td>" + getCityName(i) + "</td>";
                trElement += "<td>" + (weatherData.weather[0]?.main || '-') + "</td>";
                trElement += "<td>" + tempCelsius + "℃</td>";
                trElement += "<td>" + (weatherData.clouds?.all || '-') + "%</td>";
                trElement += "<td>" + (weatherData.main?.humidity || '-') + "%</td>";
                trElement += "<td>" + (weatherData.wind?.speed || '-') + "m/s</td>";
                trElement += "<td>" + (weatherData.sys?.sunrise ? convertDate(weatherData.sys.sunrise) : '-') + "</td>";
                trElement += "<td>" + (weatherData.sys?.sunset ? convertDate(weatherData.sys.sunset) : '-') + "</td>";
                trElement += "</tr>";
                
                validDataCount++;
            } catch (cityError) {
                console.warn(`도시 ${i} 날씨 데이터 파싱 실패:`, cityError);
                continue;
            }
        }
        
        if (validDataCount === 0) {
            weatherTbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center text-muted">
                        <i class="mdi mdi-information-outline"></i>
                        날씨 정보를 불러오는 중입니다...
                    </td>
                </tr>
            `;
        } else {
            weatherTbody.innerHTML = trElement;
        }
    } catch (error) {
        console.error('Error parsing weather data:', error);
        // 에러 발생 시에도 테이블에 메시지 표시
        const weatherTbody = document.querySelector('#weatherTbody');
        if (weatherTbody) {
            weatherTbody.innerHTML = `
                <tr>
                    <td colspan="8" class="text-center text-danger">
                        <i class="mdi mdi-alert-circle-outline"></i>
                        날씨 정보 로드 중 오류가 발생했습니다
                        <br><small>잠시 후 다시 시도해주세요.</small>
                    </td>
                </tr>
            `;
        }
    }
}

/**
 * 도시 번호에 따른 도시 이름 반환
 * @param {number} cityNumber 
 * @returns {string} 도시 이름
 */
function getCityName(cityNumber) {
    const cities = {
        1: "서울",
        2: "부산",
        3: "인천",
        4: "대구",
        5: "대전",
        6: "광주",
        7: "수원",
        8: "울산",
        9: "고양",
        10: "용인",
        11: "포항",
        12: "창원",
        13: "김해",
        14: "김천",
        15: "제주",
        16: "춘천",
        17: "원주",
        18: "강릉",
        19: "속초"
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
    try {
        if (!strJson || strJson.trim() === '') {
            console.warn('서버정보 JSON이 비어있습니다.');
            return;
        }

        var applicationJson = JSON.parse(getTargetJson("applicationJson",strJson));
        if (!applicationJson) {
            console.warn('서버정보 데이터 구조가 올바르지 않습니다.');
            return;
        }

        // 각 요소를 안전하게 업데이트
        const currentTimeElement = document.querySelector('#currentTime');
        if (currentTimeElement && applicationJson.currentTime) {
            currentTimeElement.innerHTML = applicationJson.currentTime;
        }
        
        // 서버 정보 업데이트
        const systemArchitectureElement = document.querySelector('#systemArchitecture');
        if (systemArchitectureElement && applicationJson.systemArchitecture) {
            systemArchitectureElement.textContent = applicationJson.systemArchitecture;
        }
        
        const systemNameElement = document.querySelector('#systemName');
        if (systemNameElement && applicationJson.systemName) {
            systemNameElement.textContent = applicationJson.systemName;
        }
        
        const systemLoadElement = document.querySelector('#systemLoad');
        if (systemLoadElement && applicationJson.systemLoadAverage !== undefined) {
            systemLoadElement.textContent = applicationJson.systemLoadAverage + '%';
        }
        
        const totalMemoryElement = document.querySelector('#totalMemory');
        if (totalMemoryElement && applicationJson.memory) {
            totalMemoryElement.textContent = applicationJson.memory;
        }
        
        const usedMemoryElement = document.querySelector('#usedMemory');
        if (usedMemoryElement && applicationJson.useMemory) {
            usedMemoryElement.textContent = applicationJson.useMemory;
        }
        
        const freeMemoryElement = document.querySelector('#freeMemory');
        if (freeMemoryElement && applicationJson.freeMemory) {
            freeMemoryElement.textContent = applicationJson.freeMemory;
        }
        
        // 메모리 사용량 차트 업데이트
        updateMemoryChart(applicationJson);
    } catch (error) {
        console.error('서버정보 파싱 중 오류 발생:', error);
    }
}

/**
 * 메모리 사용량 차트 업데이트
 * @param {Object} data 서버 정보 데이터
 */
function updateMemoryChart(data) {
    try {
        if (!data) {
            console.warn('메모리 차트 업데이트를 위한 데이터가 없습니다.');
            return;
        }

        const chartElement = document.getElementById('memoryChart');
        if (!chartElement) {
            console.warn('메모리 차트 요소를 찾을 수 없습니다.');
            return;
        }

        const ctx = chartElement.getContext('2d');
        if (!ctx) {
            console.warn('메모리 차트 컨텍스트를 가져올 수 없습니다.');
            return;
        }
        
        // 메모리 사용량 계산 (MB 단위) - 안전한 파싱
        let usedMemory = 0;
        let freeMemory = 0;
        
        try {
            if (data.useMemory) {
                usedMemory = parseFloat(data.useMemory) || 0;
            }
            if (data.freeMemory) {
                freeMemory = parseFloat(data.freeMemory) || 0;
            }
        } catch (parseError) {
            console.warn('메모리 값 파싱 중 오류:', parseError);
        }
        
        const totalMemory = usedMemory + freeMemory;
        
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
                            const percentage = totalMemory > 0 ? (value / totalMemory * 100).toFixed(1) : '0.0';
                            return `${context.label}: ${value}MB (${percentage}%)`;
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
        if (memoryChart && typeof memoryChart.update === 'function') {
            try {
                memoryChart.data = chartData;
                memoryChart.update('none'); // 애니메이션 없이 업데이트
            } catch (chartError) {
                console.warn('기존 차트 업데이트 중 오류:', chartError);
                // 차트 재생성
                memoryChart = new Chart(ctx, {
                    type: 'doughnut',
                    data: chartData,
                    options: options
                });
            }
        } else {
            // 첫 생성시에만 새 차트 생성
            try {
                memoryChart = new Chart(ctx, {
                    type: 'doughnut',
                    data: chartData,
                    options: options
                });
            } catch (createError) {
                console.error('차트 생성 중 오류:', createError);
            }
        }
        
        // 하단 서버 정보도 동기화하여 업데이트
        updateServerInfoDisplay(data);
    } catch (error) {
        console.error('메모리 차트 업데이트 중 오류 발생:', error);
    }
}

/**
 * 서버 정보 표시 업데이트
 * @param {Object} data 서버 정보 데이터
 */
function updateServerInfoDisplay(data) {
    // 전체 메모리 정보 업데이트
    const totalMemoryElement = document.querySelector('#totalMemory');
    if (totalMemoryElement) {
        totalMemoryElement.textContent = data.memory;
    }
    
    // 사용 중인 메모리 정보를 별도로 표시할 요소가 있다면 업데이트
    const usedMemoryElement = document.querySelector('#usedMemory');
    if (usedMemoryElement) {
        usedMemoryElement.textContent = data.useMemory;
    }
    
    // 여유 메모리 정보를 별도로 표시할 요소가 있다면 업데이트
    const freeMemoryElement = document.querySelector('#freeMemory');
    if (freeMemoryElement) {
        freeMemoryElement.textContent = data.freeMemory;
    }
}

/**
 * 연합뉴스 정보 파싱
 * @param {string} jsonString JSON 문자열
 */
function yeonhapJsonParser(jsonString) {
    try {
        if (!jsonString || jsonString.trim() === '') {
            console.warn('연합뉴스 JSON이 비어있습니다.');
            return;
        }

        const newsData = JSON.parse(getTargetJson("yeonhapJson",jsonString));
        if (!newsData) {
            console.warn('연합뉴스 데이터 구조가 올바르지 않습니다.');
            return;
        }

        const newsTbody = document.getElementById('newsTbody');
        if (!newsTbody) {
            console.error('뉴스 테이블 본문을 찾을 수 없습니다.');
            return;
        }

        if (!newsData.data || !newsData.data.items) {
            console.warn('유효하지 않은 뉴스 데이터 형식입니다.');
            newsTbody.innerHTML = `
                <tr>
                    <td colspan="3" class="text-center text-muted">
                        <i class="mdi mdi-information-outline"></i>
                        뉴스 데이터를 불러오는 중입니다...
                    </td>
                </tr>
            `;
            return;
        }

        const newsItems = newsData.data.items.slice(0, 10); // 최대 10개 항목만 표시
        
        if (newsItems.length === 0) {
            newsTbody.innerHTML = `
                <tr>
                    <td colspan="3" class="text-center text-muted">
                        <i class="mdi mdi-information-outline"></i>
                        현재 뉴스가 없습니다
                    </td>
                </tr>
            `;
            return;
        }

        newsTbody.innerHTML = '';

        newsItems.forEach(item => {
            if (!item) return;
            
            const row = document.createElement('tr');
            // 내용이 너무 길 경우 자동으로 줄임
            const truncatedContent = item.content && item.content.length > 300 
                ? item.content.substring(0, 300) + '...' 
                : item.content;
            
            row.innerHTML = `
                <td>${formatDate(item.createDT)}</td>
                <td>${item.title || '-'}</td>
                <td>${truncatedContent || '-'}</td>
            `;
            newsTbody.appendChild(row);
        });
    } catch (error) {
        console.error('연합뉴스 데이터 파싱 중 오류 발생:', error);
        // 에러 발생 시에도 테이블에 메시지 표시
        const newsTbody = document.getElementById('newsTbody');
        if (newsTbody) {
            newsTbody.innerHTML = `
                <tr>
                    <td colspan="3" class="text-center text-danger">
                        <i class="mdi mdi-alert-circle-outline"></i>
                        뉴스 데이터 로드 중 오류가 발생했습니다
                        <br><small>잠시 후 다시 시도해주세요.</small>
                    </td>
                </tr>
            `;
        }
    }
}
