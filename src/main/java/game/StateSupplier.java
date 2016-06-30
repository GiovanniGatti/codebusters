package game;

import game.Player.Action;

interface StateSupplier {

    /**
     * Computes the very first state, which usually is a match set up (build maps, boards, place players into their
     * start position...)
     */
    void first();

    /**
     * Computes the new match state based on the players' actions
     */
    void next(Action playerAction, Action opponentAction);

    /**
     * @return player view of the current match's state
     */
//    State playerState();

    /**
     * @return opponent view of the current match's state
     */
//    State opponentState();
}
