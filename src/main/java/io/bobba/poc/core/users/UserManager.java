package io.bobba.poc.core.users;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.communication.outgoing.users.LoginOkComposer;
import io.bobba.poc.communication.outgoing.users.UpdateCreditsBalanceComposer;
import io.bobba.poc.core.gameclients.GameClient;
import io.bobba.poc.misc.logging.LogLevel;
import io.bobba.poc.misc.logging.Logging;

public class UserManager {
	private Map<Integer, User> users;
	private int nextId;
	
	public UserManager() {
		this.users = new HashMap<>();
		this.nextId = 1;
	}
	
	public User getUser(int id) {
		return users.getOrDefault(id, null);
	}
	
	private User addUser(String username, String look, GameClient client) {
		User user = new User(nextId++, username, "I \uD83D\uDC96 bobba", look, client);
		this.users.put(user.getId(), user);
		return user;
	}
	public void removeUser(int id, User user) {
		if (users.containsKey(id) && users.get(id).equals(user)) {
			users.remove(id);
		}
	}
	private void addDummyFriends(User user) {
		for (User otherUser: new ArrayList<>(users.values())) {
			if (user != otherUser) {
				user.getMessenger().addHardFriendship(otherUser);	
			}
		}
		user.getMessenger().serializeFriends();
	}
	// check if user already ingame
	public boolean isUserMapContainsUsername(String username) {
		for (User user : users.values()) {
			if (user.getUsername().equals(username)) {
				return true;
			}
		}
		return false;
	}
	private boolean checkUserPassword(String username, String password) throws SQLException {
		String sql = "SELECT COUNT(*) FROM users WHERE username = ? and password = ?";
		try (Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, username);
			statement.setString(2, password);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					int count = resultSet.getInt(1);
					return count > 0;
				} else {
					return false;
				}
			}
		} catch (SQLException e) {
			throw e;
		}
	}
	public void tryLogin(GameClient client, String username, String look, String password) throws SQLException{
        if (client.getUser() == null && !isUserMapContainsUsername(username) && checkUserPassword(username, password)) {
        	User user = addUser(username, look, client);
            client.setUser(user);            
            Logging.getInstance().writeLine(client.getUser().getUsername() + " (" + client.getUser().getId() + ") has logged in!", LogLevel.Verbose, this.getClass());           

            client.sendMessage(new LoginOkComposer(user.getId(), user.getUsername(), user.getLook(), user.getMotto()));
            client.sendMessage(new UpdateCreditsBalanceComposer(user.getCredits()));
            
            addDummyFriends(user);
        } else {
            Logging.getInstance().writeLine("Client already logged!", LogLevel.Warning, this.getClass());
            client.stop();
        }
    }
	// already registered check
	private boolean isDbContainsUsername(String username) throws SQLException {	
		String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
		try (Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, username);
			try (ResultSet resultSet = statement.executeQuery()) {
				if (resultSet.next()) {
					int count = resultSet.getInt(1);
					return count > 0;
				} else {
					return false;
				}
			}
		} catch (SQLException e) {
			throw e;
		}
	}
	private void saveUser(String username, String password) throws SQLException {
		String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
		try (Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection();
			 PreparedStatement statement = connection.prepareStatement(sql)) {
			statement.setString(1, username);
			statement.setString(2, password);
			statement.executeUpdate();
		} catch (SQLException e) {
			throw e;
		}
	}
	public void trySignup(GameClient client, String username, String look, String password) throws SQLException{
        if (client.getUser() == null && !isDbContainsUsername(username)) {
        	saveUser(username, password);

			// remove later
			User user = addUser(username, look, client);
            client.setUser(user);  
			//

            Logging.getInstance().writeLine(client.getUser().getUsername() + " (" + client.getUser().getId() + ") has created profile", LogLevel.Verbose, this.getClass());           
			
			// temp -> redo later
            client.sendMessage(new LoginOkComposer(user.getId(), user.getUsername(), user.getLook(), user.getMotto()));
            //client.sendMessage(...); something that displays "Registration complete" on react

        } else {
            Logging.getInstance().writeLine("Username already existing!", LogLevel.Warning, this.getClass());
            client.stop();
        }
    }
}
