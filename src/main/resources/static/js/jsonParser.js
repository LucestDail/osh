
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
    var trafficLength = trafficArray.length > 5 ? 5 : trafficArray.length;
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
 * JSON 파서(응급)
 * @param {*} strJson 
 * @returns 
 */
function emergencyJsonParser(strJson){
    var emergencyJson = JSON.parse(getTargetJson("emergencyJson",strJson));
    var emergencyArray = emergencyJson['body'];
    if(emergencyArray === null){
        return;
    }
    emergencyArray.sort((a, b) => new Date(b.CRT_DT) - new Date(a.CRT_DT));
    var emergencyTbody = document.querySelector('#emergencyTbody');
    var trElement = "";
    for(var i = 0; i < emergencyArray.length; i++) {
        var targetJson = emergencyArray[i];
        trElement += "<tr>";
        trElement   += "<td>" + targetJson.CRT_DT + "</td>";
        trElement   += "<td>" + targetJson.RCPTN_RGN_NM + "</td>";
        trElement   += "<td>" + targetJson.MSG_CN + "</td>";
        trElement   += "<td>" + targetJson.DST_SE_NM + "</td>";
        trElement   += "<td>" + targetJson.EMRG_STEP_NM + "</td>"; 
        trElement+= "</tr>";
    }
    emergencyTbody.innerHTML = trElement;
}

/**
 * JSON 파서(연합뉴스)
 * @param {*} strJson 
 * @returns 
 */
function yeonhapJsonParser(strJson){
    var yeonhapJson = JSON.parse(getTargetJson("yeonhapJson",strJson));
    var yeonhapArray = yeonhapJson['body'];
    if(yeonhapArray === null){
        return;
    }
    yeonhapArray.sort((a, b) => new Date(b.CRT_DT) - new Date(a.CRT_DT));
    var yeonhapLength = yeonhapArray.length > 5 ? 5 : yeonhapArray.length;
    var yeonhapTbody = document.querySelector('#yeonhapTbody');
    var trElement = "";
    for(var i = 0; i < yeonhapLength; i++) {
        var targetJson = yeonhapArray[i];
        trElement += "<tr>";
        trElement   += "<td>" + targetJson.CRT_DT.substr(0,19) + "</td>";
        trElement   += "<td>" + targetJson.YNA_TTL + "</td>";
        trElement   += "<td>" + targetJson.YNA_CN + "</td>";
        trElement+= "</tr>";
    }
    yeonhapTbody.innerHTML = trElement;
}

/**
 * JSON 파서(날씨)
 * @param {*} strJson 
 */
function weatherJsonParser(strJson){
    var weatherTbody = document.querySelector('#weatherTbody');
    var trElement = "";
    var weather1Json = JSON.parse(getTargetJson("weatherJson1",strJson));
    trElement += "<tr>";
    trElement   += "<td>" + "창원" + "</td>";
    trElement   += "<td>" + weather1Json.weather[0].main + "</td>";
    trElement   += "<td>" + convertF(weather1Json.main.temp) + "℃</td>";
    trElement   += "<td>" + weather1Json.clouds.all + "%</td>";
    trElement   += "<td>" + weather1Json.main.humidity + "%</td>";
    trElement   += "<td>" + weather1Json.wind.speed + "m/s</td>";
    trElement   += "<td>" + convertDate(weather1Json.sys.sunrise) + "</td>"; 
    trElement   += "<td>" + convertDate(weather1Json.sys.sunset) + "</td>"; 
    trElement+= "</tr>";
    var weather2Json = JSON.parse(getTargetJson("weatherJson2",strJson));
    trElement += "<tr>";
    trElement   += "<td>" + "수원" + "</td>";
    trElement   += "<td>" + weather2Json.weather[0].main + "</td>";
    trElement   += "<td>" + convertF(weather2Json.main.temp) + "℃</td>";
    trElement   += "<td>" + weather2Json.clouds.all + "%</td>";
    trElement   += "<td>" + weather2Json.main.humidity + "%</td>";
    trElement   += "<td>" + weather2Json.wind.speed + "m/s</td>";
    trElement   += "<td>" + convertDate(weather2Json.sys.sunrise) + "</td>"; 
    trElement   += "<td>" + convertDate(weather2Json.sys.sunset) + "</td>"; 
    trElement+= "</tr>";
    weatherTbody.innerHTML = trElement;
}

/**
 * JSON 파서(서버정보)
 * @param {*} strJson 
 */
function applicationJsonParser(strJson){
    var applicationJson = JSON.parse(getTargetJson("applicationJson",strJson));
    document.querySelector('#currentTime').innerHTML = applicationJson.currentTime;
    var applicationTbody = document.querySelector('#applicationTbody');
    var trElement = "";
    trElement += "<tr>";
    trElement   += "<td>" + applicationJson.systemArchitecture + "</td>";
    trElement   += "<td>" + applicationJson.systemName + "</td>";
    trElement   += "<td>" + applicationJson.systemLoadAverage + "%</td>";
    trElement   += "<td>" + applicationJson.memory + "</td>";
    trElement   += "<td>" + applicationJson.useMemory + "</td>"; 
    trElement   += "<td>" + applicationJson.freeMemory + "</td>"; 
    trElement+= "</tr>";
    applicationTbody.innerHTML = trElement;
}
