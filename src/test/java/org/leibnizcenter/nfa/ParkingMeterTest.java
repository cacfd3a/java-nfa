package org.leibnizcenter.nfa;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.leibnizcenter.nfa.ParkingMeterTest.PayState.*;

/**
 * Parking meter example
 * <p>
 * Created by maarten on 17-6-16.
 */
public class ParkingMeterTest {
    /**
     * How much money is in the machine
     */
    private int cents;

    @Test
    public void parkingMeterExample() {
        // Say we can buy parking for 100 cents

        // Define some actions
        CoinDrop drop25cents = new CoinDrop(25);
        CoinDrop drop50cents = new CoinDrop(50);

        // Define our NFA
        NFA<PayState, CoinDrop> nfa = new NFA.Builder<PayState, CoinDrop>()
                .addTransition(PAYED_0, drop25cents, PAYED_25)
                .addTransition(PAYED_0, drop50cents, PAYED_50)
                .addTransition(PAYED_25, drop25cents, PAYED_50)
                .addTransition(PAYED_25, drop50cents, PAYED_75)
                .addTransition(PAYED_50, drop25cents, PAYED_75)
                .addTransition(PAYED_50, drop50cents, PAYED_0)
                .addTransition(PAYED_75, drop25cents, PAYED_0)
                .addTransition(PAYED_75, drop50cents, PAYED_0) // Payed too much... no money back!
                .build();

        Collection<State> endStates0 = manualDroppings(drop25cents, drop50cents, nfa);

        // Apply action step-by-step
        Collection<State> endStates1 = nfa.start(PAYED_0)
                .andThen(drop25cents)
                .andThen(drop50cents)
                .andThen(drop50cents)
                .andThen(drop25cents)
                .getState().collect(Collectors.toList());

        // Or apply actions in bulk
        Collection<State> endStates2 = nfa.apply(PAYED_0, new LinkedList<>(Arrays.asList(drop50cents, drop25cents, drop50cents, drop25cents)))
                .collect(Collectors.toList());

        System.out.println("Today earnings: ¢" + cents + ".");

        assertEquals(endStates1, endStates2);
        assertEquals(endStates0, endStates2);
        assertEquals(endStates1, Collections.singletonList(PAYED_25));
    }

    private Collection<State> manualDroppings(CoinDrop drop25cents, CoinDrop drop50cents, NFA<PayState, CoinDrop> nfa) {
        return nfa.getTransitions(PAYED_0, drop25cents).stream()
                .peek(transition -> drop25cents.accept(transition.getFrom(), transition.getTo()))
                .flatMap(trans -> nfa.getTransitions(trans.getTo(), drop50cents).stream())
                .peek(transition -> drop50cents.accept(transition.getFrom(), transition.getTo()))
                .flatMap(trans -> nfa.getTransitions(trans.getTo(), drop50cents).stream())
                .peek(transition -> drop50cents.accept(transition.getFrom(), transition.getTo()))
                .flatMap(trans -> nfa.getTransitions(trans.getTo(), drop25cents).stream())
                .peek(transition -> drop25cents.accept(transition.getFrom(), transition.getTo()))
                .map(Transition::getTo).collect(Collectors.toList());
    }

    enum PayState implements State {
        PAYED_0(0), PAYED_25(25), PAYED_50(50), PAYED_75(75);
        public final int centsValue;

        PayState(int centsValue) {
            this.centsValue = centsValue;
        }
    }

    private class CoinDrop implements Event<PayState> {
        final int centsValue;

        CoinDrop(int value) {
            this.centsValue = value;
        }

        @Override
        public void accept(PayState from, PayState to) {
            System.out.println("Bleep Bloop. Added ¢" + centsValue + " to ¢" + from.centsValue + ". ");
            if (to.centsValue <= 0 || to.centsValue >= 100) System.out.println("You may park. Good day.");
            else
                System.out.println("You have paid ¢" + to.centsValue + " in total. Please add ¢" + (100 - to.centsValue) + " before you may park.");
            System.out.println("----------------------------------------------");
            cents += this.centsValue;
        }

        @Override
        public String toString() {
            return "¢" + centsValue;
        }
    }
}
