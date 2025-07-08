
/**
 * 화씨 온도를 섭씨로 변경
 * @param {number} fahrenheit 
 * @returns {number}
 */
function convertF(fahrenheit){
    try {
        if (fahrenheit === null || fahrenheit === undefined || isNaN(fahrenheit)) {
            console.warn('convertF: 유효하지 않은 온도 값입니다.');
            return 0;
        }
        
        return Math.ceil(fahrenheit - 273.15);
    } catch (error) {
        console.error('convertF: 온도 변환 중 오류 발생:', error);
        return 0;
    }
}

/**
 * posix 날짜값을 일반 날짜 형식으로 변경한다
 * @param {number} dateValue 
 * @returns {string}
 */
function convertDate(dateValue){
    try {
        if (!dateValue || isNaN(dateValue)) {
            console.warn('convertDate: 유효하지 않은 날짜 값입니다.');
            return '-';
        }
        
        var myDate = new Date(0); 
        myDate.setUTCSeconds(dateValue);
        
        // 유효한 날짜인지 확인
        if (isNaN(myDate.getTime())) {
            console.warn('convertDate: 변환된 날짜가 유효하지 않습니다.');
            return '-';
        }
        
        return myDate.getFullYear() + '-' +('0' + (myDate.getMonth()+1)).slice(-2)+ '-' +  ('0' + myDate.getDate()).slice(-2) + ' '+myDate.getHours()+ ':'+('0' + (myDate.getMinutes())).slice(-2);
    } catch (error) {
        console.error('convertDate: 날짜 변환 중 오류 발생:', error);
        return '-';
    }
}

/**
 * 문자열 날짜를 지정된 형식으로 변경한다.
 * @param {string} strDate 
 * @returns {string}
 */
function strToDate(strDate){
    try {
        if (!strDate || typeof strDate !== 'string') {
            console.warn('strToDate: 유효하지 않은 날짜 문자열입니다.');
            return '-';
        }
        
        // YYYYMMDDHHmmss 형식인지 확인
        if (!/^\d{14}$/.test(strDate)) {
            console.warn('strToDate: 날짜 형식이 올바르지 않습니다. (YYYYMMDDHHmmss 형식 필요)');
            return strDate;
        }
        
        var dt = strDate.replace(/^(\d{4})(\d\d)(\d\d)(\d\d)(\d\d)(\d\d)$/, '$1/$2/$3 $4:$5:$6');
        return dt;
    } catch (error) {
        console.error('strToDate: 날짜 변환 중 오류 발생:', error);
        return strDate || '-';
    }
}