
/**
 * 화씨 온도를 섭씨로 변경
 * @param {} fahrenheit 
 * @returns 
 */
function convertF(fahrenheit){
    return Math.ceil(fahrenheit - 273.15);
}

/**
 * posix 날짜값을 일반 날짜 형식으로 변경한다
 * @param {*} dateValue 
 * @returns 
 */
function convertDate(dateValue){
    var myDate = new Date(0); 
    myDate.setUTCSeconds(dateValue);
    return myDate.getFullYear() + '-' +('0' + (myDate.getMonth()+1)).slice(-2)+ '-' +  ('0' + myDate.getDate()).slice(-2) + ' '+myDate.getHours()+ ':'+('0' + (myDate.getMinutes())).slice(-2);
}

/**
 * 문자열 날짜를 지정된 형식으로 변경한다.
 * @param {*} strDate 
 * @returns 
 */
function strToDate(strDate){
    var dt = strDate.replace(/^(\d{4})(\d\d)(\d\d)(\d\d)(\d\d)(\d\d)$/, '$1/$2/$3 $4:$5:$6');
    return dt;
}