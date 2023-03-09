
package ubc.cosc322;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sfs2x.client.entities.Room;
import ygraph.ai.smartfox.games.BaseGameGUI;
import ygraph.ai.smartfox.games.GameClient;
import ygraph.ai.smartfox.games.GameMessage;
import ygraph.ai.smartfox.games.GamePlayer;
import ygraph.ai.smartfox.games.amazons.AmazonsGameMessage;
import ygraph.ai.smartfox.games.amazons.HumanPlayer;

/**
 * An example illustrating how to implement a GamePlayer
 * 
 * @author Yong Gao (yong.gao@ubc.ca) Jan 5, 2021
 *
 */
public class COSC322Test extends GamePlayer {

	private GameClient gameClient = null;
	private BaseGameGUI gamegui = null;

	private String userName = null;
	private String passwd = null;

	int[][][] state;
	private int whiteQueen = 1;
	private int blackQueen = 2;
	private int arrow = 3; // could be any number other than 1, 2
	private int myQueen = -1;
	private int opponentQueen = -1;
	
	private boolean movedFirst = true;

	/**
	 * The main method
	 * 
	 * @param args for name and passwd (current, any string would work)
	 */
	public static void main(String[] args) {
		COSC322Test player = new COSC322Test("COSC322_GROUP_7", "pass");
		//HumanPlayer player = new HumanPlayer();

		if (player.getGameGUI() == null) {
			player.Go();
		} else {
			BaseGameGUI.sys_setup();
			java.awt.EventQueue.invokeLater(new Runnable() {
				public void run() {
					player.Go();
				}
			});
		}
	}

	/**
	 * Any name and passwd
	 * 
	 * @param userName
	 * @param passwd
	 */
	public COSC322Test(String userName, String passwd) {
		this.userName = userName;
		this.passwd = passwd;
		
		// To make a GUI-based player, create an instance of BaseGameGUI
		// and implement the method getGameGUI() accordingly
		this.gamegui = new BaseGameGUI(this);
	}

