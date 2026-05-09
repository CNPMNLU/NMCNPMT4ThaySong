package service;

import model.*;
import java.util.*;

public class GameService {

    public ShotResult fireShot(Board targetBoard, GameState gameState, String shooterId, int x, int y) {
        Cell[][] cells = targetBoard.getCells();
        if (cells[x][y] == null) cells[x][y] = new Cell(UUID.randomUUID().toString(), targetBoard.getId(), x, y);
        if (cells[x][y].isHit()) {
            ShotResult r = new ShotResult();
            r.setResult(ShotResult.ResultType.MISS);
            r.setX(x); r.setY(y);
            return r;
        }

        cells[x][y].setHit(true);

        ShotResult result = new ShotResult();
        result.setId(UUID.randomUUID().toString());
        result.setGameStateId(gameState.getId());
        result.setShooterId(shooterId);
        result.setTargetBoardId(targetBoard.getId());
        result.setX(x); result.setY(y);
        result.setTurnNumber(gameState.getTotalTurns() + 1);

        if (cells[x][y].isHasShip()) {
            String shipId = cells[x][y].getShipId();
            result.setShipId(shipId);
            Ship hitShip = targetBoard.getShips().stream()
                    .filter(s -> s.getId().equals(shipId)).findFirst().orElse(null);
            if (hitShip != null) {
                boolean sunk = checkSunk(hitShip, cells);
                if (sunk) {
                    hitShip.setSunk(true);
                    result.setResult(ShotResult.ResultType.SUNK);
                    if (checkWinCondition(targetBoard)) {
                        result.setResult(ShotResult.ResultType.GAME_OVER);
                    }
                } else {
                    result.setResult(ShotResult.ResultType.HIT);
                }
            } else {
                result.setResult(ShotResult.ResultType.HIT);
            }
        } else {
            result.setResult(ShotResult.ResultType.MISS);
        }

        gameState.setTotalTurns(gameState.getTotalTurns() + 1);
        return result;
    }

    private boolean checkSunk(Ship ship, Cell[][] cells) {
        for (int i = 0; i < ship.getLength(); i++) {
            int cx = "H".equals(ship.getDirection()) ? ship.getStartX() + i : ship.getStartX();
            int cy = "V".equals(ship.getDirection()) ? ship.getStartY() + i : ship.getStartY();
            if (cells[cx][cy] == null || !cells[cx][cy].isHit()) return false;
        }
        return true;
    }

    public boolean checkWinCondition(Board board) {
        return board.getShips().stream().allMatch(Ship::isSunk);
    }

    public void switchTurn(GameState gameState, String player1Id, String player2Id) {
        if (gameState.getCurrentTurnId().equals(player1Id)) {
            gameState.setCurrentTurnId(player2Id);
        } else {
            gameState.setCurrentTurnId(player1Id);
        }
    }

    public GameState createGameState(String roomId, String player1Id) {
        GameState gs = new GameState();
        gs.setId(UUID.randomUUID().toString());
        gs.setRoomId(roomId);
        gs.setCurrentTurnId(player1Id);
        gs.setStatus("ongoing");
        gs.setTotalTurns(0);
        return gs;
    }

    public void finishGame(GameState gs, String winnerId) {
        gs.setStatus("finished");
        gs.setWinnerId(winnerId);
    }

    public void updateRoomStatus(String roomId, String status) {
        // no-op without DB
    }
}
