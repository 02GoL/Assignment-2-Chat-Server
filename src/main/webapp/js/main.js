let roomIDs = new Set();
let callURL = "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet";
let currentRoom = null;
let ws = null;

// handles input when enter has been pressed
document.getElementById("input").addEventListener("keyup", function(event){
    if (event.key === "Enter" && event.target.value !== "") {
        sendData(event.target.value);
        event.target.value = "";
    }
});

// handles input from the send button
document.getElementById("inputButton").addEventListener("click",function (){
    if(!document.getElementById("input").value === "") {
        sendData(document.getElementById("input").value);
        document.getElementById("input").value = "";
    }
});

function sendData(msg){
    let request = {"type":currentRoom, "msg":msg};
    console.log(request);
    ws.send(JSON.stringify(request));
}

function newRoom() {
    // calling the ChatServlet to retrieve a new room ID
    let callURL = "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet";
    fetch(callURL, {
        method: 'GET',
        headers: {
            'Accept': 'text/plain',
        },
    })
        .then(response => response.text())
        .then(response => enterRoom(response))
        .catch((err) => {
            console.log("Something went wrong: " + err);
        }); // enter the room with the code
}

function leaveRoom(){
    if(ws != null) {
        document.getElementById("log").value += "[" + timestamp()
            + "] (Server) You have left your current room\n";
        console.log("Leaving room");
        ws.close();
        ws = null;
        currentRoom = null;
    }
}

function enterRoom(code) {
    leaveRoom(code);
    console.log("Entering room: " + code);
    currentRoom = code;
    // create the web socket
    ws = new WebSocket("ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/" + code);
    // parse messages received from the server and update the UI accordingly
    ws.onmessage = function (event) {
        console.log(event.data);
        // parsing the server's message as json
        let message = JSON.parse(event.data);

        // handle message
        document.getElementById("log").value += "[" + timestamp()
            + " | " + message.type + "] " + message.message + "\n";
    }
}

function timestamp() {
    let d = new Date(), minutes = d.getMinutes();
    if (minutes < 10) minutes = '0' + minutes;
    return d.getHours() + ':' + minutes;
}

// updates the room list
function updateRoomList(roomList){
    if(roomList.rooms.length > 0) {
        for (let roomID of roomList.rooms) {
            if (!roomIDs.has(roomID)) {
                roomIDs.add(roomID);
                const buttonDiv = document.getElementsByClassName("roomButtons")[0];
                let buttonElement = document.createElement("button");
                buttonElement.addEventListener('click', function(){
                    enterRoom(roomID)
                });
                buttonElement.appendChild(document.createTextNode(roomID));
                buttonDiv.appendChild(buttonElement);
            }
        }
    }
}

// Handles initialization of room list when site is opened
function fetchRooms(){
    fetch(callURL, {
        method: 'POST',
        headers: {
            'Accept': 'application/json',
        },
    })
        .then(response => response.json())
        .then(response => updateRoomList(response))
        .catch((err) => {
            console.log("Something went wrong: " + err);
        });
}

(function(){
    fetchRooms() // once website loads it will display all the rooms
    setInterval(() => fetchRooms(),1000); // sets an interval that updates the room list
})();

