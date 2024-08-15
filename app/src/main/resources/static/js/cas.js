/**
 * Original script: https://github.com/apereo/cas/blob/v7.0.5.1/support/cas-server-support-thymeleaf/src/main/resources/static/js/cas.js
 * See line 95 in our version for the changes
 */
function randomWord() {
    let things = ["admiring", "adoring", "affectionate", "agitated", "amazing",
        "angry", "awesome", "beautiful", "blissful", "bold", "boring",
        "brave", "busy", "charming", "clever", "cool", "compassionate", "competent",
        "confident", "dazzling", "determined", "sweet", "sad", "silly",
        "relaxed", "romantic", "sad", "serene", "sharp", "quirky", "scared",
        "sleepy", "stoic", "strange", "suspicious", "sweet", "tender", "thirsty",
        "trusting", "unruffled", "upbeat", "vibrant", "vigilant", "vigorous",
        "wizardly", "wonderful", "youthful", "zealous", "zen"];

    let names = ["austin", "borg", "bohr", "wozniak", "bose", "wu", "wing", "wilson",
        "boyd", "guss", "jobs", "hawking", "hertz", "ford", "solomon", "spence",
        "turing", "torvalds", "morse", "ford", "penicillin", "lovelace", "davinci",
        "darwin", "buck", "brown", "benz", "boss", "allen", "gates", "bose",
        "edison", "einstein", "feynman", "ferman", "franklin", "lincoln", "jefferson",
        "mandela", "gandhi", "curie", "newton", "tesla", "faraday", "bell",
        "aristotle", "hubble", "nobel", "pascal", "washington", "galileo"];

    let n1 = things[Math.floor(Math.random() * things.length)];
    let n2 = names[Math.floor(Math.random() * names.length)];
    return `${n1}_${n2}`;
}

function copyClipboard(element) {
    element.select();
    element.setSelectionRange(0, 99999);
    document.execCommand("copy");
}

function isValidURL(str) {
    let pattern = new RegExp('^(https?:\\/\\/)?' + // protocol
        '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.)+[a-z]{2,}|' + // domain name
        '((\\d{1,3}\\.){3}\\d{1,3}))' + // OR ip (v4) address
        '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*' + // port and path
        '(\\?[;&a-z\\d%_.~+=-]*)?' + // query string
        '(\\#[-a-z\\d_]*)?$', 'i'); // fragment locator
    return !!pattern.test(str);
}

function requestGeoPosition() {
    // console.log('Requesting GeoLocation data from the browser...');
    if (navigator.geolocation) {
        navigator.geolocation.watchPosition(showGeoPosition, logGeoLocationError,
            {maximumAge: 600000, timeout: 8000, enableHighAccuracy: true});
    } else {
        console.log('Browser does not support Geo Location');
    }
}

function logGeoLocationError(error) {
    switch (error.code) {
        case error.PERMISSION_DENIED:
            console.log('User denied the request for GeoLocation.');
            break;
        case error.POSITION_UNAVAILABLE:
            console.log('Location information is unavailable.');
            break;
        case error.TIMEOUT:
            console.log('The request to get user location timed out.');
            break;
        default:
            console.log('An unknown error occurred.');
            break;
    }
}

function showGeoPosition(position) {
    let loc = `${position.coords.latitude},${position.coords.longitude},${position.coords.accuracy},${position.timestamp}`;
    console.log(`Tracking geolocation for ${loc}`);
    $('[name="geolocation"]').val(loc);
}

function preserveAnchorTagOnForm() {
    $('#fm1').submit(() => {
        let location = self.document.location;

        let action = $('#fm1').attr('action');
        if (action === undefined) {
            action = location.href;
        } else {
            action += location.search + encodeURIComponent(location.hash);
        }
        $('#fm1').attr('action', action);

    });
}

function preventFormResubmission() {
    $('form').submit(() => {
        $(':submit').attr('disabled', true);
        // This part was changed. Before, the submit was disabled forever.
        // The behaviour was changed because the live validation was removed
        // and the form can now be submitted without a reload.
        setTimeout(() => {
            $(':submit').attr('disabled', false);
        }, 500);
        let altText = $(':submit').attr('data-processing-text');
        if (altText) {
            $(':submit').attr('value', altText);
        }
        return true;
    });
}

function writeToLocalStorage(value, key= "localStorageKey") {
    if (typeof (Storage) === "undefined") {
        console.log("Browser does not support local storage for write-ops");
    } else {
        window.localStorage.removeItem(key);
        window.localStorage.setItem(key, value);
        console.log(`Stored ${value} in local storage under key ${key}`);
    }
}

function readFromLocalStorage(key = "localStorageKey") {
    if (typeof (Storage) === "undefined") {
        console.log("Browser does not support local storage for read-ops");
        return null;
    }
    let payload = window.localStorage.getItem(key);
    console.log(`Read ${payload} in local storage under key ${key}`);
    window.localStorage.removeItem(key);
    return payload;
}

function writeToSessionStorage(value, key= "sessionStorageKey") {
    if (typeof (Storage) === "undefined") {
        console.log("Browser does not support session storage for write-ops");
    } else {
        window.sessionStorage.removeItem(key);
        window.sessionStorage.setItem(key, value);
        console.log(`Stored ${value} in session storage under key ${key}`);
    }
}

function readFromSessionStorage(key= "sessionStorageKey") {
    if (typeof (Storage) === "undefined") {
        console.log("Browser does not support session storage for read-ops");
        return null;
    }
    let payload = window.sessionStorage.getItem(key);
    console.log(`Read ${payload} in session storage under key ${key}`);
    window.sessionStorage.removeItem(key);
    return payload;
}

function resourceLoadedSuccessfully() {
    $(document).ready(() => {
        if (trackGeoLocation) {
            requestGeoPosition();
        }

        if ($(':focus').length === 0) {
            $('input:visible:enabled:first').focus();
        }

        preserveAnchorTagOnForm();
        preventFormResubmission();
        $('#fm1 input[name="username"],[name="password"]').trigger('input');
        $('#fm1 input[name="username"]').focus();

        $('.reveal-password').click(ev => {
            if ($('.pwd').attr('type') === 'text') {
                $('.pwd').attr('type', 'password');
                $(".reveal-password-icon").removeClass("mdi mdi-eye-off").addClass("mdi mdi-eye");
            } else {
                $('.pwd').attr('type', 'text');
                $(".reveal-password-icon").removeClass("mdi mdi-eye").addClass("mdi mdi-eye-off");
            }
            ev.preventDefault();
        });
        // console.log(`JQuery Ready: ${typeof (jqueryReady)}`);
        if (typeof (jqueryReady) == 'function') {
            jqueryReady();
        }
    });
}

function autoHideElement(id, timeout = 1500) {
    let elementToFadeOut = document.getElementById(id);
    function hideElement() {
        $(elementToFadeOut).fadeOut(500);
    }
    setTimeout(hideElement, timeout);
}