	@Override
	public void onLogin() {
		userName = gameClient.getUserName();
		if (gamegui != null) {
			gamegui.setRoomInformation(gameClient.getRoomList());
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean handleGameMessage(String messageType, Map<String, Object> msgDetails) {
		// This method will be called by the GameClient when it receives a game-related
		// message
		// from the server.

		// For a detailed description of the message types and format,
		// see the method GamePlayer.handleGameMessage() in the game-client-api
		// document.
		switch (messageType) {
		case GameMessage.GAME_STATE_BOARD:					
			ArrayList<Integer> stateArr = (ArrayList<Integer>) (msgDetails.get(AmazonsGameMessage.GAME_STATE));
			this.getGameGUI().setGameState(stateArr);
			
			System.out.println("GAME_STATE_BOARD");
			InitalizeBoard();
			
			break; /* THIS GAME STATE BOARD IS A MESSAGE CONTAINING THE CURRENT STATE */
		case GameMessage.GAME_ACTION_MOVE:
			ArrayList<Integer> queenPosCurr = (ArrayList<Integer>) (msgDetails.get(AmazonsGameMessage.QUEEN_POS_CURR));
			ArrayList<Integer> queenPosNext = (ArrayList<Integer>) (msgDetails.get(AmazonsGameMessage.QUEEN_POS_NEXT));
			ArrayList<Integer> arrowPos = (ArrayList<Integer>) (msgDetails.get(AmazonsGameMessage.ARROW_POS));
			
			ApplyOpponentMove(queenPosCurr, queenPosNext, arrowPos);
			MakeMove();
			break;

		case GameMessage.GAME_ACTION_START:
			
			String playingWhiteQueens = (String) msgDetails.get(AmazonsGameMessage.PLAYER_WHITE);
			String playingBlackQueens = (String) msgDetails.get(AmazonsGameMessage.PLAYER_BLACK);
			assert (!playingWhiteQueens.equals(playingBlackQueens));	
			SetMyQueen(playingWhiteQueens, playingBlackQueens);
			
			if(this.myQueen == this.whiteQueen && movedFirst) {
				movedFirst = false;
				MakeMove();
			}
			break;
			
		default:
			assert (false);
			break;
		}
		return true;
	}
	
	public void SetMyQueen(String playingWhiteQueens, String playingBlackQueens) {
    	if(this.userName().equals(playingWhiteQueens)) {
    		 this.myQueen = this.whiteQueen;
    		 this.opponentQueen = this.blackQueen;
    	} else if(this.userName().equals(playingBlackQueens)) {
    		 this.myQueen = this.blackQueen;
    		 this.opponentQueen = this.whiteQueen;
    	} else {
    		System.out.println("Fatal error, invalid queen value received " + this.myQueen + ", please restart");
    		assert(false);
    	}
    	
    	System.out.println("SetMyQueen " + this.myQueen);
	}
	
	public void MakeMove() {
		
		MonteCarlo ai = new MonteCarlo(new TreeNode(this.state, this.myQueen), 5000, 1.4);
		AmazonsAction action = ai.MCTS();
		
		if(action != null) {
			ArrayList<Integer> aiQueenPosCurr = new ArrayList<Integer>();
			aiQueenPosCurr.add(action.queenSrcY + 1);
			aiQueenPosCurr.add(action.queenSrcX + 1);
			
			ArrayList<Integer> aiQueenPosNext = new ArrayList<Integer>();
			aiQueenPosNext.add(action.queenDestY + 1);
			aiQueenPosNext.add(action.queenDestX + 1);
			
			ArrayList<Integer> aiArrowPos = new ArrayList<Integer>();
			aiArrowPos.add(action.arrowDestY + 1);
			aiArrowPos.add(action.arrowDestX + 1);
			
			this.getGameGUI().updateGameState(aiQueenPosCurr, aiQueenPosNext, aiArrowPos);
			this.getGameClient().sendMoveMessage(aiQueenPosCurr, aiQueenPosNext, aiArrowPos);
			this.state = AmazonsAction.applyAction(action, this.state);
		} else {
			System.out.println("You lose");
			// TODO: is there a server message you send once the game is over?
		}
	}
	
	public void ApplyOpponentMove(ArrayList<Integer> queenPosCurr, ArrayList<Integer> queenPosNext, ArrayList<Integer> arrowPos) {
		AmazonsAction action = new AmazonsAction(queenPosCurr.get(1)-1, queenPosCurr.get(0)-1, queenPosNext.get(1)-1, queenPosNext.get(0)-1, arrowPos.get(1)-1, arrowPos.get(0)-1);
		this.state = AmazonsAction.applyAction(action, this.state);
		this.getGameGUI().updateGameState(queenPosCurr, queenPosNext, arrowPos);
	}
	
	public void InitalizeBoard() {
		movedFirst = true;
		
		this.state = new int[2][10][10];

		// hard coded but ideally set using stateArr
		this.state[0][0][3] = this.whiteQueen;
		this.state[0][0][6] = this.whiteQueen;

		this.state[0][3][0] = this.whiteQueen;
		this.state[0][3][9] = this.whiteQueen;

		this.state[0][6][0] = this.blackQueen;
		this.state[0][6][9] = this.blackQueen;

		this.state[0][9][3] = this.blackQueen;
		this.state[0][9][6] = this.blackQueen;
		
		this.state[1] = AmazonsUtility.getMobilityMap(this.state[0]);
	}

	@Override
	public String userName() {
		return userName;
	}

	@Override
	public GameClient getGameClient() {
		return this.gameClient;
	}

	@Override
	public BaseGameGUI getGameGUI() {
		return this.gamegui;
	}

	@Override
	public void connect() {
		gameClient = new GameClient(userName, passwd, this);
	}

}// end of class
