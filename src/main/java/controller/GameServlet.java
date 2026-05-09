package controller;

import com.google.gson.*;
import dao.*;
import model.*;
import service.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.*;
import java.util.*;

@WebServlet("/game")
public class GameServlet extends HttpServlet {
    private final BoardService    boardService    = new BoardService();
    private final GameService     gameService     = new GameService();
    private final ScoreService    scoreService    = new ScoreService();
    private final GameHistoryDAO  historyDAO      = new GameHistoryDAO();
    private final LeaderboardDAO  leaderboardDAO  = new LeaderboardDAO();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(false);
        if (session == null || session.getAttribute("playerId") == null) {
            resp.sendRedirect(req.getContextPath() + "/login"); return;
        }
        String roomId = (String) session.getAttribute("roomId");
        if (roomId == null) { resp.sendRedirect(req.getContextPath() + "/setup"); return; }

        try {
            String    playerId    = (String) session.getAttribute("playerId");
            String    mode        = (String) session.getAttribute("mode");
            String    difficulty  = (String) session.getAttribute("difficulty");
            Board     playerBoard = (Board)  session.getAttribute("board");

            GameState gs = (GameState) session.getAttribute("gameState");
            if (gs == null) {
                gs = gameService.createGameState(roomId, playerId);
                gs.setMode(mode);
                gs.setDifficulty(difficulty);
                gs.setStartedAt(LocalDateTime.now());
                session.setAttribute("gameState", gs);
            }

            if ("PvE".equals(mode)) {
                Board aiBoard = (Board) session.getAttribute("aiBoard");
                if (aiBoard == null) {
                    aiBoard = boardService.createBoard(UUID.randomUUID().toString(), roomId, "AI_PLAYER");
                    boardService.autoPlace(aiBoard);
                    session.setAttribute("aiBoard", aiBoard);
                }
                AIService aiService = (AIService) session.getAttribute("aiService");
                if (aiService == null) {
                    aiService = new AIService(difficulty != null ? difficulty : "Easy");
                    session.setAttribute("aiService", aiService);
                }
            }

            req.setAttribute("gameState",   gs);
            req.setAttribute("playerBoard", playerBoard);
            req.getRequestDispatcher("/game.jsp").forward(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.sendRedirect(req.getContextPath() + "/setup");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession(false);

        if (session == null || session.getAttribute("playerId") == null) {
            out.print("{\"error\":\"not_logged_in\"}"); return;
        }

        String    playerId    = (String)   session.getAttribute("playerId");
        String    mode        = (String)   session.getAttribute("mode");
        GameState gs          = (GameState) session.getAttribute("gameState");
        Board     playerBoard = (Board)    session.getAttribute("board");
        Board     aiBoard     = (Board)    session.getAttribute("aiBoard");
        AIService aiService   = (AIService) session.getAttribute("aiService");

        if (gs == null || playerBoard == null) { out.print("{\"error\":\"no_game\"}"); return; }
        if (!"ongoing".equals(gs.getStatus()))  { out.print("{\"error\":\"game_over\"}"); return; }

        try {
            String action = req.getParameter("action");

            // Timeout skip
            if ("skip".equals(action)) {
                JsonObject response = new JsonObject();
                response.addProperty("skipped", true);
                if ("PvE".equals(mode)) {
                    JsonObject aiMove = doAITurn(gs, playerBoard, aiBoard, aiService, playerId, session, response);
                    if (aiMove != null) response.add("aiMove", aiMove);
                }
                out.print(response.toString()); return;
            }

            if (!gs.getCurrentTurnId().equals(playerId)) {
                out.print("{\"error\":\"not_your_turn\"}"); return;
            }

            int x = Integer.parseInt(req.getParameter("x"));
            int y = Integer.parseInt(req.getParameter("y"));

            Board      targetBoard = "PvE".equals(mode) ? aiBoard : playerBoard;
            ShotResult result      = gameService.fireShot(targetBoard, gs, playerId, x, y);

            JsonObject response = new JsonObject();
            response.addProperty("result", result.getResult().name());
            response.addProperty("x", x);
            response.addProperty("y", y);

            if (result.getResult() == ShotResult.ResultType.GAME_OVER) {
                gs.setStatus("finished");
                gs.setWinnerId(playerId);
                gameService.finishGame(gs, playerId);

                long duration = gs.getStartedAt() != null
                    ? Duration.between(gs.getStartedAt(), LocalDateTime.now()).getSeconds() : 0;
                int score = scoreService.calculateScore(gs.getTotalTurns(), (int) duration);

                // Save to DB
                saveGameRecord(gs, playerId, null, playerId, mode, score, 0);
                saveLeaderboard(playerId, true, score);

                response.addProperty("score", score);
                response.addProperty("winner", session.getAttribute("playerName").toString());
                session.setAttribute("lastScore", score);
                session.setAttribute("gameWinner", session.getAttribute("playerName").toString());
            } else if ("PvE".equals(mode)) {
                JsonObject aiMove = doAITurn(gs, playerBoard, aiBoard, aiService, playerId, session, response);
                if (aiMove != null) response.add("aiMove", aiMove);
            }

            out.print(response.toString());
        } catch (Exception e) {
            e.printStackTrace();
            out.print("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private JsonObject doAITurn(GameState gs, Board playerBoard, Board aiBoard, AIService aiService,
                                 String playerId, HttpSession session, JsonObject response) {
        if (aiService == null) return null;
        gs.setCurrentTurnId("AI_PLAYER");

        int[] target = aiService.selectTarget(playerBoard);
        if (target == null) return null;

        ShotResult aiResult = gameService.fireShot(playerBoard, gs, "AI_PLAYER", target[0], target[1]);
        boolean hit  = aiResult.getResult() != ShotResult.ResultType.MISS;
        boolean sunk = aiResult.getResult() == ShotResult.ResultType.SUNK
                    || aiResult.getResult() == ShotResult.ResultType.GAME_OVER;
        aiService.notifyResult(target[0], target[1], hit, sunk, playerBoard);

        JsonObject aiMove = new JsonObject();
        aiMove.addProperty("x",      target[0]);
        aiMove.addProperty("y",      target[1]);
        aiMove.addProperty("result", aiResult.getResult().name());

        if (aiResult.getResult() == ShotResult.ResultType.GAME_OVER) {
            gs.setStatus("finished");
            gs.setWinnerId("AI_PLAYER");
            gameService.finishGame(gs, "AI_PLAYER");

            // Save to DB: player lost
            saveGameRecord(gs, playerId, null, "AI_PLAYER", gs.getMode(), 0, 0);
            saveLeaderboard(playerId, false, 0);

            response.addProperty("aiWon", true);
            session.setAttribute("gameWinner", "AI");
            session.setAttribute("lastScore", 0);
        } else {
            gs.setCurrentTurnId(playerId);
        }
        return aiMove;
    }

    private void saveGameRecord(GameState gs, String p1Id, String p2Id, String winnerId,
                                 String mode, int p1Score, int p2Score) {
        try {
            GameRecord record = new GameRecord();
            record.setId(UUID.randomUUID().toString());
            record.setRoomId(gs.getRoomId());
            record.setPlayer1Id(p1Id);
            record.setPlayer2Id(p2Id);
            record.setWinnerId(winnerId);
            record.setMode(mode);
            record.setPlayer1Score(p1Score);
            record.setPlayer2Score(p2Score);
            record.setTotalShots(gs.getTotalTurns());
            long dur = gs.getStartedAt() != null
                ? Duration.between(gs.getStartedAt(), LocalDateTime.now()).getSeconds() : 0;
            record.setDurationSeconds((int) dur);
            historyDAO.insert(record);
        } catch (Exception e) {
            System.err.println("saveGameRecord failed (no DB?): " + e.getMessage());
        }
    }

    private void saveLeaderboard(String userId, boolean won, int score) {
        try {
            leaderboardDAO.upsert(userId, won, score);
        } catch (Exception e) {
            System.err.println("saveLeaderboard failed (no DB?): " + e.getMessage());
        }
    }
}
