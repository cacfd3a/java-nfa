# Nondeterministic finite state automata
[![Build Status](https://travis-ci.org/cacfd3a/java-nfa.svg?branch=master)](https://travis-ci.org/cacfd3a/java-nfa)
[![GitHub version](https://badge.fury.io/gh/cacfd3a%2Fjava-nfa.svg)](https://github.com/cacfd3a/java-nfa/releases)
[![License](https://img.shields.io/npm/l/probabilistic-earley-parser.svg)](https://github.com/cacfd3a/java-nfa/blob/master/LICENSE.txt)

This is a library that provides an implemention of [nondeterminstic finite state automata](https://en.wikipedia.org/wiki/Nondeterministic_finite_automaton) (NFAs) in Java. You can think of NFAs as flowcharts: you are in a state, take some action, and arrive in a new state. The action can produce a side effect, such as writing a string to a tape. 

## Usage
Download [the latest JAR](https://github.com/cacfd3a/nfa/releases/latest) or grab from Maven:

```xml
<dependencies>
        <dependency>
            <groupId>org.leibnizcenter</groupId>
            <artifactId>nfa</artifactId>
            <version>1.0.0</version>
        </dependency>
</dependencies>
```

or Gradle:
```groovy
compile 'org.leibnizcenter:nfa:1.0.0'
```

## Why?
There are already a bunch of libraries out there which work with deterministic finite state automata (DFAs), and there is a well-known result in automata theory which says that for any language recognized by an NFA, we can construct a DFA which recognizes the same language.

So why not just use DFAs? Two reasons:
* Determinizing an NFA has an exponential blowup in the number of states.
* An NFA may have side-effects, which may be problematic with the standard way of determinizing NFAs. Indeed, [some non-deterministic finite state transducers have no equivalent deterministic finite state transducer](http://www.let.rug.nl/~vannoord/papers/preds/node22.html). 

On a side note, [Allauzen & Mohri](http://www.cs.nyu.edu/~allauzen/pdf/twins.pdf) have described efficient algorithms for determining when a transducer is in fact determinizable, and this would be a nice feature to implement.

## Current features
* Arbitrary input tokens, and arbitrary side effect to state transitions. For example we may implement a finite state transducer by taking strings as input tokens and writing some output string to tape as a side effect.
* Compute possible transition paths in polynomial time! Using a [forward-backward-like algorithm](https://en.wikipedia.org/wiki/Forward%E2%80%93backward_algorithm), we can compute all paths through automaton *A* originating from state *S*, given input *I* all possible paths in O(|*S*| * |*I*| * |*A*|).
* Transition paths can be accessed through a Spliterator: Java 8 streaming APIs can automatically branch transition paths on states where one action may lead to multiple result states.

## Example
Here is a simple example of a parking meter that takes money:

```java
public class ParkingMeter {
    /**
     * How much money is in the machine
     */
    private int ¢¢¢;

    public static void main(String[] args) {
        // Run example
        new ParkingMeter().run();
    }
    
    public void run() {
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

        // Apply action step-by-step
        Collection<State> endStates1 = nfa.start(PAYED_0)
                .andThen(drop25cents)
                .andThen(drop50cents)
                .andThen(drop50cents)
                .andThen(drop25cents)
                .getState().collect(Collectors.toList());

        // Or apply actions in bulk (this makes calculations of the possible paths more efficient, but it doesn't matter if we iterate over all transitions anyway)
        Collection<State> endStates2 = nfa.apply(PAYED_0, new LinkedList<>(Arrays.asList(drop50cents, drop25cents, drop50cents, drop25cents)))
                .collect(Collectors.toList());

        System.out.println("Today earnings: ¢" + ¢¢¢ + ".");
    }
    
    private class CoinDrop implements Event<PayState> {
        final int ¢;
    
        CoinDrop(int value) {
            this.¢ = value;
        }
    
        @Override
        public void accept(PayState from, PayState to) {
            System.out.println("Bleep Bloop. Added ¢" + ¢ + " to ¢" + from.¢ + ". ");
            if (to.¢ <= 0 || to.¢ >= 100) System.out.println("You may park. Good day.");
            else 
                System.out.println("You have paid ¢" + to.¢ + " in total. Please add ¢" + (100 - to.¢) + " before you may park.");
            System.out.println("----------------------------------------------");
            ¢¢¢ += this.¢;
        }
    
        @Override
        public String toString() {
            return "¢" + ¢;
        }
    }
    
    enum PayState implements State {
        PAYED_0(0), PAYED_25(25), PAYED_50(50), PAYED_75(75);
        public int ¢;
    
        PayState(int ¢) {
            this.¢ = ¢;
        }
    }
}
```
