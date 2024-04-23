package com.example.webchatserver;


import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * This class represents a web socket server, a new connection is created and it receives a roomID as a parameter
 * **/
@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {

    // contains a static List of ChatRoom used to control the existing rooms and their users
    private static ArrayList<ChatRoom> chatRooms = new ArrayList<>();
    // you may add other attributes as you see fit

    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {
        String userID = session.getId();
        boolean roomExist = false;

        for(ChatRoom room: chatRooms){
            if(roomID.equals(room.getCode())){
                room.setUserName(userID,"");
                roomExist = true;
                break;
            }
        }
        if(!roomExist){
            chatRooms.add(new ChatRoom(roomID,userID));
        }
        session.getBasicRemote().sendText("{\"type\": \"" + roomID + "\", \"message\":\"(Server): Hello! Welcome " +
                "to the chat room. Please enter a username.\"}");
    }

    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        String userId = session.getId();
        for(ChatRoom room: chatRooms){
            if(room.inRoom(userId)){
                String username = room.getUsername(userId);
                String roomID = room.getCode();
                room.removeUser(userId);
                for (Session peer : session.getOpenSessions()) {
                    if (room.inRoom(peer.getId())) {
                        peer.getBasicRemote().sendText("{\"type\":\"" + roomID + "\", \"message\":\"(Server): "
                                + username + " left the chat room.\",\"userCount\":" + room.getUsers().size() + "}");
                    }
                }
                break;
            }
        }
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {
        String userID = session.getId();
        JSONObject jsonMsg = new JSONObject(comm);
        String roomID = ((String) jsonMsg.get("type")).substring(0,5);
        String message = (String) jsonMsg.get("msg");

        for(ChatRoom room: chatRooms) {
            if (roomID.equals(room.getCode())) {
                if (room.getUsername(userID).isEmpty()) { // runs if the user hasn't put in a username
                    room.setUserName(userID, message);
                    session.getBasicRemote().sendText("{\"type\": \"" + roomID + "\", \"message\":\"(Server): " +
                            "Welcome, " + message + "! These are the current users in the room: "
                            + getUsers(room) + "\",\"userCount\":" + room.getUsers().size() + "}");
                    for (Session peer : session.getOpenSessions()) {
                        if (room.inRoom(peer.getId())) {
                            peer.getBasicRemote().sendText("{\"type\": \"" + roomID + "\", \"message\":\"(Server): "
                                    + message + " has joined the chat.\",\"userCount\":" + room.getUsers().size() + "}");
                        }
                    }
                    break;
                } else {
                    if(message.equals("/users")){
                        session.getBasicRemote().sendText("{\"type\": \"" + roomID + "\", \"message\":\"(Server): " +
                                "These are the current users in the room: " + getUsers(room) + "\",\"userCount\":"
                                + room.getUsers().size() + "}");
                        break;
                    }else {
                        String username = room.getUsername(userID);
                        System.out.println(username);
                        for (Session peer : session.getOpenSessions()) {
                            if (room.inRoom(peer.getId())) {
                                if (!room.getUsername(peer.getId()).isEmpty()) {
                                    // message will appear for those who entered the room and have a username inputted
                                    peer.getBasicRemote().sendText("{\"type\": \"" + roomID + "\", \"message\":\"("
                                            + username + "): " + message + "\",\"userCount\":" +
                                            room.getUsers().size() + "}");
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    // get all the users in the current room and outputs a string
    public String getUsers(ChatRoom room){
        String currentUsers = "";
        for(String user: room.getUsers().values()){
            currentUsers+=user+", ";
        }
        currentUsers = currentUsers.substring(0,currentUsers.length()-2);
        return currentUsers;
    }
}