package edu.ucr.cs.riple.core.checkers.nullaway.codefix;

import edu.ucr.cs.riple.core.agent.ChatGPT;
import edu.ucr.cs.riple.core.checkers.nullaway.NullAwayError;

public class NullAwayCodeFix {

    private final ChatGPT gpt;


    public NullAwayCodeFix(){
        gpt = new ChatGPT();
    }

    public void fix(NullAwayError error){
        switch (error.messageType){
            case "DEREFERENCE":
                resolveDereferenceError(error);
                break;
            default:
                return;
        }
    }

    private void resolveDereferenceError(NullAwayError error) {

    }


}
