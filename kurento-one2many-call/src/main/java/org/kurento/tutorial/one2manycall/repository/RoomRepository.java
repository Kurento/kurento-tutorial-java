package org.kurento.tutorial.one2manycall.repository;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.client.KurentoClient;
import org.kurento.tutorial.one2manycall.model.Room;
import org.kurento.tutorial.one2manycall.model.UserSession;

public class RoomRepository {
	private final static ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<String, Room>();	
	
	public static Map<String, Room> rooms() {
        return Collections.unmodifiableMap(rooms);
    }

    public static Room addRoom(Room room) {
        rooms.put(room.getName(), room);
        return room;
    }
    
    public static void joinRoomByViewer(UserSession user, String roomName) {
    	rooms.get(roomName).joinByViewer(user);
    }
    
    public static void joinRoomByPresenter(UserSession user, String roomName) {
    	Room room = rooms().get(roomName);
    	if(room == null) {
    		room = new Room(roomName, KurentoClient.create());
    		addRoom(room);
    	}
    	room.joinByPresenter(user);
    }
    
    public static Room findByPresenterId(String sessionId) {
    	for(Room room : rooms.values()) {
    		if(room.getPresenter() != null && room.getPresenter().getSession().getId().equals(sessionId)) {
    			return room;
    		}
    	}
    	return null;
    }
}
