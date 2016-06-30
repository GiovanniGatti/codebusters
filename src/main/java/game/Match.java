package game;

import game.Player.AI;

import java.util.concurrent.Callable;

/**
 * Represents a single match between any two IAs
 */
final class Match implements Callable<Match.MatchResult> {

    private final AI player;
    private final AI opponent;
    private final StateSupplier stateSupplier;

    Match(
            AI player,
            AI opponent,
            StateSupplier stateSupplier) {

        this.player = player;
        this.opponent = opponent;
        this.stateSupplier = stateSupplier;
    }

    @Override
    public MatchResult call() throws Exception {
        stateSupplier.first();

        player.reset();
        opponent.reset();

//        State playerCurrentState = stateSupplier.playerState();
//        State opponentCurrentState = stateSupplier.opponentState();
//        int rounds = 0;
//
//        do {
//            Action playerAction = player.play(playerCurrentState.clone());
//            Action opponentAction = opponent.play(opponentCurrentState.clone());
//
//            stateSupplier.next(playerAction, opponentAction);
//
//            playerCurrentState = stateSupplier.playerState();
//            opponentCurrentState = stateSupplier.opponentState();
//
//            rounds++;
//        } while (isNotFinished(playerCurrentState, opponentCurrentState, rounds));
//
//        return new MatchResult(
//                playerCurrentState.getPlayerScore(),
//                opponentCurrentState.getPlayerScore(),
//                rounds,
//                getWinner(playerCurrentState, opponentCurrentState, rounds));
        return null;
    }

    /**
     * //     * @param playerState
     * //     * @param opponentState
     *
     * @param rounds total number of played rounds
     * @return
     */
    static boolean isNotFinished(/*State playerState, State opponentState, */int rounds) {
        // TODO: implement game ending conditions
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * //     * @param playerState
     * //     * @param opponentState
     *
     * @param rounds total number of played rounds
     * @return
     */
    static Winner getWinner(/*State playerState, State opponentState,*/ int rounds) {
        // TODO: extract winner from finished game match
        throw new UnsupportedOperationException("Not implemented");
    }

    static final class MatchResult {

        private final int playerScore;
        private final int opponentScore;
        private final int rounds;
        private final Winner winner;

        private MatchResult(
                int playerScore,
                int opponentScore,
                int rounds,
                Winner winner) {

            this.playerScore = playerScore;
            this.opponentScore = opponentScore;
            this.rounds = rounds;
            this.winner = winner;
        }

        int getPlayerScore() {
            return playerScore;
        }

        int getOpponentScore() {
            return opponentScore;
        }

        int getRounds() {
            return rounds;
        }

        Winner getWinner() {
            return winner;
        }

        @Override
        public String toString() {
            return com.google.common.base.MoreObjects.toStringHelper(this)
                    .add("playerScore", playerScore)
                    .add("opponentScore", opponentScore)
                    .add("rounds", rounds)
                    .add("winner", winner)
                    .toString();
        }
    }
}
