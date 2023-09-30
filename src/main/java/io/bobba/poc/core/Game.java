package io.bobba.poc.core;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

import io.bobba.poc.BobbaEnvironment;
import io.bobba.poc.core.catalogue.Catalogue;
import io.bobba.poc.core.gameclients.GameClientManager;
import io.bobba.poc.core.items.BaseItemManager;
import io.bobba.poc.core.navigator.Navigator;
import io.bobba.poc.core.rooms.RoomManager;
import io.bobba.poc.core.users.UserManager;
import io.bobba.poc.database.Database;
import io.bobba.poc.misc.SSLHelper;
import io.bobba.poc.misc.logging.Logging;
import io.bobba.poc.net.ConnectionManager;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;

public class Game {
	private ConnectionManager connectionManager;
	private GameClientManager gameClientManager;
	private UserManager userManager;
	private BaseItemManager itemManager;
	private Catalogue catalogue;
	private Navigator navigator;
	private RoomManager roomManager;
	private Database database;

	private final int DELTA_TIME = 500;
	
	public Game() throws Exception {
		this.database = new Database(10, 2, BobbaEnvironment.getConfigManager().getMysqlHost(), Integer.parseInt(BobbaEnvironment.getConfigManager().getMysqlPort()), BobbaEnvironment.getConfigManager().getMysqlDatabase(), BobbaEnvironment.getConfigManager().getMysqlUser(), BobbaEnvironment.getConfigManager().getMysqlPass());
		this.gameClientManager = new GameClientManager();
		this.userManager = new UserManager();
		this.itemManager = new BaseItemManager();
		this.catalogue = new Catalogue();
		this.roomManager = new RoomManager();
		this.navigator = new Navigator();
	}

	public void initialize(int port) throws Exception {
		if (BobbaEnvironment.getConfigManager().getSslEnabled().toLowerCase().equals("true")) {
			this.connectionManager = new ConnectionManager(port, this.gameClientManager, SSLHelper.loadSslContext());
		} else {
			this.connectionManager = new ConnectionManager(port, this.gameClientManager);
		}

		Thread roomThread = new Thread(new Runnable() {
			@Override
			public void run() {
				gameThread();
			}
		});
		roomThread.start();

		initializeLiquibase();
		getItemManager().initialize();
		getRoomManager().initialize();
		getCatalogue().initialize();
	}

	private void initializeLiquibase() throws SQLException, LiquibaseException {
		try (Connection connection = BobbaEnvironment.getGame().getDatabase().getDataSource().getConnection()) {

			liquibase.database.Database database = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(new JdbcConnection(connection));

			Liquibase liquibase = new liquibase.Liquibase("liquibase/master.xml", new ClassLoaderResourceAccessor(), database);
			liquibase.update(new Contexts(), new LabelExpression());
		} catch(SQLException e) {
			throw e;
		}
	}

	private void gameThread() {
		Instant starts, ends;
		while (true) {
			try {
				starts = Instant.now();
				onCycle();
				ends = Instant.now();

				long sleepTime = DELTA_TIME - Duration.between(starts, ends).toMillis();
				if (sleepTime < 0) {
					sleepTime = 0;
				}

				Thread.sleep(sleepTime);

			} catch (Exception e) {
				Logging.getInstance().logError("Game thread error", e, this.getClass());
			}
		}
	}

	public void onCycle() {
		this.roomManager.onCycle();
	}

	public ConnectionManager getConnectionManager() {
		return connectionManager;
	}

	public GameClientManager getGameClientManager() {
		return gameClientManager;
	}

	public UserManager getUserManager() {
		return userManager;
	}

	public BaseItemManager getItemManager() {
		return itemManager;
	}

	public Catalogue getCatalogue() {
		return catalogue;
	}

	public Navigator getNavigator() {
		return navigator;
	}

	public RoomManager getRoomManager() {
		return roomManager;
	}
	
	public Database getDatabase() {
		return database;
	}
}
