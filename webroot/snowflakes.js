var script = document.createElement("script")
script.type = "text/javascript"
script.src = "https://unpkg.com/magic-snowflakes/dist/snowflakes.min.js"
document.getElementsByTagName("body")[0].appendChild(script)

var trigger = document.createElement("script")
trigger.type = "text/javascript"
trigger.innerHTML = "var sf = new Snowflakes();"
script.onload = function() {
    document.getElementsByTagName("body")[0].appendChild(trigger)
};